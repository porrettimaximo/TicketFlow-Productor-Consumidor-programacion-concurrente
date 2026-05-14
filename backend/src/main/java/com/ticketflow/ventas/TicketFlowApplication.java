package com.ticketflow.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * MEDSIM-INSCRIPCIONES - Punto de entrada principal
 * ============================================================
 * Sistema que demuestra el problema Productor-Consumidor
 * aplicado a la reserva concurrente de clientes a exámenes.
 *
 * CONCEPTOS CLAVE:
 * - Semáforos: mecanismos de sincronización que controlan acceso
 *   a recursos compartidos entre múltiples threads.
 * - Buffer: estructura de datos compartida entre productores y consumidor.
 * - Sección Crítica: zona de código donde solo un thread puede estar a la vez.
 */
@SpringBootApplication
public class TicketFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketFlowApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  TicketFlow - Servidor iniciado");
        System.out.println("  http://localhost:8080");
        System.out.println("  Patrón: Productor-Consumidor con Semáforos");
        System.out.println("==============================================");
    }
}
