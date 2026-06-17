import { Alert, Box, Chip, Stack, Typography } from '@mui/material';
import { terminalMissionStatuses } from '../../shared/constants';
import { Panel } from '../../shared/components/Panel';
import { fmtNumber } from '../../shared/formatters';
import { missionStatusHint, riskDecisionText, statusColor, statusText } from '../../shared/status';

function MissionCardRow({ label, children }) {
  return (
    <Stack className="mission-card-row" direction="row">
      <Typography color="text.secondary">{label}</Typography>
      <Box className="mission-card-value" fontWeight={700}>{children}</Box>
    </Stack>
  );
}

export function MissionCard({ mission, compact, embedded = false }) {
  if (!mission) {
    return embedded
      ? <Typography color="text.secondary">Нет выбранного рейса</Typography>
      : <Panel title="Рейс"><Typography color="text.secondary">Нет выбранного рейса</Typography></Panel>;
  }
  const terminal = terminalMissionStatuses.has(mission.status);
  const content = (
      <Stack className={`mission-card-body ${compact ? 'mission-card-compact' : ''}`} spacing={compact ? 1 : 1.5}>
        <MissionCardRow label="Статус"><Chip size="small" color={statusColor(mission.status)} label={statusText(mission.status)} /></MissionCardRow>
        <MissionCardRow label="Состояние">{missionStatusHint(mission)}</MissionCardRow>
        <MissionCardRow label="Зона">{mission.zoneName || '—'}</MissionCardRow>
        <MissionCardRow label="Харвестер">{mission.harvesterName || '—'}</MissionCardRow>
        <MissionCardRow label="Экипаж">{mission.crewName || '—'}</MissionCardRow>
        <MissionCardRow label="P(attack)">{mission.risk ? `${fmtNumber(mission.risk.pAttack * 100, 1)}%` : '—'}</MissionCardRow>
        <MissionCardRow label="Risk-score">{mission.risk?.riskScore ?? '—'}{mission.risk ? ` · ${riskDecisionText(mission.risk.decisionZone)}` : ''}</MissionCardRow>
        <MissionCardRow label="Приоритет мониторинга">{mission.monitoringPriority || 0}</MissionCardRow>
        {mission.closedBy && <MissionCardRow label="Закрыл">{mission.closedBy}</MissionCardRow>}
        {mission.draftMissingFields && <Alert severity="warning">Не хватает данных: {mission.draftMissingFields}</Alert>}
        {mission.riskReviewRequiredAt && !terminal && <Alert severity="warning">Риск требует пересчёта: {mission.riskReviewReason || 'изменились факторы'}</Alert>}
        {mission.monitoringContext && <Alert severity={mission.monitoringPriority >= 90 ? 'error' : 'warning'}>{mission.monitoringContext}</Alert>}
        {mission.risk?.stale && !terminal && <Alert severity="warning">Оценка риска устарела</Alert>}
      </Stack>
  );
  if (embedded) {
    return (
      <Box className="mission-card-embedded">
        <Typography variant="h3" gutterBottom>HRV-{mission.id}</Typography>
        {content}
      </Box>
    );
  }
  return (
    <Panel title={`HRV-${mission.id}`}>
      {content}
    </Panel>
  );
}
