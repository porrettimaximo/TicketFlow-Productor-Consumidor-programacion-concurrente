import React from 'react';
import { Ticket, UserCheck, UserX, Calendar, Users } from 'lucide-react';

const PanelTickets = ({ estado }) => {
  const ticketsTotales    = estado?.ticketsTotales    ?? 0;
  const ticketsDisponibles = estado?.ticketsDisponibles ?? 0;
  const ticketsOcupados   = ticketsTotales - ticketsDisponibles;
  const porcentaje = ticketsTotales > 0
    ? Math.round((ticketsOcupados / ticketsTotales) * 100)
    : 0;

  // Color según ocupación
  const getColor = () => {
    if (porcentaje >= 90) return { stroke: '#ef4444', text: 'text-red-400', label: 'COMPLETO' };
    if (porcentaje >= 70) return { stroke: '#f59e0b', text: 'text-amber-400', label: 'CASI LLENO' };
    return { stroke: '#a855f7', text: 'text-purple-400', label: 'DISPONIBLE' };
  };

  const color = getColor();

  // SVG círculo de progreso
  const radio = 45;
  const circunferencia = 2 * Math.PI * radio;
  const offset = circunferencia - (porcentaje / 100) * circunferencia;

  return (
    <div className="card-glass p-6 h-[320px] flex flex-col justify-center">
      <div className="flex items-center gap-3 mb-5">
        <div className="w-8 h-8 rounded-lg bg-fuchsia-500/20 flex items-center justify-center">
          <Ticket size={16} className="text-fuchsia-400" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-white">Tickets del Evento</h2>
          <p className="text-xs text-slate-400">Estado de capacidad</p>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row items-center gap-6">
        {/* Círculo de progreso */}
        <div className="flex flex-col items-center flex-shrink-0">
          <div className="relative">
            <svg width="120" height="120" viewBox="0 0 100 100">
              {/* Círculo de fondo */}
              <circle
                cx="50" cy="50" r={radio}
                fill="none"
                stroke="rgba(168,85,247,0.1)"
                strokeWidth="8"
              />
              {/* Círculo de progreso */}
              <circle
                cx="50" cy="50" r={radio}
                fill="none"
                stroke={color.stroke}
                strokeWidth="8"
                strokeLinecap="round"
                strokeDasharray={circunferencia}
                strokeDashoffset={offset}
                transform="rotate(-90 50 50)"
                style={{
                  transition: 'stroke-dashoffset 0.8s ease-out',
                  filter: `drop-shadow(0 0 6px ${color.stroke})`
                }}
              />
              {/* Texto central */}
              <text x="50" y="45" textAnchor="middle"
                    fill="white" fontSize="18" fontWeight="bold" fontFamily="Inter">
                {ticketsDisponibles}
              </text>
              <text x="50" y="60" textAnchor="middle"
                    fill="#94a3b8" fontSize="8" fontFamily="Inter">
                disponibles
              </text>
            </svg>
          </div>

          <div className={`text-sm font-bold mt-2 ${color.text}`}>
            {color.label}
          </div>
          <div className="text-xs text-slate-400 mt-1 text-center">
            {porcentaje}% ocupados
          </div>
        </div>

        {/* Detalle de tickets y solicitudes */}
        <div className="flex-1 w-full grid grid-cols-2 gap-3">
          <div className="flex flex-col p-3 bg-purple-500/10 rounded-lg border border-purple-500/20">
            <div className="flex items-center gap-2 mb-1">
              <Users size={14} className="text-purple-400" />
              <span className="text-xs text-purple-400 font-medium">Total Solicitudes</span>
            </div>
            <span className="text-xl font-bold text-purple-400">{estado?.totalSolicitudes ?? 0}</span>
          </div>

          <div className="flex flex-col p-3 bg-green-500/10 rounded-lg border border-green-500/20">
            <div className="flex items-center gap-2 mb-1">
              <UserCheck size={14} className="text-green-400" />
              <span className="text-xs text-green-400 font-medium">Aprobadas</span>
            </div>
            <span className="text-xl font-bold text-green-400">{estado?.solicitudesAprobadas ?? 0}</span>
          </div>

          <div className="flex flex-col p-3 bg-red-500/10 rounded-lg border border-red-500/20">
            <div className="flex items-center gap-2 mb-1">
              <UserX size={14} className="text-red-400" />
              <span className="text-xs text-red-400 font-medium">Agotadas</span>
            </div>
            <span className="text-xl font-bold text-red-400">{estado?.solicitudesAgotadas ?? 0}</span>
          </div>

          <div className="flex flex-col p-3 bg-fuchsia-500/10 rounded-lg border border-fuchsia-500/20">
            <div className="flex items-center gap-2 mb-1">
              <Calendar size={14} className="text-fuchsia-400" />
              <span className="text-xs text-fuchsia-400 font-medium">Capacidad</span>
            </div>
            <span className="text-xl font-bold text-fuchsia-400">{ticketsTotales}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PanelTickets;
