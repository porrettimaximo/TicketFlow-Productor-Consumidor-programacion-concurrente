import React from 'react';
import { Activity, Wifi, WifiOff, Ticket, RefreshCcw, CheckCircle2 } from 'lucide-react';

const Header = ({ backendConectado, simulacionActiva, showReset, onReset }) => {
  return (
    <header className="relative bg-dark-800/90 backdrop-blur-md border-b border-purple-500/20 px-6 py-4">
      {/* Línea decorativa superior */}
      <div className="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-purple-500 to-transparent" />

      <div className="max-w-screen-2xl mx-auto flex items-center justify-between">
        {/* Logo y título */}
        <div className="flex items-center gap-4">
          <div className="relative">
            <div className="w-10 h-10 rounded-xl bg-purple-600/30 border border-purple-500/40
                            flex items-center justify-center">
              <Ticket size={20} className="text-purple-400" />
            </div>
            {/* Punto de actividad */}
            {simulacionActiva && (
              <span className="absolute -top-1 -right-1 w-3 h-3 bg-green-500 rounded-full
                               border-2 border-dark-800 animate-pulse" />
            )}
          </div>

          <div>
            <h1 className="text-xl font-bold text-white leading-none">
              TicketFlow
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">
              Virtual Waiting Room · Productor-Consumidor
            </p>
          </div>
        </div>

        {/* Estado del sistema y Controles */}
        <div className="flex items-center gap-4">
          {showReset && (
            <button
              onClick={onReset}
              className="px-4 py-1.5 bg-red-700/80 hover:bg-red-600 text-white text-sm font-semibold rounded-lg flex items-center gap-2 transition-all active:scale-95"
            >
              <RefreshCcw size={14} />
              Resetear
            </button>
          )}

          {/* Indicador de simulación activa */}
          {simulacionActiva ? (
            <div className="live-indicator active gap-2">
              <Activity size={12} className="animate-pulse" />
              <span>EN VIVO</span>
            </div>
          ) : showReset ? (
            <div className="live-indicator gap-2 bg-blue-500/20 text-blue-400 border border-blue-500/30">
              <CheckCircle2 size={12} />
              <span>FINALIZADO</span>
            </div>
          ) : (
            <div className="live-indicator inactive gap-2">
              <span>ESPERANDO</span>
            </div>
          )}

          {/* Estado de conexión al backend */}
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium
                          ${backendConectado
                            ? 'bg-green-500/10 text-green-400 border border-green-500/20'
                            : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
            {backendConectado
              ? <><Wifi size={12} /> OK</>
              : <><WifiOff size={12} /> Sin Conexión</>
            }
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
