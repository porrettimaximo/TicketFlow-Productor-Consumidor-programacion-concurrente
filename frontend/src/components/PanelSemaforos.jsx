/**
 * ============================================================
 * COMPONENTE: PanelSemaforos
 * ============================================================
 * Visualización educativa del estado de los tres semáforos:
 * MUTEX, EMPTY y FULL.
 *
 * Este es el componente más importante para la exposición:
 * muestra en tiempo real qué pasa con la sincronización.
 */
import React from 'react';
import { Lock, Layers, ArrowDownUp } from 'lucide-react';

const PanelSemaforos = ({ estado }) => {
  const mutex = estado?.semaforoMutexPermisos ?? 1;
  const empty = estado?.semaforoEmptyPermisos ?? 5;
  const full  = estado?.semaforoFullPermisos  ?? 0;
  const capacidadBuffer = estado?.capacidadBuffer ?? 5;
  const bufferActual = estado?.solicitudesEnBuffer ?? 0;
  const productoresActivos = estado?.threadProductoresActivos ?? 0;

  return (
    <div className="card-glass p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-8 h-8 rounded-lg bg-purple-500/20 flex items-center justify-center">
          <Lock size={16} className="text-purple-400" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-white">Estado de Semáforos</h2>
          <p className="text-xs text-slate-400">Mecanismos de sincronización en tiempo real</p>
        </div>
      </div>

      <div className="space-y-5">
        {/* SEMÁFORO MUTEX */}
        <SemaforoCard
          nombre="MUTEX"
          descripcion="Exclusión Mutua · Protege la Sección Crítica"
          tipo="binario"
          valor={mutex}
          maximo={1}
          colorActivo="purple"
          icono={<Lock size={16} />}
          explicacion={
            mutex === 0
              ? "🔒 BLOQUEADO — Un thread está en la sección crítica"
              : "🔓 LIBRE — Ningún thread en la sección crítica"
          }
          bloqueado={mutex === 0}
        />

        {/* SEMÁFORO EMPTY */}
        <SemaforoCard
          nombre="EMPTY"
          descripcion="Espacios Vacíos en el Buffer"
          tipo="contador"
          valor={empty}
          maximo={capacidadBuffer}
          colorActivo="cyan"
          icono={<Layers size={16} />}
          explicacion={
            empty === 0
              ? "⛔ BLOQUEADO — Buffer lleno, productores esperando"
              : `✅ ${empty} espacio(s) libre(s) en el buffer`
          }
          bloqueado={empty === 0}
        />

        {/* SEMÁFORO FULL */}
        <SemaforoCard
          nombre="FULL"
          descripcion="Ítems Disponibles para Consumir"
          tipo="contador"
          valor={full}
          maximo={capacidadBuffer}
          colorActivo="amber"
          icono={<ArrowDownUp size={16} />}
          explicacion={
            full === 0
              ? "⏸ BLOQUEADO — Buffer vacío, consumidor esperando"
              : `📦 ${full} solicitud(es) lista(s) para procesar`
          }
          bloqueado={full === 0}
        />
      </div>

      {/* Estado del buffer visual */}
      <div className="mt-6 p-4 bg-dark-700/50 rounded-xl border border-medical-500/10">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-semibold text-slate-300">Buffer Compartido</span>
          <span className="text-xs font-mono text-medical-400">
            [{bufferActual}/{capacidadBuffer}]
          </span>
        </div>

        {/* Representación visual del buffer como celdas */}
        <div className="flex gap-2">
          {Array.from({ length: capacidadBuffer }, (_, i) => (
            <div
              key={i}
              className={`flex-1 h-8 rounded-md border transition-all duration-300 flex items-center justify-center
                         ${i < bufferActual
                           ? 'bg-medical-500/40 border-medical-400/60 shadow-[0_0_8px_rgba(14,165,233,0.4)]'
                           : 'bg-dark-700/50 border-slate-700/50'}`}
            >
              {i < bufferActual && (
                <div className="w-2 h-2 rounded-full bg-medical-400 animate-pulse" />
              )}
            </div>
          ))}
        </div>

        <div className="flex justify-between text-xs text-slate-500 mt-2">
          <span>Vacío</span>
          <span>{bufferActual} solicitudes en cola</span>
          <span>Lleno</span>
        </div>
      </div>

      {/* Threads productores activos */}
      <div className="mt-4 flex items-center justify-between p-3 bg-dark-700/30 rounded-lg border border-slate-700/30">
        <span className="text-xs text-slate-400">Threads Productores Activos</span>
        <div className="flex items-center gap-2">
          {productoresActivos > 0 ? (
            <>
              {Array.from({ length: Math.min(productoresActivos, 10) }, (_, i) => (
                <div key={i} className="w-2 h-2 rounded-full bg-green-400 animate-pulse"
                     style={{ animationDelay: `${i * 0.15}s` }} />
              ))}
              {productoresActivos > 10 && (
                <span className="text-xs text-green-400">+{productoresActivos - 10}</span>
              )}
              <span className="text-xs font-bold text-green-400 ml-1">{productoresActivos}</span>
            </>
          ) : (
            <span className="text-xs text-slate-500">Ninguno activo</span>
          )}
        </div>
      </div>
    </div>
  );
};

/* ── Tarjeta individual de semáforo ── */
const SemaforoCard = ({ nombre, descripcion, tipo, valor, maximo, colorActivo, icono, explicacion, bloqueado }) => {
  const porcentaje = maximo > 0 ? (valor / maximo) * 100 : 0;

  const colorMap = {
    purple: {
      bg: 'bg-purple-500/10', border: 'border-purple-500/30',
      text: 'text-purple-400', fill: '#a855f7',
      glow: 'shadow-[0_0_15px_rgba(168,85,247,0.5)]'
    },
    cyan: {
      bg: 'bg-fuchsia-500/10', border: 'border-fuchsia-500/30',
      text: 'text-fuchsia-400', fill: '#06b6d4',
      glow: 'shadow-[0_0_15px_rgba(6,182,212,0.5)]'
    },
    amber: {
      bg: 'bg-amber-500/10', border: 'border-amber-500/30',
      text: 'text-amber-400', fill: '#f59e0b',
      glow: 'shadow-[0_0_15px_rgba(245,158,11,0.5)]'
    },
  };

  const c = colorMap[colorActivo] || colorMap.cyan;
  const isBloqueado = bloqueado || valor === 0;

  return (
    <div className={`p-4 rounded-xl border ${c.bg} ${c.border}
                     ${isBloqueado ? 'opacity-70' : ''}
                     transition-all duration-300`}>
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          {/* Indicador semáforo (círculo) */}
          <div className={`w-8 h-8 rounded-full flex items-center justify-center
                          ${!isBloqueado ? `${c.bg} ${c.glow} semaphore-active` : 'bg-slate-700/50'}
                          transition-all duration-500`}>
            <span className={`${c.text} ${isBloqueado ? 'opacity-40' : ''}`}>{icono}</span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-white text-sm">{nombre}</span>
              <span className={`text-xs px-1.5 py-0.5 rounded ${isBloqueado ? 'bg-red-500/20 text-red-400' : 'bg-green-500/20 text-green-400'}`}>
                {isBloqueado ? 'WAIT' : tipo === 'binario' ? 'LIBRE' : 'OK'}
              </span>
            </div>
            <span className="text-xs text-slate-400">{descripcion}</span>
          </div>
        </div>

        {/* Valor del semáforo */}
        <div className={`font-mono font-bold text-2xl ${c.text}`}>
          {valor}
          {tipo === 'contador' && <span className="text-sm text-slate-500">/{maximo}</span>}
        </div>
      </div>

      {/* Barra de estado */}
      {tipo === 'contador' && (
        <div className="h-1.5 bg-dark-700 rounded-full overflow-hidden mt-2">
          <div
            className="h-full rounded-full transition-all duration-500"
            style={{
              width: `${porcentaje}%`,
              backgroundColor: c.fill,
              boxShadow: `0 0 8px ${c.fill}80`
            }}
          />
        </div>
      )}

      {/* Explicación del estado */}
      <p className="text-xs text-slate-400 mt-2 italic">{explicacion}</p>
    </div>
  );
};

export default PanelSemaforos;
