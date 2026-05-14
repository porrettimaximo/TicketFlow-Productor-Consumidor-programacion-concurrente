/**
 * ============================================================
 * COMPONENTE: PanelLogs
 * ============================================================
 * Consola de logs en tiempo real que muestra los eventos
 * del sistema: cuándo llegan solicitudes, cuándo se bloquean
 * threads, resultados de tickets, etc.
 */
import React, { useRef, useEffect, useState } from 'react';
import { Terminal, ChevronDown } from 'lucide-react';

const PanelLogs = ({ logs = [] }) => {
  const logsEndRef = useRef(null);
  const containerRef = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // Auto-scroll al final cuando llegan nuevos logs
  useEffect(() => {
    if (autoScroll && containerRef.current) {
      // Usamos directamente el scrollTop del contenedor para evitar que 
      // la ventana principal (el body) salte hacia el componente.
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const handleScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;
    // Si el usuario scrollea hacia arriba, desactivamos el auto-scroll
    // Dejamos un margen de 10px
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 10;
    setAutoScroll(isAtBottom);
  };

  const getLevelIcon = (nivel) => {
    const icons = {
      INFO: 'ℹ',
      SUCCESS: '✓',
      WARN: '⚠',
      ERROR: '✗',
    };
    return icons[nivel] || 'ℹ';
  };

  return (
    <div className="card-glass p-6 flex flex-col w-full h-[320px]">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-green-500/20 flex items-center justify-center">
            <Terminal size={16} className="text-green-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Log del Sistema</h2>
            <p className="text-xs text-slate-400">Eventos en tiempo real</p>
          </div>
        </div>

        {/* Indicador de auto-scroll */}
        <div className="flex items-center gap-2">
          {!autoScroll && (
            <button
              onClick={() => {
                setAutoScroll(true);
                logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
              }}
              className="flex items-center gap-1 px-2 py-1 rounded text-xs
                         bg-medical-500/20 text-medical-400 hover:bg-medical-500/30 transition-colors"
            >
              <ChevronDown size={12} />
              Ir al final
            </button>
          )}
          <span className="text-xs font-mono text-slate-500">
            {logs.length} eventos
          </span>
        </div>
      </div>

      {/* Consola de logs */}
      <div
        ref={containerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto bg-dark-900/80 rounded-xl border border-slate-700/30 p-3
                   font-mono text-xs scroll-smooth"
        style={{ minHeight: '0' }}
      >
        {logs.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-600 gap-3">
            <Terminal size={24} />
            <span>El sistema está en espera...</span>
            <span className="text-xs">Inicia la simulación para ver los logs</span>
          </div>
        ) : (
          <>
            {/* Los logs vienen en orden descendente (más reciente primero),
                pero los mostramos en orden ascendente para que se lea naturalmente */}
            {[...logs].reverse().map((log, idx) => (
              <div
                key={idx}
                className={`log-entry ${log.nivel || 'INFO'} animate-slide-in`}
              >
                <span className="text-slate-600">{log.timestamp}</span>
                {' '}
                <span className="font-semibold">[{getLevelIcon(log.nivel)} {log.nivel}]</span>
                {' '}
                <span className="text-slate-300 text-xs italic">({log.thread})</span>
                {' '}
                <span>{log.mensaje}</span>
              </div>
            ))}
            <div ref={logsEndRef} />
          </>
        )}
      </div>

      {/* Leyenda */}
      <div className="flex gap-4 mt-3">
        {[
          { nivel: 'INFO', color: 'text-purple-400', icon: 'ℹ' },
          { nivel: 'SUCCESS', color: 'text-green-400', icon: '✓' },
          { nivel: 'WARN', color: 'text-yellow-400', icon: '⚠' },
          { nivel: 'ERROR', color: 'text-red-400', icon: '✗' },
        ].map(({ nivel, color, icon }) => (
          <div key={nivel} className={`flex items-center gap-1 text-xs ${color}`}>
            <span>{icon}</span>
            <span>{nivel}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PanelLogs;
