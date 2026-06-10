import { terminalIncidentStatuses, terminalMissionStatuses } from './constants';
import { timeLeft } from './formatters';

const successfulStatuses = new Set(['ACTIVE', 'DELIVERED', 'RISK_ASSESSED', 'RECALCULATED', 'TERMS_UPDATED']);
const failedStatuses = new Set(['RISK_CANCELLED', 'LOST', 'EXPIRED', 'DELIVERY_FAILED', 'REJECTED']);
const warningStatuses = new Set(['OPEN', 'CLASSIFIED', 'EVACUATION_ORDERED', 'READY_FOR_RECALCULATION', 'WAITING_FOR_DATA', 'COMPLETED_PENDING_CLOSE', 'CREATED', 'SENT']);

export function historyEventText(value) {
  return {
    RECALCULATION: 'Перерасчёт',
    TERMS_UPDATED: 'Изменение условий'
  }[value] || value || '—';
}

export function objectLabel(type, id) {
  return {
    mission: `HRV-${id}`,
    incident: `INC-${id}`,
    insurance_case: `CLM-${id}`
  }[type] || `${type || 'object'}-${id}`;
}

export function statusText(value) {
  return {
    DRAFT: 'Черновик',
    READY_FOR_RISK: 'Готов к риску',
    RISK_ASSESSED: 'Риск рассчитан',
    ACTIVE: 'Активен',
    RISK_CANCELLED: 'Риск-отмена',
    CANCELLED: 'Отменён',
    COMPLETED_PENDING_CLOSE: 'Ожидает закрытия',
    LOST: 'Потерян',
    CLOSED: 'Закрыт',
    OPEN: 'Новый',
    CLASSIFIED: 'Классифицирован',
    MONITORING: 'Мониторинг',
    EVACUATION_ORDERED: 'Эвакуация',
    EVACUATION_ACKNOWLEDGED: 'Подтверждена',
    WAITING_FOR_DATA: 'Ожидает данные',
    READY_FOR_RECALCULATION: 'К перерасчёту',
    RECALCULATED: 'Пересчитан',
    TERMS_UPDATED: 'Условия обновлены',
    REJECTED: 'Отклонён',
    READY: 'Готов',
    BUSY: 'Занят',
    MAINTENANCE: 'Обслуживание',
    CREATED: 'Создана',
    SENT: 'Отправлена',
    DELIVERED: 'Доставлена',
    ACKNOWLEDGED: 'Подтверждена',
    DELIVERY_FAILED: 'Ошибка доставки',
    EXPIRED: 'Просрочена',
    RISK_CANCELLATION: 'Риск-отмена',
    INCIDENT: 'Инцидент',
    SLA_BREACH: 'Нарушение НВР',
    MISSION_LOSS: 'Потеря рейса',
    MISSION_CLOSE: 'Закрытие рейса'
  }[value] || value || '—';
}

export function severityColor(value) {
  return {
    LOW: 'success',
    MEDIUM: 'info',
    HIGH: 'warning',
    CRITICAL: 'error'
  }[value] || 'default';
}

export function severityText(value) {
  return {
    LOW: 'Низкая',
    MEDIUM: 'Средняя',
    HIGH: 'Высокая',
    CRITICAL: 'Критическая'
  }[value] || value || '—';
}

export function riskDecisionText(value) {
  return {
    ALLOWED: 'разрешён',
    WARNING: 'требует подтверждения',
    BLOCKING: 'блокирует запуск'
  }[value] || value || '—';
}

export function missionStatusHint(mission) {
  if (!mission) return 'Нет выбранного рейса';
  if (mission.draftMissingFields) return 'Черновик неполный';
  if (mission.riskReviewRequiredAt && !terminalMissionStatuses.has(mission.status)) return 'Нужен новый risk snapshot';
  if (mission.risk?.stale && !terminalMissionStatuses.has(mission.status)) return 'Risk snapshot устарел';
  if (mission.status === 'RISK_ASSESSED') return `Риск ${mission.risk?.riskScore ?? '—'}/100, можно запускать по решению`;
  if (mission.status === 'ACTIVE' && !mission.plan?.acknowledgedAt) return 'План опубликован, экипаж не подтвердил';
  if (mission.status === 'ACTIVE') return 'Рейс выполняется';
  if (mission.status === 'COMPLETED_PENDING_CLOSE') return 'Ожидает итоговый отчёт и закрытие';
  if (terminalMissionStatuses.has(mission.status)) return 'Финальный статус';
  return 'Ожидает следующего действия';
}

export function slaState(incident, now = Date.now()) {
  if (!incident) return { label: '—', tone: 'primary' };
  if (incident.slaBreached) return { label: 'Нарушен', tone: 'error' };
  if (terminalIncidentStatuses.has(incident.status)) return { label: 'Соблюдён', tone: 'success' };
  if (!incident.slaDeadlineAt) return { label: '—', tone: 'primary' };
  if (now >= new Date(incident.slaDeadlineAt).getTime()) return { label: 'Нарушен', tone: 'error' };
  const secondsLeft = Math.floor((new Date(incident.slaDeadlineAt).getTime() - now) / 1000);
  return {
    label: timeLeft(incident.slaDeadlineAt, now),
    tone: secondsLeft <= 60 ? 'warning' : 'success'
  };
}

export function statusColor(value) {
  if (successfulStatuses.has(value)) return 'success';
  if (failedStatuses.has(value)) return 'error';
  if (warningStatuses.has(value)) return 'warning';
  return 'default';
}
