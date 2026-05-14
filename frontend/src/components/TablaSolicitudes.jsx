/**
 * ============================================================
 * COMPONENTE: TablaSolicitudes
 * ============================================================
 * Tabla con el registro de todas las solicitudes de reserva.
 * Muestra: nombre, ID, estado, tiempo de procesamiento y mensaje.
 * Las filas nuevas tienen animación de entrada.
 */
import React, { useState, useRef, useEffect } from 'react';
import { ClipboardList, ChevronUp, ChevronDown, CheckSquare } from 'lucide-react';

const TablaSolicitudes = ({ solicitudes = [] }) => {
  const [ordenCampo, setOrdenCampo] = useState('timestampCreacion');
  const [ordenAsc, setOrdenAsc] = useState(false);
  const containerRef = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // Efecto para auto-scrollear a la fila activa (EN_PROCESO o PENDIENTE)
  useEffect(() => {
    if (autoScroll && containerRef.current) {
      // Buscar primero una fila en proceso, si no hay, la primera pendiente
      let target = containerRef.current.querySelector('.row-EN_PROCESO');
      if (!target) {
        target = containerRef.current.querySelector('.row-PENDIENTE');
      }

      if (target) {
        // Scrollear para que la fila quede en el medio del contenedor
        const container = containerRef.current;
        container.scrollTo({
          top: target.offsetTop - container.clientHeight / 2 + target.clientHeight / 2,
          behavior: 'smooth'
        });
      }
    }
  }, [solicitudes, autoScroll]);

  const handleScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;
    // Si el usuario scrollea hacia arriba, desactivamos el auto-scroll
    // Si llega casi al fondo, lo reactivamos. 
    // Como las activas pueden estar en cualquier lado, reactivamos si no hay scroll manual reciente.
    // Para simplificar, asumiremos que si scrollea desactivamos temporalmente.
    // Pero en este caso es mejor dejar que el botón lo reactive si lo hacemos.
  };

  const handleOrdenar = (campo) => {
    if (ordenCampo === campo) {
      setOrdenAsc(!ordenAsc);
    } else {
      setOrdenCampo(campo);
      setOrdenAsc(true);
    }
  };

  const solicitudesOrdenadas = [...solicitudes].sort((a, b) => {
    const valA = a[ordenCampo] ?? '';
    const valB = b[ordenCampo] ?? '';
    const cmp = valA < valB ? -1 : valA > valB ? 1 : 0;
    return ordenAsc ? cmp : -cmp;
  });

  const getEstadoBadge = (estado) => {
    const badges = {
      'PENDIENTE':  { cls: 'status-badge pendiente',  label: '⏳ Pendiente'  },
      'EN_PROCESO': { cls: 'status-badge en-proceso', label: '🔄 En Proceso' },
      'ACEPTADA':   { cls: 'status-badge aprobada',   label: '✅ Aprobada'   },
      'RECHAZADA':  { cls: 'status-badge rechazada',  label: '❌ Agotada'  },
    };
    return badges[estado] || { cls: 'status-badge pendiente', label: estado };
  };

  const ColHeader = ({ campo, label }) => (
    <th
      className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase
                 tracking-wider cursor-pointer hover:text-medical-400 transition-colors"
      onClick={() => handleOrdenar(campo)}
    >
      <div className="flex items-center gap-1">
        {label}
        {ordenCampo === campo
          ? (ordenAsc ? <ChevronUp size={12} /> : <ChevronDown size={12} />)
          : <span className="w-3" />
        }
      </div>
    </th>
  );

  return (
    <div className="card-glass p-6">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-medical-500/20 flex items-center justify-center">
            <ClipboardList size={16} className="text-medical-400" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Registro de Tickets</h2>
            <p className="text-xs text-slate-400">
              {solicitudes.length} solicitudes registradas
            </p>
          </div>
        </div>

        {/* Leyenda de estados */}
        <div className="hidden md:flex gap-3">
          {['ACEPTADA', 'RECHAZADA', 'EN_PROCESO', 'PENDIENTE'].map(est => {
            const b = getEstadoBadge(est);
            return <span key={est} className={b.cls}>{b.label}</span>;
          })}
        </div>
      </div>

      <div 
        ref={containerRef}
        className="overflow-x-auto overflow-y-auto max-h-64 rounded-xl border border-slate-700/30 relative"
      >
        <table className="w-full text-sm relative">
          <thead>
            <tr className="bg-dark-700/70 border-b border-slate-700/50">
              <ColHeader campo="clienteId"    label="ID Cliente" />
              <ColHeader campo="clienteNombre" label="Nombre" />
              <ColHeader campo="estado"      label="Estado" />
              <ColHeader campo="tiempoProcesamiento" label="Tiempo (ms)" />
              <th className="px-4 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Mensaje
              </th>
            </tr>
          </thead>
          <tbody>
            {solicitudesOrdenadas.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center py-12 text-slate-500">
                  <div className="flex flex-col items-center gap-3">
                    <ClipboardList size={32} className="text-slate-600" />
                    <span>Sin solicitudes. Inicia la simulación para ver registros.</span>
                  </div>
                </td>
              </tr>
            ) : (
              solicitudesOrdenadas.map((sol, idx) => {
                const badge = getEstadoBadge(sol.estado);
                const esNueva = sol.estado === 'PENDIENTE' || sol.estado === 'EN_PROCESO';

                return (
                  <tr
                    key={sol.id}
                    className={`border-b border-slate-700/20 table-row-hover
                               ${esNueva ? 'row-new' : ''} row-${sol.estado}`}
                  >
                    <td className="px-4 py-3 font-mono text-xs text-slate-400">
                      {sol.clienteId}
                    </td>
                    <td className="px-4 py-3 font-medium text-white">
                      {sol.clienteNombre}
                    </td>
                    <td className="px-4 py-3">
                      <span className={badge.cls}>{badge.label}</span>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs">
                      {sol.tiempoProcesamiento > 0 ? (
                        <span className="text-medical-400">{sol.tiempoProcesamiento}ms</span>
                      ) : (
                        <span className="text-slate-600">—</span>
                      )}
                    </td>
                    <td className="px-4 py-2 text-xs text-slate-400 max-w-xs truncate">
                      {sol.mensaje || <span className="text-slate-600 italic">Esperando...</span>}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TablaSolicitudes;
