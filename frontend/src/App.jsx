/**
 * ============================================================
 * APP PRINCIPAL - Dashboard TicketFlow
 * ============================================================
 */
import React, { useState } from 'react';
import { useSimulacion } from './hooks/useSimulacion';
import Header from './components/Header';
import PanelSemaforos from './components/PanelSemaforos';
import PanelTickets from './components/PanelTickets';
import PanelLogs from './components/PanelLogs';
import TablaSolicitudes from './components/TablaSolicitudes';
import { Play } from 'lucide-react';

function App() {
  const {
    estado,
    cargando,
    error,
    backendConectado,
    handleIniciar,
    handleReset,
  } = useSimulacion();

  const [configTickets, setConfigTickets] = useState(10);
  const [configClientes, setConfigClientes] = useState(20);

  const simulacionActiva = estado?.simulacionActiva ?? false;
  const hasData = (estado?.totalSolicitudes ?? 0) > 0;
  const showDashboard = simulacionActiva || hasData;

  const onIniciarClick = () => {
    handleIniciar({
      tickets: configTickets,
      clientes: configClientes
    });
  };

  return (
    <div className="min-h-screen bg-dark-900 bg-grid flex flex-col">
      {/* ── Degradado de fondo animado ── */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-purple-700/10 rounded-full blur-3xl" />
        <div className="absolute top-1/2 -right-40 w-96 h-96 bg-fuchsia-700/8 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 left-1/3 w-96 h-96 bg-purple-700/8 rounded-full blur-3xl" />
      </div>

      <div className="relative z-10 flex-1 flex flex-col">
        {/* ── Header ── */}
        <Header
          backendConectado={backendConectado}
          simulacionActiva={simulacionActiva}
          showReset={showDashboard}
          onReset={handleReset}
        />

        {/* ── Banner de error ── */}
        {error && (
          <div className="mx-6 mt-4 p-4 bg-red-900/30 border border-red-500/30 rounded-xl
                          text-red-300 text-sm flex items-center gap-3 animate-fade-in">
            <span className="text-red-400 text-lg">⚠</span>
            <div>
              <strong>Error de conexión:</strong> {error}
              <p className="text-xs text-red-400 mt-1">
                Asegúrate de que el backend esté corriendo en <code>http://localhost:8080</code>
              </p>
            </div>
          </div>
        )}

        {/* ── Contenido principal ── */}
        <main className="flex-1 w-full max-w-[1600px] mx-auto px-6 py-6 flex flex-col gap-6">
          
          {!showDashboard ? (
            /* ── PANEL DE CONFIGURACIÓN PRE-SIMULACIÓN ── */
            <div className="flex-1 flex flex-col items-center justify-center animate-fade-in">
              <div className="card-glass p-8 max-w-md w-full flex flex-col items-center gap-6">
                <h2 className="text-2xl font-bold text-white text-center">
                  Configurar Venta
                </h2>
                <p className="text-slate-400 text-sm text-center mb-2">
                  Ajusta los parámetros para la sala de espera virtual antes de iniciar la venta concurrente.
                </p>
                
                <div className="w-full space-y-4">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-purple-400">Total de Tickets Disponibles</label>
                    <input 
                      type="number" 
                      min="1" max="1000"
                      value={configTickets}
                      onChange={(e) => setConfigTickets(parseInt(e.target.value) || 0)}
                      className="w-full bg-dark-900/50 border border-purple-500/30 rounded-lg px-4 py-2 text-white outline-none focus:border-purple-500 transition-colors"
                    />
                  </div>
                  
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-fuchsia-400">Clientes Simulados (Usuarios)</label>
                    <input 
                      type="number" 
                      min="1" max="1000"
                      value={configClientes}
                      onChange={(e) => setConfigClientes(parseInt(e.target.value) || 0)}
                      className="w-full bg-dark-900/50 border border-fuchsia-500/30 rounded-lg px-4 py-2 text-white outline-none focus:border-fuchsia-500 transition-colors"
                    />
                  </div>
                </div>

                <button
                  onClick={onIniciarClick}
                  disabled={cargando || !backendConectado}
                  className="w-full mt-4 btn-primary bg-purple-600 hover:bg-purple-500 flex items-center justify-center gap-2 py-4 text-lg"
                >
                  <Play size={20} />
                  {cargando ? 'Iniciando...' : 'Iniciar Simulación'}
                </button>
              </div>
            </div>
          ) : (
            /* ── DASHBOARD COMPACTO EN VIVO/FINALIZADO ── */
            <div className="flex-1 animate-fade-in grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              {/* Columna Izquierda: Semáforos */}
              <div className="lg:col-span-3">
                <PanelSemaforos estado={estado} />
              </div>

              {/* Área Derecha: Tickets, Logs y Tabla (9 columnas) */}
              <div className="lg:col-span-9 flex flex-col gap-6">
                
                {/* Fila superior: Tickets y Logs (mitad y mitad) */}
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
                  <div className="h-full">
                    <PanelTickets estado={estado} />
                  </div>
                  <div className="h-full">
                    <PanelLogs logs={estado?.logs ?? []} />
                  </div>
                </div>

                {/* Fila inferior: Tabla ocupando todo el ancho disponible hacia la derecha */}
                <div className="w-full">
                  <TablaSolicitudes solicitudes={estado?.ultimasSolicitudes ?? []} />
                </div>
              </div>
            </div>
          )}

        </main>
      </div>
    </div>
  );
}

export default App;
