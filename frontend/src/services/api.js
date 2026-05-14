/**
 * ============================================================
 * SERVICIO API - Comunicación con el backend Spring Boot
 * ============================================================
 * Centraliza todas las llamadas HTTP al backend.
 * Usa fetch nativo (sin axios) para simplicidad.
 */

const BASE_URL = 'http://localhost:8080/api';

/**
 * Obtiene el estado actual del sistema.
 * Llamado cada 1 segundo por el hook useSimulacion (polling).
 */
export const getEstado = async () => {
  const response = await fetch(`${BASE_URL}/estado`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
};

/**
 * Inicia la simulación concurrente.
 * El backend lanza los threads productores y consumidor.
 */
export const iniciarSimulacion = async (config = null) => {
  const options = {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  };
  if (config) {
    options.body = JSON.stringify(config);
  }
  const response = await fetch(`${BASE_URL}/iniciar`, options);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
};

/**
 * Resetea el sistema: detiene threads, limpia estado.
 */
export const resetearSimulacion = async () => {
  const response = await fetch(`${BASE_URL}/reset`, { method: 'POST' });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
};

/**
 * Verifica que el backend esté vivo.
 */
export const checkHealth = async () => {
  const response = await fetch(`${BASE_URL}/health`);
  return response.ok;
};
