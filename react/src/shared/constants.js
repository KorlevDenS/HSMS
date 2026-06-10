export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';
export const TELEMETRY_QUEUE_KEY = 'hsms.telemetry.queue.v1';
export const SESSION_KEY = 'hsms.session.v1';

export const terminalMissionStatuses = new Set(['CLOSED', 'LOST', 'CANCELLED', 'RISK_CANCELLED']);
export const terminalIncidentStatuses = new Set(['CLOSED', 'EVACUATION_ACKNOWLEDGED', 'MONITORING']);
export const terminalEvacuationStatuses = new Set(['ACKNOWLEDGED', 'DELIVERY_FAILED', 'EXPIRED', 'CANCELLED']);
export const transientHttpStatuses = new Set([0, 408, 425, 429, 500, 502, 503, 504]);
export const criticalRealtimeActions = new Set([
  'ALARM_RECEIVED',
  'INCIDENT_CREATED',
  'INCIDENT_SLA_BREACHED',
  'EVACUATION_COMMAND_SENT',
  'EVACUATION_DELIVERY_FAILED',
  'EVACUATION_ACK_EXPIRED',
  'MISSION_RISK_CANCELLED'
]);
