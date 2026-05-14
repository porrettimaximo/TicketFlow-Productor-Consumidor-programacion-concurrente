package com.ticketflow.ventas.service;

import com.ticketflow.ventas.model.SolicitudTicket;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * SERVICIO DE REGISTRO
 * ============================================================
 * Almacena en memoria el registro de todas las solicitudes
 * de reserva procesadas por el sistema.
 *
 * En un sistema real, esto persistiría en base de datos.
 * Para esta simulación usamos estructuras en memoria.
 *
 * THREAD-SAFETY:
 * ConcurrentHashMap para acceso thread-safe sin bloqueos
 * de alto costo. Múltiples threads pueden leer/escribir
 * simultáneamente de forma segura.
 */
@Service
public class RegistroService {

    /**
     * ConcurrentHashMap: almacenamiento thread-safe de solicitudes.
     * Clave: ID de la solicitud
     * Valor: La solicitud completa
     *
     * A diferencia de HashMap, ConcurrentHashMap maneja
     * internamente la sincronización sin bloquear toda la tabla.
     */
    private final ConcurrentHashMap<String, SolicitudTicket> solicitudes
            = new ConcurrentHashMap<>();

    /**
     * Registra una nueva solicitud en el sistema.
     */
    public void registrarSolicitud(SolicitudTicket solicitud) {
        solicitudes.put(solicitud.getId(), solicitud);
    }

    /**
     * Actualiza el estado de una solicitud existente.
     * Se llama cuando el consumidor cambia el estado (EN_PROCESO, ACEPTADA, RECHAZADA).
     */
    public void actualizarSolicitud(SolicitudTicket solicitud) {
        solicitudes.put(solicitud.getId(), solicitud);
    }

    /**
     * @return Lista de todas las solicitudes, ordenadas por timestamp de creación (más reciente primero)
     */
    public List<SolicitudTicket> getTodas() {
        List<SolicitudTicket> lista = new ArrayList<>(solicitudes.values());
        lista.sort((a, b) -> {
            if (a.getTimestampCreacion() == null) return 1;
            if (b.getTimestampCreacion() == null) return -1;
            return b.getTimestampCreacion().compareTo(a.getTimestampCreacion());
        });
        return lista;
    }

    /** @return Cantidad de solicitudes aprobadas */
    public long getCountAprobadas() {
        return solicitudes.values().stream()
                .filter(s -> s.getEstado() == SolicitudTicket.EstadoSolicitud.ACEPTADA)
                .count();
    }

    /** @return Cantidad de solicitudes rechazadas */
    public long getCountAgotadas() {
        return solicitudes.values().stream()
                .filter(s -> s.getEstado() == SolicitudTicket.EstadoSolicitud.RECHAZADA)
                .count();
    }

    /** @return Cantidad de solicitudes pendientes o en proceso */
    public long getCountPendientes() {
        return solicitudes.values().stream()
                .filter(s -> s.getEstado() == SolicitudTicket.EstadoSolicitud.PENDIENTE
                          || s.getEstado() == SolicitudTicket.EstadoSolicitud.EN_PROCESO)
                .count();
    }

    /** @return Total de solicitudes registradas */
    public int getTotal() { return solicitudes.size(); }

    /** Limpia todos los registros */
    public void limpiar() { solicitudes.clear(); }
}
