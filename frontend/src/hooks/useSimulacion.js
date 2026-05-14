/**
 * ============================================================
 * HOOK: useSimulacion
 * ============================================================
 * Hook personalizado que encapsula toda la lógica de estado
 * del dashboard: polling al backend, manejo de errores, etc.
 *
 * POLLING: Llama al backend cada 1 segundo para obtener el
 * estado actualizado (semáforos, buffer, solicitudes, logs).
 * Esto simula "tiempo real" sin necesidad de WebSockets.
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { getEstado, iniciarSimulacion, resetearSimulacion, checkHealth } from '../services/api';

export const useSimulacion = () => {
  // ── Estado del sistema (del backend) ─────────────────────
  const [estado, setEstado] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [backendConectado, setBackendConectado] = useState(false);

  // ── Referencia al intervalo de polling ───────────────────
  const pollingRef = useRef(null);

  /**
   * Obtiene el estado del backend y actualiza el state local.
   * Se llama cada 1 segundo via setInterval.
   */
  const actualizarEstado = useCallback(async () => {
    try {
      const data = await getEstado();
      setEstado(data);
      setBackendConectado(true);
      setError(null);
    } catch (err) {
      setBackendConectado(false);
      setError('No se puede conectar al backend. ¿Está Spring Boot corriendo?');
    }
  }, []);

  /**
   * Inicia el polling: llama al backend cada 1000ms.
   */
  const iniciarPolling = useCallback(() => {
    actualizarEstado(); // Llamada inmediata
    pollingRef.current = setInterval(actualizarEstado, 1000);
  }, [actualizarEstado]);

  /**
   * Detiene el polling (cleanup al desmontar el componente).
   */
  const detenerPolling = useCallback(() => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  }, []);

  // Iniciar polling al montar el componente
  useEffect(() => {
    iniciarPolling();
    return () => detenerPolling(); // Cleanup al desmontar
  }, [iniciarPolling, detenerPolling]);

  /**
   * Acción: Iniciar simulación
   */
  const handleIniciar = useCallback(async (config = null) => {
    setCargando(true);
    try {
      await iniciarSimulacion(config);
    } catch (err) {
      setError('Error al iniciar la simulación. Verifica el backend.');
    } finally {
      setCargando(false);
    }
  }, []);

  /**
   * Acción: Resetear simulación
   */
  const handleReset = useCallback(async () => {
    setCargando(true);
    try {
      await resetearSimulacion();
    } catch (err) {
      setError('Error al resetear. Verifica el backend.');
    } finally {
      setCargando(false);
    }
  }, []);

  return {
    estado,
    cargando,
    error,
    backendConectado,
    handleIniciar,
    handleReset,
  };
};
