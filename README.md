#  TicketFlow

> **Sistema de Tickets a Exámenes Clínicos con Control de Concurrencia**  
> Implementación del patrón **Productor-Consumidor** usando **Semáforos** en Java <br>
> Video: https://youtu.be/yBYnTCtBKV0?si=Cf7mafTTyTaVpAVt

---

##  Descripción del Proyecto

**TicketFlow** es una plataforma de venta de entradas para conciertos y eventos masivos de alta demanda (Virtual Waiting Room). Simula el proceso de compra concurrente de múltiples usuarios intentando adquirir tickets limitados simultáneamente.

El proyecto demuestra de forma visual e interactiva los conceptos fundamentales de **programación concurrente**:

| Concepto | Implementación |
|---|---|
| **Patrón** | Productor-Consumidor |
| **Sincronización** | `java.util.concurrent.Semaphore` |
| **Buffer compartido** | Cola FIFO con acceso protegido |
| **Productores** | Threads que simulan clientes solicitando reserva |
| **Consumidor** | Thread único que procesa tickets |
| **Exclusión Mutua** | Semáforo binario MUTEX |
| **Control de flujo** | Semáforos contador EMPTY y FULL |

---

##  Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    TicketFlow                     │
│                                                             │
│  Frontend (React + TailwindCSS)   Backend (Spring Boot)     │
│  ┌──────────────────────┐         ┌───────────────────┐     │
│  │  Dashboard Visual    │◄──────► │   API REST        │     │
│  │  - Panel Semáforos   │ HTTP    │   /api/estado     │     │
│  │  - Panel Tickets       │ Polling │   /api/iniciar    │     │
│  │  - Logs en Tiempo    │ 1s      │   /api/reset      │     │
│  │  - Tabla Registros   │         │                   │     │
│  └──────────────────────┘         └────────┬──────────┘     │
│                                            │                │
│                                   ┌────────▼──────────┐     │
│                                   │  SimulacionService │     │
│                                   │                   │     │
│                            ┌──────┼──────────────────┐│     │
│                            │ P1   │ P2  │ P3  │ ...  ││     │
│                            │Thread│Thread│Thread│     ││     │
│                            └───┬──┴──┬──┴──┬──┴──────┘│     │
│                                └─────┼─────┘          │     │
│                                      ▼                 │     │
│                            ┌─────────────────┐         │     │
│                            │ BufferCompartido│         │     │
│                            │ mutex=Semaphore │         │     │
│                            │ empty=Semaphore │         │     │
│                            │ full =Semaphore │         │     │
│                            └────────┬────────┘         │     │
│                                     ▼                  │     │
│                            ┌─────────────────┐         │     │
│                            │ ThreadConsumidor│         │     │
│                            │ (1 thread)      │         │     │
│                            └─────────────────┘         │     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧵 Los Tres Semáforos (Diagrama de Dijkstra)

```java
// MUTEX: Exclusión mutua - protege la sección crítica
Semaphore mutex = new Semaphore(1);  // Binario: 0 o 1

// EMPTY: Espacios vacíos en el buffer - bloquea productores si lleno
Semaphore empty = new Semaphore(N);  // Contador: 0 a N

// FULL: Ítems disponibles - bloquea consumidor si vacío
Semaphore full  = new Semaphore(0);  // Contador: 0 a N

// ── PRODUCTOR (cliente) ──────────────────────
empty.acquire();  // Esperar espacio (bloquea si buffer lleno)
mutex.acquire();  // Entrar a sección crítica
  buffer.add(solicitud);  // ← SECCIÓN CRÍTICA
mutex.release();  // Salir de sección crítica
full.release();   // Señalizar ítem disponible

// ── CONSUMIDOR (sistema) ────────────────────
full.acquire();   // Esperar ítem (bloquea si buffer vacío)
mutex.acquire();  // Entrar a sección crítica
  solicitud = buffer.poll();  // ← SECCIÓN CRÍTICA
mutex.release();  // Salir de sección crítica
empty.release();  // Señalizar espacio liberado
```

---

## 📁 Estructura del Proyecto

```
ticketflow/
├── 📁 backend/
│   ├── pom.xml
│   └── src/main/java/com/medsim/tickets/
│       ├── TicketFlowApplication.java      ← Punto de entrada
│       ├── config/
│       │   └── WebConfig.java                 ← Configuración CORS
│       ├── controller/
│       │   └── VentaController.java     ← API REST
│       ├── concurrent/                        ← ⭐ NÚCLEO CONCURRENTE
│       │   ├── BufferCompartido.java          ← Buffer + 3 Semáforos
│       │   ├── ThreadProductor.java           ← Thread Productor
│       │   └── ThreadConsumidor.java          ← Thread Consumidor
│       ├── model/
│       │   ├── SolicitudTicket.java      ← Entidad principal
│       │   └── EstadoSistema.java             ← DTO para el frontend
│       └── service/
│           ├── SimulacionService.java         ← Orquestador
│           ├── RegistroService.java           ← Persistencia en memoria
│           └── LogService.java                ← Sistema de logs
│
└── 📁 frontend/
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    ├── index.html
    └── src/
        ├── App.jsx                            ← Dashboard principal
        ├── main.jsx                           ← Entry point React
        ├── index.css                          ← Estilos globales
        ├── hooks/
        │   └── useSimulacion.js               ← Hook de polling
        ├── services/
        │   └── api.js                         ← Llamadas al backend
        └── components/
            ├── Header.jsx                     ← Barra superior
            ├── PanelControles.jsx             ← Botones + estadísticas
            ├── PanelSemaforos.jsx             ← ⭐ Visualización semáforos
            ├── PanelTickets.jsx                 ← Gráfico de tickets
            ├── PanelLogs.jsx                  ← Consola en tiempo real
            └── TablaSolicitudes.jsx         ← Tabla de solicitudes
```

---

## 🚀 Cómo Ejecutar

### Prerequisitos

- **Java 17+** (`java -version`)
- **Maven 3.6+** (`mvn -version`)
- **Node.js 18+** (`node -version`)
- **npm** (`npm -version`)

### 1. Iniciar el Backend (Spring Boot)

```bash
cd ticketflow/backend

# Compilar y ejecutar
mvn spring-boot:run

# El servidor inicia en http://localhost:8080
# Verificar: http://localhost:8080/api/health
```

### 2. Instalar dependencias del Frontend

```bash
cd ticketflow/frontend
npm install
```

### 3. Iniciar el Frontend (React + Vite)

```bash
npm run dev

# La aplicación abre en http://localhost:5173
```

### 4. Usar el sistema

1. Abre `http://localhost:5173` en el navegador
2. Verifica que el indicador "Backend Conectado" esté en verde
3. Haz clic en **"Iniciar Simulación"**
4. Observa en tiempo real:
   - Los semáforos MUTEX, EMPTY y FULL
   - El buffer llenándose y vaciándose
   - Los logs del sistema
   - Las tickets aprobadas y rechazadas
5. Haz clic en **"Reset"** para reiniciar

---

## ⚙️ Configuración

Editando `backend/src/main/resources/application.properties`:

```properties
# Tickets del evento (clientes que pueden inscribirse)
simulation.exam.capacity=10

# Tamaño del buffer (solicitudes en cola)
simulation.buffer.size=5

# Cantidad de clientes simulados (threads productores)
simulation.students.count=20

# Tiempo de procesamiento por solicitud (milisegundos)
simulation.processing.min-time=500
simulation.processing.max-time=2000
```
