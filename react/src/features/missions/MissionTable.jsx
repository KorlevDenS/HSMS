import { Chip, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { terminalMissionStatuses } from '../../shared/constants';
import { fmtDate } from '../../shared/formatters';
import { missionStatusHint, riskDecisionText, statusColor, statusText } from '../../shared/status';

export function MissionTable({ missions, selectedMission, onSelect }) {
  return (
    <Table size="small">
      <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Зона</TableCell>
            <TableCell>Окно</TableCell>
            <TableCell>Экипаж</TableCell>
            <TableCell>Мониторинг</TableCell>
            <TableCell>Риск</TableCell>
          </TableRow>
      </TableHead>
      <TableBody>
        {missions.map((mission) => (
          <TableRow key={mission.id} hover selected={mission.id === selectedMission?.id} onClick={() => onSelect(mission.id)}>
            <TableCell>HRV-{mission.id}</TableCell>
            <TableCell><MissionStatusCell mission={mission} /></TableCell>
            <TableCell>{mission.zoneName || '—'}</TableCell>
            <TableCell>{fmtDate(mission.plannedStart)} – {fmtDate(mission.plannedEnd)}</TableCell>
            <TableCell>{mission.crewName || '—'}</TableCell>
            <TableCell>{mission.monitoringPriority ? mission.monitoringPriority : '—'}</TableCell>
            <TableCell><MissionRiskCell mission={mission} /></TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function MissionStatusCell({ mission }) {
  return (
    <Stack spacing={0.25}>
      <Chip size="small" color={statusColor(mission.status)} label={statusText(mission.status)} />
      <Typography variant="caption" color="text.secondary">{missionStatusHint(mission)}</Typography>
    </Stack>
  );
}

function MissionRiskCell({ mission }) {
  if (mission.riskReviewRequiredAt && !terminalMissionStatuses.has(mission.status)) {
    return (
      <Stack spacing={0.25}>
        <Chip size="small" color="warning" label="Пересчитать" />
        <Typography variant="caption" color="text.secondary">
          {mission.risk ? `Последний: ${mission.risk.riskScore}/100` : mission.riskReviewReason || 'Нет актуального snapshot'}
        </Typography>
      </Stack>
    );
  }
  if (!mission.risk) return '—';
  return (
    <Stack spacing={0.25}>
      <Typography fontWeight={700}>{mission.risk.riskScore}/100</Typography>
      <Typography variant="caption" color="text.secondary">{riskDecisionText(mission.risk.decisionZone)}</Typography>
    </Stack>
  );
}
