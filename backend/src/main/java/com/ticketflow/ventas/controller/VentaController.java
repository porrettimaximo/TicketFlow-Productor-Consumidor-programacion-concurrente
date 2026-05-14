package com.ticketflow.ventas.controller;

import com.ticketflow.ventas.model.EstadoSistema;
import com.ticketflow.ventas.service.SimulacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================
 * CONTROLADOR REST - API de TicketFlow
 * ============================================================
 * Expone los endpoints HTTP que el frontend React consume.
 *
 * Endpoints:
 *   GET  /api/estado     → Estado actual del sistema (polling)
 *   POST /api/iniciar    → Iniciar nueva simulación
 *   POST /api/reset      → Resetear el sistema
 *   GET  /api/config     → Configuración actual
 *
 * El frontend hace polling al endpoint /api/estado cada 1
 * segundo para actualizar el dashboard en tiempo real.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VentaController {

    private final SimulacionService simulacionService;

    /**
     * GET /api/estado
     * ─────────────────────────────────────────────────────────
     * Retorna el estado completo del sistema.
     * El frontend lo llama cada 1 segundo (polling) para
     * actualizar: semáforos, buffer, estadísticas, logs, tabla.
     */
    @GetMapping("/estado")
    public ResponseEntity<EstadoSistema> getEstado() {
        return ResponseEntity.ok(simulacionService.getEstado());
    }

    /**
     * POST /api/iniciar
     * ─────────────────────────────────────────────────────────
     * Inicia la simulación: lanza threads productores y consumidor.
     * No bloquea la petición HTTP: los threads corren en background.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar(@RequestBody(required = false) Map<String, Integer> config) {
        log.info("POST /api/iniciar - Iniciando simulación");

        if (simulacionService.isSimulacionActiva()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", "Ya hay una simulación activa. Usa /reset primero."
            ));
        }

        if (config != null) {
            if (config.containsKey("tickets")) {
                simulacionService.setCapacidadEvento(config.get("tickets"));
            }
            if (config.containsKey("clientes")) {
                simulacionService.setCantidadClientes(config.get("clientes"));
            }
        }

        simulacionService.iniciarSimulacion();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Simulación iniciada",
                "clientes", simulacionService.getCantidadClientes(),
                "tickets", simulacionService.getCapacidadEvento()
        ));
    }

    /**
     * POST /api/reset
     * ─────────────────────────────────────────────────────────
     * Detiene todos los threads, limpia el estado del sistema
     * y permite iniciar una nueva simulación desde cero.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        log.info("POST /api/reset - Reseteando simulación");
        simulacionService.resetearSimulacion();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Sistema reseteado correctamente"
        ));
    }

    /**
     * GET /api/config
     * ─────────────────────────────────────────────────────────
     * Retorna la configuración actual del sistema.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "capacidadEvento", simulacionService.getCapacidadEvento(),
                "cantidadClientes", simulacionService.getCantidadClientes(),
                "descripcion", "Sistema de tickets con Productor-Consumidor + Semáforos"
        ));
    }

    /**
     * GET /api/health
     * ─────────────────────────────────────────────────────────
     * Health check básico para verificar que el backend está vivo.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "TicketFlow",
                "version", "1.0.0"
        ));
    }
}
