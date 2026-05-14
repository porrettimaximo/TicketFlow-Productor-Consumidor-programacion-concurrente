package com.ticketflow.ventas.concurrent;

import com.ticketflow.ventas.model.SolicitudTicket;
import com.ticketflow.ventas.model.SolicitudTicket.EstadoSolicitud;
import com.ticketflow.ventas.service.LogService;
import com.ticketflow.ventas.service.RegistroService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * CONSUMIDOR - Thread único que procesa solicitudes del buffer
 * ============================================================
 *
 * CONCEPTO: En el patrón Productor-Consumidor:
 *   - El CONSUMIDOR extrae ítems del buffer y los procesa.
 *   - En nuestro contexto: el sistema TOMA solicitudes del
 *     buffer y PROCESA cada reserva (acepta o rechaza).
 *
 * IMPORTANTE: Solo existe UN thread consumidor.
 * Múltiples productores, pero UN solo consumidor.
 * Esto es el clásico patrón "N productores - 1 consumidor".
 *
 * CONTROL DE CUPOS:
 * El consumidor mantiene un semáforo adicional para controlar
 * los tickets del evento, evitando sobretickets incluso
 * cuando múltiples solicitudes son procesadas concurrentemente.
 *
 * SEMÁFORO DE CUPOS:
 * ─────────────────
 * Tipo: Semáforo contador
 * Valor inicial: capacidad del evento (ej: 10)
 * Uso:
 *   - ticketsDisponibles.tryAcquire() → intenta tomar un ticket
 *     Si devuelve true: hay ticket → ACEPTAR reserva
 *     Si devuelve false: sin ticket → RECHAZAR reserva
 *
 * DIAGRAMA DE FLUJO:
 *
 *   [Consumidor esperando]
 *           │
 *           ▼
 *   [Espera FULL > 0]    ← SE BLOQUEA si el buffer está vacío
 *           │
 *           ▼
 *   [Adquiere MUTEX]
 *           │
 *           ▼
 *   [Extrae del Buffer]  ← SECCIÓN CRÍTICA
 *           │
 *           ▼
 *   [Libera MUTEX]
 *           │
 *           ▼
 *   [Incrementa EMPTY]   ← Señaliza a productores
 *           │
 *           ▼
 *   [¿Hay ticket?] ──Sí──▶ [ACEPTA reserva]
 *           │
 *          No
 *           │
 *           ▼
 *   [RECHAZA reserva]
 *           │
 *           ▼
 *   [Vuelve a esperar FULL] ← Loop infinito
 */
@Slf4j
public class ThreadConsumidor implements Runnable {

    /** Referencia al buffer compartido */
    private final BufferCompartido buffer;

    /** Servicio para persistir los resultados */
    private final RegistroService registroService;

    /** Servicio de logs */
    private final LogService logService;

    /**
     * SEMÁFORO DE CUPOS DEL EXAMEN
     * ────────────────────────────
     * Controla cuántos clientes pueden inscribirse al evento.
     * tryAcquire() en lugar de acquire() para no bloquear,
     * sino rechazar si no hay ticket disponible.
     */
    private final Semaphore ticketsDisponibles;

    /** Tickets totales (para reportes) */
    private final int ticketsTotales;

    /**
     * Flag de control: indica si el consumidor debe seguir corriendo.
     * AtomicBoolean para acceso thread-safe sin necesitar synchronized.
     */
    private final AtomicBoolean activo;

    /**
     * Tiempo mínimo y máximo de procesamiento de una solicitud.
     * Simula el tiempo real que tarda el sistema en procesar una reserva.
     */
    private final int minTiempoProcesamiento;
    private final int maxTiempoProcesamiento;

    /** Contadores atómicos para estadísticas (thread-safe) */
    private final AtomicInteger totalAprobadas = new AtomicInteger(0);
    private final AtomicInteger totalAgotadas = new AtomicInteger(0);

    public ThreadConsumidor(BufferCompartido buffer, RegistroService registroService,
                            LogService logService, int ticketsTotales,
                            int minTiempoProcesamiento, int maxTiempoProcesamiento) {
        this.buffer = buffer;
        this.registroService = registroService;
        this.logService = logService;
        this.ticketsTotales = ticketsTotales;
        this.minTiempoProcesamiento = minTiempoProcesamiento;
        this.maxTiempoProcesamiento = maxTiempoProcesamiento;
        this.activo = new AtomicBoolean(true);

        // Inicializar semáforo de tickets con la capacidad del evento
        this.ticketsDisponibles = new Semaphore(ticketsTotales);

        log.info("Consumidor inicializado con {} tickets disponibles", ticketsTotales);
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] 🏥 Sistema de tickets iniciado. Tickets: {}", threadName, ticketsTotales);
        logService.agregarLog("INFO",
                String.format("Sistema activo. Tickets totales: %d", ticketsTotales),
                threadName);

        // ──────────────────────────────────────────────────────
        // LOOP PRINCIPAL DEL CONSUMIDOR
        // El consumidor corre indefinidamente, procesando
        // solicitudes una a una mientras el sistema esté activo.
        // ──────────────────────────────────────────────────────
        while (activo.get()) {
            try {
                procesarSiguienteSolicitud(threadName);
            } catch (InterruptedException e) {
                log.info("[{}] Consumidor interrumpido, deteniendo...", threadName);
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("[{}] 🛑 Consumidor detenido. Aprobadas: {}, Agotadas: {}",
                threadName, totalAprobadas.get(), totalAgotadas.get());
    }

    /**
     * Procesa una solicitud del buffer: la extrae, evalúa tickets y registra resultado.
     */
    private void procesarSiguienteSolicitud(String threadName) throws InterruptedException {

        logService.agregarLog("INFO", "⏳ Sistema esperando solicitudes en el buffer...", threadName);

        // ──────────────────────────────────────────────────────
        // CONSUMIR del buffer (puede bloquearse si está vacío)
        // Internamente hace: full.acquire() → mutex → poll → mutex.release() → empty.release()
        // ──────────────────────────────────────────────────────
        SolicitudTicket solicitud = buffer.consumir();

        if (solicitud == null || !activo.get()) return;

        // Marcar como en proceso
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        registroService.actualizarSolicitud(solicitud);

        logService.agregarLog("INFO",
                String.format("🔄 Procesando solicitud de %s...", solicitud.getClienteNombre()),
                threadName);

        // ──────────────────────────────────────────────────────
        // Simular tiempo de procesamiento (realismo)
        // ──────────────────────────────────────────────────────
        int tiempoProcesamiento = minTiempoProcesamiento +
                (int)(Math.random() * (maxTiempoProcesamiento - minTiempoProcesamiento));

        long inicio = System.currentTimeMillis();
        Thread.sleep(tiempoProcesamiento);
        long fin = System.currentTimeMillis();

        solicitud.setTiempoProcesamiento(fin - inicio);

        // ──────────────────────────────────────────────────────
        // VERIFICAR CUPOS con el semáforo de tickets
        //
        // tryAcquire() es NO BLOQUEANTE:
        //   - Devuelve TRUE si hay ticket (y lo consume)
        //   - Devuelve FALSE si no hay ticket (sin bloquear)
        //
        // Esto evita sobretickets: el semáforo garantiza
        // que exactamente N clientes serán aprobados.
        // ──────────────────────────────────────────────────────
        boolean ticketObtenido = ticketsDisponibles.tryAcquire();

        solicitud.setTimestampProcesado(LocalDateTime.now());

        if (ticketObtenido) {
            // ── ÉXITO: Se obtuvo un ticket ─────────────────────
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
            solicitud.setMensaje("Reserva exitosa. Ticket asignado.");
            totalAprobadas.incrementAndGet();

            int ticketsRestantes = ticketsDisponibles.availablePermits();
            log.info("[{}] ✅ ACEPTADO: {} | Tickets restantes: {}",
                    threadName, solicitud.getClienteNombre(), ticketsRestantes);
            logService.agregarLog("SUCCESS",
                    String.format("✅ ACEPTADO: %s | Tickets restantes: %d",
                            solicitud.getClienteNombre(), ticketsRestantes),
                    threadName);
        } else {
            // ── RECHAZO: Sin tickets disponibles ──────────────
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitud.setMensaje("Sin tickets disponibles. Evento completo.");
            totalAgotadas.incrementAndGet();

            log.info("[{}] ❌ RECHAZADO: {} | Sin tickets disponibles",
                    threadName, solicitud.getClienteNombre());
            logService.agregarLog("ERROR",
                    String.format("❌ RECHAZADO: %s | Sin tickets (evento lleno)",
                            solicitud.getClienteNombre()),
                    threadName);
        }

        // Persistir resultado final
        registroService.actualizarSolicitud(solicitud);
    }

    /** Detiene el consumidor de forma segura */
    public void detener() {
        activo.set(false);
    }

    /** @return Tickets actualmente disponibles */
    public int getTicketsDisponibles() {
        return ticketsDisponibles.availablePermits();
    }

    /** @return Total de tickets del evento */
    public int getTicketsTotales() {
        return ticketsTotales;
    }

    /** @return Tickets ya ocupados */
    public int getTicketsOcupados() {
        return ticketsTotales - ticketsDisponibles.availablePermits();
    }

    public int getTotalAprobadas() { return totalAprobadas.get(); }
    public int getTotalAgotadas() { return totalAgotadas.get(); }
}
