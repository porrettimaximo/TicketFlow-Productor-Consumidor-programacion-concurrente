package com.ticketflow.ventas.service;

import com.ticketflow.ventas.model.EstadoSistema.LogEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * ============================================================
 * SERVICIO DE LOGS
 * ============================================================
 * Mantiene un historial de los últimos N eventos del sistema
 * para mostrarlos en el panel de logs del frontend.
 *
 * Thread-safe: usa Collections.synchronizedList para que
 * múltiples threads puedan agregar logs sin condiciones de carrera.
 */
@Service
public class LogService {

    /** Máximo de entradas a mantener en memoria */
    private static final int MAX_LOGS = 100;

    /** Lista sincronizada (thread-safe) de entradas de log */
    private final List<LogEntry> logs = Collections.synchronizedList(new LinkedList<>());

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Agrega una entrada al log del sistema.
     *
     * @param nivel   INFO, SUCCESS, WARN, ERROR
     * @param mensaje Texto descriptivo del evento
     * @param thread  Nombre del thread que generó el evento
     */
    public void agregarLog(String nivel, String mensaje, String thread) {
        LogEntry entry = LogEntry.builder()
                .timestamp(LocalDateTime.now().format(formatter))
                .nivel(nivel)
                .mensaje(mensaje)
                .thread(thread)
                .build();

        synchronized (logs) {
            logs.add(0, entry); // Agregar al principio (más reciente primero)
            // Mantener solo los últimos MAX_LOGS
            while (logs.size() > MAX_LOGS) {
                logs.remove(logs.size() - 1);
            }
        }
    }

    /** @return Copia de los logs actuales */
    public List<LogEntry> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    /** Limpia todos los logs */
    public void limpiar() {
        logs.clear();
    }
}
