import { criticalRealtimeActions, transientHttpStatuses } from './constants';

export function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

export function isTransientApiError(error) {
  return transientHttpStatuses.has(error?.status);
}

export function realtimeText(action) {
  return {
    MISSION_CREATED: 'Создан рейс',
    MISSION_UPDATED: 'Рейс обновлён',
    MISSION_LAUNCHED: 'Рейс запущен',
    MISSION_RISK_CANCELLED: 'Рейс отменён по риску',
    MISSION_PLAN_ACKNOWLEDGED: 'План рейса подтверждён экипажем',
    RISK_ASSESSED: 'Риск рассчитан',
    RISK_MARKED_STALE: 'Риск требует пересчёта',
    RISK_RECALCULATED_AFTER_CHANGE: 'Риск пересчитан после изменения условий',
    RISK_POLICY_UPDATED: 'Политика риска обновлена',
    TELEMETRY_RECEIVED: 'Поступила телеметрия',
    MISSION_MONITORING_PRIORITY_RAISED: 'Повышен приоритет мониторинга',
    ALARM_RECEIVED: 'Поступила тревога',
    INCIDENT_CREATED: 'Создан инцидент',
    INCIDENT_CLASSIFIED: 'Инцидент классифицирован',
    INCIDENT_SLA_BREACHED: 'НВР нарушен',
    EVACUATION_COMMAND_CREATED: 'Команда эвакуации создана',
    EVACUATION_COMMAND_SENT: 'Команда эвакуации отправлена',
    EVACUATION_DELIVERED: 'Команда доставлена экипажу',
    EVACUATION_DELIVERY_FAILED: 'Сбой доставки эвакуации',
    EVACUATION_ACKNOWLEDGED: 'Эвакуация подтверждена',
    EVACUATION_ACK_EXPIRED: 'Подтверждение эвакуации просрочено',
    INCIDENT_CLOSED: 'Инцидент закрыт',
    MISSION_REPORT_SUBMITTED: 'Итоговый отчёт принят',
    MISSION_CLOSED: 'Рейс закрыт',
    INSURANCE_CASE_OPENED: 'Страховой кейс открыт',
    INSURANCE_CASE_WAITING_FOR_DATA: 'Кейс ожидает данные',
    INSURANCE_RECALCULATED: 'Страховой кейс пересчитан',
    INSURANCE_TERMS_UPDATED: 'Условия страхования обновлены',
    INSURANCE_RECALCULATION_REJECTED: 'Перерасчёт страхования отклонён',
    INSURANCE_CASE_CLOSED: 'Страховой кейс закрыт',
    USER_CREATED: 'Создан пользователь',
    USER_ROLES_UPDATED: 'Роли пользователя обновлены',
    HARVESTER_CREATED: 'Создан харвестер',
    CREW_CREATED: 'Создан экипаж'
  }[action] || 'Событие HSMS';
}

export function realtimeSeverity(action) {
  return criticalRealtimeActions.has(action) ? 'error' : 'info';
}
