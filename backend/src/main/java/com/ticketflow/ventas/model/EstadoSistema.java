package com.ticketflow.ventas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ============================================================
 * MODELO: EstadoSistema
 * ============================================================
 * Snapshot del estado actual del sistema en un momento dado.
 * Este objeto es lo que el frontend consume via polling para
 * actualizar la interfaz visual en tiempo real.
 *
 * Representa el "fotografía" completa del sistema incluyendo:
 * - Estado de los semáforos
 * - Contenido del buffer
 * - Estadísticas globales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoSistema {

    // ── Estado del buffer ─────────────────────────────────────
    /** Cantidad de solicitudes actualmente en el buffer/cola */
    private int solicitudesEnBuffer;

    /** Capacidad máxima del buffer (tamaño fijo) */
    private int capacidadBuffer;

    // ── Estado del evento ─────────────────────────────────────
    /** Tickets totales del evento (fijo) */
    private int ticketsTotales;

    /** Tickets ya utilizados (clientes aprobados) */
    private int ticketsOcupados;

    /** Tickets que quedan disponibles */
    private int ticketsDisponibles;

    // ── Estadísticas globales ─────────────────────────────────
    /** Total de solicitudes que llegaron al sistema */
    private int totalSolicitudes;

    /** Solicitudes aprobadas exitosamente */
    private int solicitudesAprobadas;

    /** Solicitudes rechazadas por falta de ticket */
    private int solicitudesAgotadas;

    /** Solicitudes pendientes de procesar */
    private int solicitudesPendientes;

    // ── Estado de la simulación ───────────────────────────────
    /** Indica si hay una simulación en curso */
    private boolean simulacionActiva;

    /** Cantidad de threads productores activos actualmente */
    private int threadProductoresActivos;

    // ── Estado de los semáforos (para visualización) ──────────
    /**
     * Semáforo MUTEX: cuántos permisos disponibles (0 o 1)
     * Si es 0: hay un thread en la sección crítica
     * Si es 1: la sección crítica está libre
     */
    private int semaforoMutexPermisos;

    /**
     * Semáforo EMPTY: cuántos espacios vacíos quedan en el buffer
     * Cuando llega a 0, los productores se bloquean
     */
    private int semaforoEmptyPermisos;

    /**
     * Semáforo FULL: cuántos ítems hay disponibles para consumir
     * Cuando es 0, el consumidor se bloquea esperando
     */
    private int semaforoFullPermisos;

    // ── Datos para la tabla ───────────────────────────────────
    /** Lista de los últimos 50 registros de solicitudes */
    private List<SolicitudTicket> ultimasSolicitudes;

    // ── Logs del sistema ──────────────────────────────────────
    /** Últimas entradas del log del sistema */
    private List<LogEntry> logs;

    /**
     * Entrada individual del log del sistema
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private String timestamp;
        private String nivel;    // INFO, WARN, SUCCESS, ERROR
        private String mensaje;
        private String thread;
    }
}
