import { SESSION_KEY, TELEMETRY_QUEUE_KEY } from './constants';

function readJson(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key)) || fallback;
  } catch {
    return fallback;
  }
}

export function loadSession() {
  return readJson(SESSION_KEY, null);
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function loadTelemetryQueue() {
  return readJson(TELEMETRY_QUEUE_KEY, []);
}

export function saveTelemetryQueue(queue) {
  localStorage.setItem(TELEMETRY_QUEUE_KEY, JSON.stringify(queue));
}
