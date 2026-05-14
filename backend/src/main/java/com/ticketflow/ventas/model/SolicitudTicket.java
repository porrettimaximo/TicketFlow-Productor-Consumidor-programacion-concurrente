package com.ticketflow.ventas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * MODELO: SolicitudTicket
 * ============================================================
 * Representa una solicitud de un cliente para inscribirse
 * a un evento musical. Este objeto viaja por el BUFFER
 * del sistema Productor-Consumidor.
 *
 * ANALOGÍA:
 * - El cliente es el PRODUCTOR (genera la solicitud)
 * - Esta solicitud es el ÍTEM que se pone en el buffer
 * - El sistema de tickets es el CONSUMIDOR (la procesa)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTicket {

    /** ID único de la solicitud */
    private String id;

    /** ID del cliente que solicita la reserva */
    private String clienteId;

    /** Nombre del cliente */
    private String clienteNombre;

    /** ID del evento al que quiere inscribirse */
    private String eventoId;

    /** Nombre del evento musical */
    private String eventoNombre;

    /**
     * Estado actual de la solicitud.
     * Transiciones posibles:
     *   PENDIENTE → EN_PROCESO → ACEPTADA
     *                          → RECHAZADA
     */
    private EstadoSolicitud estado;

    /** Momento en que el cliente generó la solicitud */
    private LocalDateTime timestampCreacion;

    /** Momento en que el sistema procesó la solicitud */
    private LocalDateTime timestampProcesado;

    /** Mensaje explicativo del resultado del procesamiento */
    private String mensaje;

    /** Tiempo que tardó en procesarse (ms) - para visualización */
    private long tiempoProcesamiento;

    /**
     * Estados posibles de una solicitud de reserva
     */
    public enum EstadoSolicitud {
        /** La solicitud fue generada y está esperando en el buffer */
        PENDIENTE,

        /** El consumidor tomó la solicitud del buffer y la está procesando */
        EN_PROCESO,

        /** El cliente fue inscripto exitosamente (había ticket disponible) */
        ACEPTADA,

        /** El cliente no pudo inscribirse (sin tickets disponibles) */
        RECHAZADA
    }
}
