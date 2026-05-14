package com.ticketflow.ventas.concurrent;

import com.ticketflow.ventas.model.SolicitudTicket;
import com.ticketflow.ventas.service.LogService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * ============================================================
 * PRODUCTOR - Thread que simula a un cliente solicitando reserva
 * ============================================================
 *
 * CONCEPTO: En el patrón Productor-Consumidor:
 *   - El PRODUCTOR genera ítems y los pone en el buffer.
 *   - En nuestro contexto: el cliente GENERA una solicitud
 *     de reserva y la PONE en el buffer compartido.
 *
 * PARALELISMO: Se crean MÚLTIPLES instancias de este thread,
 * una por cliente, simulando que muchos clientes intentan
 * inscribirse al mismo tiempo (concurrentemente).
 *
 * DIAGRAMA DE FLUJO:
 *
 *   [Cliente quiere inscribirse]
 *           │
 *           ▼
 *   [Espera EMPTY > 0]  ← SE BLOQUEA si el buffer está lleno
 *           │
 *           ▼
 *   [Adquiere MUTEX]    ← SE BLOQUEA si otro thread está en la sección crítica
 *           │
 *           ▼
 *   [Inserta en Buffer] ← SECCIÓN CRÍTICA
 *           │
 *           ▼
 *   [Libera MUTEX]
 *           │
 *           ▼
 *   [Incrementa FULL]   ← Señaliza al consumidor
 *           │
 *           ▼
 *   [Thread termina]
 */
@Slf4j
public class ThreadProductor implements Runnable {

    /** Referencia al buffer compartido donde se depositan solicitudes */
    private final BufferCompartido buffer;

    /** La solicitud que este thread va a intentar inscribir */
    private final SolicitudTicket solicitud;

    /** Servicio para registrar eventos en el log del sistema */
    private final LogService logService;

    /** Tiempo de "preparación" antes de intentar inscribirse (ms) */
    private final int tiempoEspera;

    /**
     * @param buffer     El buffer compartido (recurso compartido)
     * @param solicitud  La solicitud de reserva del cliente
     * @param logService Para registrar eventos
     * @param tiempoEspera Simula el tiempo que tarda el cliente en completar el formulario
     */
    public ThreadProductor(BufferCompartido buffer, SolicitudTicket solicitud,
                           LogService logService, int tiempoEspera) {
        this.buffer = buffer;
        this.solicitud = solicitud;
        this.logService = logService;
        this.tiempoEspera = tiempoEspera;
    }

    /**
     * Método principal del thread (se ejecuta cuando se inicia).
     *
     * IMPORTANTE: Este método corre en un THREAD SEPARADO.
     * Múltiples instancias de run() pueden estar ejecutándose
     * simultáneamente en threads diferentes.
     */
    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] 🎓 Cliente {} iniciando proceso de reserva",
                threadName, solicitud.getClienteNombre());

        logService.agregarLog("INFO",
                String.format("Cliente %s preparando solicitud de reserva...",
                        solicitud.getClienteNombre()),
                threadName);

        try {
            // ──────────────────────────────────────────────────
            // Simular tiempo de "preparación" del cliente
            // (tiempo en completar el formulario, por ejemplo)
            // Esto hace más realista la simulación y genera
            // concurrencia real entre los threads.
            // ──────────────────────────────────────────────────
            Thread.sleep(tiempoEspera);

            logService.agregarLog("INFO",
                    String.format("Cliente %s enviando solicitud al sistema...",
                            solicitud.getClienteNombre()),
                    threadName);

            // ──────────────────────────────────────────────────
            // PRODUCIR: Intentar poner la solicitud en el buffer
            //
            // Internamente, buffer.producir() hace:
            //   1. empty.acquire() → esperar si no hay espacio
            //   2. mutex.acquire() → sección crítica
            //   3. buffer.add()    → insertar solicitud
            //   4. mutex.release() → fin sección crítica
            //   5. full.release()  → señalizar al consumidor
            //
            // Si el buffer está lleno, este thread SE BLOQUEA
            // en buffer.producir() hasta que haya espacio.
            // ──────────────────────────────────────────────────
            solicitud.setEstado(SolicitudTicket.EstadoSolicitud.PENDIENTE);
            solicitud.setTimestampCreacion(LocalDateTime.now());

            buffer.producir(solicitud);

            log.info("[{}] ✅ Solicitud de {} colocada en el buffer",
                    threadName, solicitud.getClienteNombre());
            logService.agregarLog("SUCCESS",
                    String.format("✅ Solicitud de %s en cola (buffer: %d/%d)",
                            solicitud.getClienteNombre(),
                            buffer.getTamanioActual(),
                            buffer.getCapacidad()),
                    threadName);

        } catch (InterruptedException e) {
            // Si el thread es interrumpido externamente (ej: al resetear)
            log.warn("[{}] ⚠️ Thread interrumpido para cliente {}", threadName, solicitud.getClienteNombre());
            Thread.currentThread().interrupt();
        }
    }
}
