package com.ticketflow.ventas.service;

import com.ticketflow.ventas.concurrent.BufferCompartido;
import com.ticketflow.ventas.concurrent.ThreadConsumidor;
import com.ticketflow.ventas.concurrent.ThreadProductor;
import com.ticketflow.ventas.model.EstadoSistema;
import com.ticketflow.ventas.model.SolicitudTicket;
import com.ticketflow.ventas.model.SolicitudTicket.EstadoSolicitud;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * SERVICIO PRINCIPAL DE SIMULACIÓN
 * ============================================================
 * Orquesta toda la lógica del patrón Productor-Consumidor:
 *
 * 1. Inicializa el buffer compartido con semáforos
 * 2. Crea y lanza threads Productores (clientes)
 * 3. Crea y lanza el thread Consumidor (sistema de reserva)
 * 4. Expone el estado del sistema para el frontend
 * 5. Permite resetear la simulación
 *
 * ARQUITECTURA DEL SISTEMA:
 *
 *  ┌──────────────────────────────────────────────────────┐
 *  │                  SimulacionService                   │
 *  │                                                      │
 *  │  ExecutorService (pool de threads)                   │
 *  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
 *  │  │Productor1│  │Productor2│  │ProductorN│  ← N threads│
 *  │  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
 *  │       └─────────────┼─────────────┘                  │
 *  │                     ▼                                │
 *  │         ┌───────────────────────┐                    │
 *  │         │   BufferCompartido    │ ← Semáforos        │
 *  │         │   [S1, S2, S3, ...]   │   mutex/empty/full │
 *  │         └───────────┬───────────┘                    │
 *  │                     │                                │
 *  │                     ▼                                │
 *  │         ┌───────────────────────┐                    │
 *  │         │   ThreadConsumidor    │ ← 1 thread         │
 *  │         │   (procesa + tickets)   │                    │
 *  │         └───────────────────────┘                    │
 *  └──────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacionService {

    private final BufferCompartido buffer;
    private final RegistroService registroService;
    private final LogService logService;

    // ── Parámetros de simulación (configurables) ───────────────
    private int capacidadEvento = 10;
    private int cantidadClientes = 20;

    @Value("${simulation.processing.min-time:500}")
    private int tiempoMinProcesamiento;

    @Value("${simulation.processing.max-time:2000}")
    private int tiempoMaxProcesamiento;

    // ── Estado de la simulación ───────────────────────────────
    /** Indica si hay una simulación activa */
    private final AtomicBoolean simulacionActiva = new AtomicBoolean(false);

    /** Contador de threads productores activos */
    private final AtomicInteger productoresActivos = new AtomicInteger(0);

    /** Referencia al consumidor para poder detenerlo y consultar su estado */
    private ThreadConsumidor consumidorActual;

    /** Pool de threads para productores y consumidor */
    private ExecutorService executorService;

    /** Thread dedicado al consumidor */
    private Thread threadConsumidor;

    // ── Datos mock para la simulación ────────────────────────
    private static final String[] NOMBRES = {
        "Ana García", "Carlos López", "María Rodríguez", "José Martínez",
        "Laura Sánchez", "Miguel Pérez", "Sofía Hernández", "Daniel Torres",
        "Valentina Díaz", "Andrés Flores", "Camila Jiménez", "Sebastián Castro",
        "Isabella Morales", "Mateo Vargas", "Lucía Ortega", "Emilio Navarro",
        "Gabriela Reyes", "Tomás Muñoz", "Natalia Ruiz", "Alejandro Silva",
        "Paula Mendoza", "Roberto Aguilar", "Fernanda Cruz", "Eduardo Ríos"
    };

    private static final String[] EXAMENES = {
        "Semiología Clínica - Evento de Simulación A",
        "Diagnóstico Diferencial - Caso Clínico B",
        "Urgencias Médicas - Simulación Trauma",
        "Cardiología Clínica - ECG Avanzado"
    };

    /**
     * ============================================================
     * INICIAR SIMULACIÓN
     * ============================================================
     * Lanza la simulación Productor-Consumidor completa:
     * 1. Crea el consumidor (1 thread)
     * 2. Crea N productores (1 por cliente, en paralelo)
     */
    public synchronized void iniciarSimulacion() {
        if (simulacionActiva.get()) {
            log.warn("Ya hay una simulación activa. Usar /reset primero.");
            return;
        }

        // Limpieza de hilos zombis o datos anteriores antes de iniciar
        resetearEstadoInterno();

        log.info("=== INICIANDO SIMULACIÓN ===");
        log.info("Clientes: {}, Tickets: {}, Buffer: {}",
                cantidadClientes, capacidadEvento, buffer.getCapacidad());

        simulacionActiva.set(true);
        productoresActivos.set(cantidadClientes);

        // ──────────────────────────────────────────────────────
        // PASO 1: Crear e iniciar el CONSUMIDOR
        // El consumidor empieza a esperar solicitudes en el buffer
        // (se bloquea en full.acquire() hasta que llegue algo)
        // ──────────────────────────────────────────────────────
        consumidorActual = new ThreadConsumidor(
                buffer, registroService, logService,
                capacidadEvento, tiempoMinProcesamiento, tiempoMaxProcesamiento);

        threadConsumidor = new Thread(consumidorActual, "Consumidor-Sistema");
        threadConsumidor.setDaemon(true);
        threadConsumidor.start();

        logService.agregarLog("INFO",
                "🏥 Sistema de tickets iniciado. Esperando solicitudes...",
                "Sistema");

        // ──────────────────────────────────────────────────────
        // PASO 2: Crear pool de threads para PRODUCTORES
        // Cada cliente tiene su propio thread (concurrencia real)
        // ──────────────────────────────────────────────────────
        executorService = Executors.newFixedThreadPool(cantidadClientes);

        // Generar solicitudes para todos los clientes
        List<SolicitudTicket> solicitudes = generarSolicitudesMock();

        logService.agregarLog("INFO",
                String.format("🎓 Lanzando %d clientes concurrentemente...", cantidadClientes),
                "Sistema");

        // ──────────────────────────────────────────────────────
        // Lanzar un thread productor por cada cliente
        // TODOS se lanzan "casi simultáneamente" → concurrencia real
        // ──────────────────────────────────────────────────────
        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudTicket solicitud = solicitudes.get(i);
            registroService.registrarSolicitud(solicitud);

            // Tiempo de espera escalonado (0-3 segundos) para simular
            // que los clientes llegan en momentos ligeramente distintos
            int tiempoEspera = (int)(Math.random() * 3000);

            ThreadProductor productor = new ThreadProductor(
                    buffer, solicitud, logService, tiempoEspera);

            String nombreThread = "Productor-" + solicitud.getClienteNombre().split(" ")[0];

            // Enviar al pool de threads (se ejecuta en paralelo)
            executorService.submit(() -> {
                Thread.currentThread().setName(nombreThread);
                try {
                    productor.run();
                } finally {
                    // Cuando este productor termina, decrementar contador
                    int restantes = productoresActivos.decrementAndGet();
                    if (restantes == 0) {
                        log.info("Todos los productores terminaron de enviar solicitudes.");
                        logService.agregarLog("INFO",
                                "✅ Todos los clientes enviaron sus solicitudes. Procesando cola...",
                                "Sistema");
                        // La simulación queda "activa" hasta que el consumidor procese todo
                        // Se marca inactiva cuando el buffer está vacío y no hay pendientes
                    }
                }
            });
        }

        // El ExecutorService no acepta más tareas
        executorService.shutdown();

        log.info("=== {} threads productores lanzados ===", cantidadClientes);
    }

    /**
     * ============================================================
     * RESETEAR SIMULACIÓN
     * ============================================================
     * Detiene todos los threads, limpia el estado y permite
     * iniciar una nueva simulación desde cero.
     */
    public synchronized void resetearSimulacion() {
        log.info("=== RESETEANDO SIMULACIÓN ===");
        resetearEstadoInterno();
        
        logService.agregarLog("INFO", "🔄 Sistema reseteado. Listo para nueva simulación.", "Sistema");
        log.info("=== RESET COMPLETADO ===");
    }

    private void resetearEstadoInterno() {
        // Detener consumidor
        if (consumidorActual != null) {
            consumidorActual.detener();
        }
        if (threadConsumidor != null) {
            threadConsumidor.interrupt();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }

        // Limpiar estado
        buffer.limpiar();
        registroService.limpiar();
        logService.limpiar();

        simulacionActiva.set(false);
        productoresActivos.set(0);
        consumidorActual = null;
        threadConsumidor = null;

        logService.agregarLog("INFO", "🔄 Sistema reseteado. Listo para nueva simulación.", "Sistema");
        log.info("=== RESET COMPLETADO ===");
    }

    /**
     * ============================================================
     * OBTENER ESTADO DEL SISTEMA (para el frontend)
     * ============================================================
     * Retorna un snapshot del estado actual para mostrar en el dashboard.
     * El frontend hace polling cada 1 segundo para actualizar la UI.
     */
    public EstadoSistema getEstado() {
        // Verificar si la simulación terminó automáticamente
        verificarFinSimulacion();

        int ticketsDisponibles = consumidorActual != null ? consumidorActual.getTicketsDisponibles() : capacidadEvento;
        int ticketsOcupados = consumidorActual != null ? consumidorActual.getTicketsOcupados() : 0;

        return EstadoSistema.builder()
                // Estado del buffer
                .solicitudesEnBuffer(buffer.getTamanioActual())
                .capacidadBuffer(buffer.getCapacidad())
                // Estado del evento
                .ticketsTotales(capacidadEvento)
                .ticketsOcupados(ticketsOcupados)
                .ticketsDisponibles(ticketsDisponibles)
                // Estadísticas
                .totalSolicitudes(registroService.getTotal())
                .solicitudesAprobadas((int) registroService.getCountAprobadas())
                .solicitudesAgotadas((int) registroService.getCountAgotadas())
                .solicitudesPendientes((int) registroService.getCountPendientes())
                // Estado de la simulación
                .simulacionActiva(simulacionActiva.get())
                .threadProductoresActivos(productoresActivos.get())
                // Estado de semáforos (para visualización educativa)
                .semaforoMutexPermisos(buffer.getMutexPermisos())
                .semaforoEmptyPermisos(buffer.getEmptyPermisos())
                .semaforoFullPermisos(buffer.getFullPermisos())
                // Datos
                .ultimasSolicitudes(registroService.getTodas()
                        .stream().limit(50).toList())
                .logs(logService.getLogs()
                        .stream().limit(30).toList())
                .build();
    }

    /**
     * Verifica si la simulación terminó automáticamente.
     * Esto ocurre cuando todos los productores terminaron Y
     * el buffer está vacío Y no hay solicitudes pendientes.
     */
    private void verificarFinSimulacion() {
        if (simulacionActiva.get()
                && productoresActivos.get() == 0
                && buffer.getTamanioActual() == 0
                && registroService.getCountPendientes() == 0
                && registroService.getTotal() > 0) {
            // Detener el consumidor para que no quede como zombie
            if (consumidorActual != null) {
                consumidorActual.detener();
            }
            if (threadConsumidor != null) {
                threadConsumidor.interrupt();
            }

            simulacionActiva.set(false);
            logService.agregarLog("SUCCESS",
                    String.format("🏁 Simulación completada. Aprobados: %d, Agotados: %d",
                            registroService.getCountAprobadas(),
                            registroService.getCountAgotadas()),
                    "Sistema");
        }
    }

    /**
     * Genera la lista de solicitudes mock para los clientes simulados.
     */
    private List<SolicitudTicket> generarSolicitudesMock() {
        List<SolicitudTicket> lista = new ArrayList<>();
        String eventoId = "EXAM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String eventoNombre = EXAMENES[(int)(Math.random() * EXAMENES.length)];

        for (int i = 0; i < cantidadClientes; i++) {
            String nombre = NOMBRES[i % NOMBRES.length];
            if (i >= NOMBRES.length) {
                nombre = nombre + " " + (i + 1); // Evitar duplicados
            }

            SolicitudTicket solicitud = SolicitudTicket.builder()
                    .id(UUID.randomUUID().toString())
                    .clienteId("A-" + String.format("%04d", i + 1))
                    .clienteNombre(nombre)
                    .eventoId(eventoId)
                    .eventoNombre(eventoNombre)
                    .estado(EstadoSolicitud.PENDIENTE)
                    .build();

            lista.add(solicitud);
        }
        return lista;
    }

    public boolean isSimulacionActiva() { return simulacionActiva.get(); }
    public int getCapacidadEvento() { return capacidadEvento; }
    public void setCapacidadEvento(int capacidad) { this.capacidadEvento = capacidad; }
    public int getCantidadClientes() { return cantidadClientes; }
    public void setCantidadClientes(int cantidad) { this.cantidadClientes = cantidad; }
}
