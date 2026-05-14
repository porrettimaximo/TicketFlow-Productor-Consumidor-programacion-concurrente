package com.ticketflow.ventas.concurrent;

import com.ticketflow.ventas.model.SolicitudTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * ============================================================
 * BUFFER COMPARTIDO - Núcleo del Patrón Productor-Consumidor
 * ============================================================
 *
 * CONCEPTO FUNDAMENTAL:
 * El buffer es la estructura de datos compartida entre los
 * threads PRODUCTORES (clientes que solicitan reserva) y
 * el thread CONSUMIDOR (el sistema que procesa solicitudes).
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │                    SISTEMA DE BUFFER                    │
 * │                                                         │
 * │  Productor1 ──┐                                         │
 * │  Productor2 ──┤──▶ [BUFFER: S1, S2, S3] ──▶ Consumidor│
 * │  Productor3 ──┘    (cola circular fija)                 │
 * │                                                         │
 * └─────────────────────────────────────────────────────────┘
 *
 * LOS TRES SEMÁFOROS (clásico de Dijkstra):
 * ──────────────────────────────────────────
 *
 * 1. MUTEX (binary semaphore = 1):
 *    - Garantiza exclusión mutua al acceder al buffer.
 *    - Solo UN thread puede leer/escribir en el buffer a la vez.
 *    - Protege la SECCIÓN CRÍTICA.
 *
 * 2. EMPTY (counting semaphore = N):
 *    - Representa cuántos ESPACIOS VACÍOS hay en el buffer.
 *    - Inicializado en N (capacidad del buffer).
 *    - Productor hace acquire() antes de insertar:
 *        si EMPTY == 0 → el productor SE BLOQUEA (buffer lleno).
 *    - Consumidor hace release() después de extraer:
 *        incrementa EMPTY (liberó un espacio).
 *
 * 3. FULL (counting semaphore = 0):
 *    - Representa cuántos ÍTEMS HAY para consumir.
 *    - Inicializado en 0 (buffer vacío).
 *    - Consumidor hace acquire() antes de extraer:
 *        si FULL == 0 → el consumidor SE BLOQUEA (nada que procesar).
 *    - Productor hace release() después de insertar:
 *        incrementa FULL (hay un nuevo ítem disponible).
 */
@Slf4j
@Component
public class BufferCompartido {

    // ── La estructura de datos del buffer ────────────────────
    /**
     * COLA (Queue): estructura FIFO donde se almacenan solicitudes.
     * FIFO = First In, First Out = el primero en entrar es el primero en salir.
     * Esto garantiza equidad en el procesamiento de solicitudes.
     */
    private final Queue<SolicitudTicket> buffer;

    /** Capacidad máxima del buffer (fija, no puede cambiar en runtime) */
    private final int capacidad;

    // ══════════════════════════════════════════════════════════
    //  DECLARACIÓN DE SEMÁFOROS
    // ══════════════════════════════════════════════════════════

    /**
     * SEMÁFORO MUTEX (Mutual Exclusion)
     * ────────────────────────────────
     * Tipo: Semáforo binario (máximo 1 permiso)
     * Propósito: Exclusión mutua para acceder al buffer.
     *
     * Garantiza que en la SECCIÓN CRÍTICA (manipulación del buffer)
     * solo haya UN thread a la vez, evitando condiciones de carrera.
     *
     * Estado inicial: 1 (la sección crítica está libre)
     *
     * Uso:
     *   mutex.acquire() → entro a la sección crítica
     *   [... operación sobre el buffer ...]
     *   mutex.release() → salgo de la sección crítica
     */
    private final Semaphore mutex;

    /**
     * SEMÁFORO EMPTY (Espacios vacíos)
     * ─────────────────────────────────
     * Tipo: Semáforo contador
     * Propósito: Controlar si hay ESPACIO para insertar en el buffer.
     *
     * Estado inicial: capacidad (todos los espacios están vacíos)
     *
     * Cuándo un PRODUCTOR se bloquea:
     *   Si empty.availablePermits() == 0 → buffer lleno → WAIT
     *   El productor espera hasta que el consumidor libere espacio.
     */
    private final Semaphore empty;

    /**
     * SEMÁFORO FULL (Ítems disponibles)
     * ──────────────────────────────────
     * Tipo: Semáforo contador
     * Propósito: Controlar si hay ÍTEMS para que el consumidor procese.
     *
     * Estado inicial: 0 (no hay nada en el buffer al inicio)
     *
     * Cuándo el CONSUMIDOR se bloquea:
     *   Si full.availablePermits() == 0 → buffer vacío → WAIT
     *   El consumidor espera hasta que algún productor inserte algo.
     */
    private final Semaphore full;

    /**
     * Constructor: inicializa el buffer y los tres semáforos.
     *
     * @param capacidad Tamaño máximo del buffer (del application.properties)
     */
    public BufferCompartido(@Value("${simulation.buffer.size:5}") int capacidad) {
        this.capacidad = capacidad;
        this.buffer = new LinkedList<>();

        // ── Inicialización de semáforos ───────────────────────
        // mutex = 1: la sección crítica está libre al inicio
        this.mutex = new Semaphore(1);

        // empty = capacidad: todos los espacios del buffer están vacíos
        this.empty = new Semaphore(capacidad);

        // full = 0: no hay ítems en el buffer para consumir
        this.full = new Semaphore(0);

        log.info("Buffer inicializado con capacidad={}, semáforos: mutex=1, empty={}, full=0",
                capacidad, capacidad);
    }

    // ══════════════════════════════════════════════════════════
    //  OPERACIÓN PRODUCIR (llamada por threads Productores)
    // ══════════════════════════════════════════════════════════

    /**
     * PRODUCIR: Inserta una solicitud en el buffer.
     * ─────────────────────────────────────────────
     * Implementa la lógica clásica del productor:
     *
     *   1. empty.acquire()  → esperar si no hay espacio
     *   2. mutex.acquire()  → entrar a la sección crítica
     *   3. buffer.add()     → insertar en el buffer
     *   4. mutex.release()  → salir de la sección crítica
     *   5. full.release()   → señalizar que hay un ítem nuevo
     *
     * @param solicitud La solicitud a insertar en el buffer
     * @throws InterruptedException si el thread es interrumpido mientras espera
     */
    public void producir(SolicitudTicket solicitud) throws InterruptedException {
        String threadName = Thread.currentThread().getName();

        log.debug("[{}] Intentando producir solicitud de {}", threadName, solicitud.getClienteNombre());

        // ──────────────────────────────────────────────────────
        // PASO 1: Verificar espacio disponible en el buffer
        // Si empty == 0, este thread SE BLOQUEA aquí hasta que
        // el consumidor libere espacio (llame a empty.release())
        // ──────────────────────────────────────────────────────
        log.debug("[{}] Esperando semáforo EMPTY (disponibles={})", threadName, empty.availablePermits());
        empty.acquire();  // ← POSIBLE PUNTO DE BLOQUEO DEL PRODUCTOR
        log.debug("[{}] Semáforo EMPTY adquirido, hay espacio en el buffer", threadName);

        // ──────────────────────────────────────────────────────
        // PASO 2: Entrar a la SECCIÓN CRÍTICA
        // mutex.acquire() garantiza exclusión mutua: si otro thread
        // ya está manipulando el buffer, este se bloquea aquí.
        //
        // SECCIÓN CRÍTICA: código que accede al recurso compartido (buffer)
        // Solo UN thread puede ejecutar esto a la vez.
        // ──────────────────────────────────────────────────────
        log.debug("[{}] Esperando MUTEX para entrar a sección crítica", threadName);
        mutex.acquire();  // ← ENTRO A LA SECCIÓN CRÍTICA
        try {
            // ── DENTRO DE LA SECCIÓN CRÍTICA ──────────────────
            // Aquí estamos seguros: somos el único thread modificando el buffer
            buffer.add(solicitud);
            log.debug("[{}] ✅ Solicitud añadida al buffer. Tamaño actual: {}/{}", 
                     threadName, buffer.size(), capacidad);
            // ── FIN DE LA SECCIÓN CRÍTICA ─────────────────────
        } finally {
            // PASO 3: Salir de la sección crítica
            // El finally garantiza que SIEMPRE liberamos el mutex,
            // incluso si ocurre una excepción (evita deadlock)
            mutex.release();  // ← SALGO DE LA SECCIÓN CRÍTICA
            log.debug("[{}] MUTEX liberado", threadName);
        }

        // ──────────────────────────────────────────────────────
        // PASO 4: Señalizar que hay un ítem nuevo para consumir
        // Esto desbloquea al consumidor si estaba esperando en full.acquire()
        // ──────────────────────────────────────────────────────
        full.release();  // ← DESBLOQUEO AL CONSUMIDOR (si estaba esperando)
        log.debug("[{}] Semáforo FULL incrementado (disponibles={})", threadName, full.availablePermits());
    }

    // ══════════════════════════════════════════════════════════
    //  OPERACIÓN CONSUMIR (llamada por el thread Consumidor)
    // ══════════════════════════════════════════════════════════

    /**
     * CONSUMIR: Extrae una solicitud del buffer.
     * ──────────────────────────────────────────
     * Implementa la lógica clásica del consumidor:
     *
     *   1. full.acquire()  → esperar si no hay ítems
     *   2. mutex.acquire() → entrar a la sección crítica
     *   3. buffer.poll()   → extraer del buffer
     *   4. mutex.release() → salir de la sección crítica
     *   5. empty.release() → señalizar que se liberó un espacio
     *
     * @return La solicitud extraída del buffer
     * @throws InterruptedException si el thread es interrumpido mientras espera
     */
    public SolicitudTicket consumir() throws InterruptedException {
        String threadName = Thread.currentThread().getName();

        log.debug("[{}] Consumidor esperando ítems en el buffer", threadName);

        // ──────────────────────────────────────────────────────
        // PASO 1: Verificar si hay ítems para consumir
        // Si full == 0, el consumidor SE BLOQUEA aquí hasta que
        // algún productor inserte algo (llame a full.release())
        // ──────────────────────────────────────────────────────
        log.debug("[{}] Esperando semáforo FULL (disponibles={})", threadName, full.availablePermits());
        full.acquire();  // ← POSIBLE PUNTO DE BLOQUEO DEL CONSUMIDOR
        log.debug("[{}] Semáforo FULL adquirido, hay ítem para procesar", threadName);

        // ──────────────────────────────────────────────────────
        // PASO 2: Entrar a la SECCIÓN CRÍTICA para extraer del buffer
        // ──────────────────────────────────────────────────────
        mutex.acquire();  // ← ENTRO A LA SECCIÓN CRÍTICA
        SolicitudTicket solicitud;
        try {
            // ── DENTRO DE LA SECCIÓN CRÍTICA ──────────────────
            // poll() extrae y elimina el primer elemento (FIFO)
            solicitud = buffer.poll();
            log.debug("[{}] ✅ Solicitud extraída del buffer: {}. Restantes: {}/{}",
                     threadName, solicitud != null ? solicitud.getClienteNombre() : "null",
                     buffer.size(), capacidad);
            // ── FIN DE LA SECCIÓN CRÍTICA ─────────────────────
        } finally {
            mutex.release();  // ← SALGO DE LA SECCIÓN CRÍTICA
        }

        // ──────────────────────────────────────────────────────
        // PASO 3: Señalizar que se liberó un espacio en el buffer
        // Esto desbloquea a cualquier productor que estaba esperando
        // en empty.acquire() porque el buffer estaba lleno.
        // ──────────────────────────────────────────────────────
        empty.release();  // ← DESBLOQUEO A UN PRODUCTOR (si había alguno esperando)
        log.debug("[{}] Semáforo EMPTY incrementado (disponibles={})", threadName, empty.availablePermits());

        return solicitud;
    }

    // ── Getters para observar el estado de los semáforos ─────

    public int getTamanioActual() { return buffer.size(); }
    public int getCapacidad() { return capacidad; }
    public int getMutexPermisos() { return mutex.availablePermits(); }
    public int getEmptyPermisos() { return empty.availablePermits(); }
    public int getFullPermisos() { return full.availablePermits(); }

    /** Limpia el buffer (para reiniciar la simulación) */
    public void limpiar() {
        buffer.clear();
        // Resetear semáforos
        // Drenar todos los permisos actuales
        mutex.drainPermits();
        empty.drainPermits();
        full.drainPermits();
        // Restaurar estado inicial
        mutex.release(1);
        empty.release(capacidad);
        log.info("Buffer reiniciado. Semáforos reseteados a estado inicial.");
    }
}
