import { Alert, Box, Chip, Grid, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { CellTower, GppMaybe, History, Rule } from '@mui/icons-material';
import { Kpi } from '../../shared/components/Kpi';
import { detailsText, fmtDate, fmtNumber } from '../../shared/formatters';
import { realtimeText } from '../../shared/realtime';
import { riskDecisionText, severityColor, severityText, slaState, statusColor, statusText } from '../../shared/status';
import { useNow } from '../../hooks/useNow';

const securityDecisionActions = new Set([
  'INCIDENT_CLASSIFIED',
  'INCIDENT_SLA_BREACHED',
  'EVACUATION_COMMAND_CREATED',
  'EVACUATION_COMMAND_SENT',
  'EVACUATION_DELIVERY_FAILED',
  'EVACUATION_COMMAND_CANCELLED',
  'EVACUATION_ACK_EXPIRED',
  'EVACUATION_DELIVERED',
  'EVACUATION_ACKNOWLEDGED',
  'INCIDENT_CLOSED'
]);

function latestAcceptedTelemetry(telemetry) {
  return telemetry.find((event) => event.freshnessStatus === 'ACCEPTED') || telemetry[0] || null;
}

function coordinateText(telemetry) {
  if (!telemetry) return '—';
  return `${fmtNumber(telemetry.lat, 3)}, ${fmtNumber(telemetry.lon, 3)}`;
}

function EmptyRow({ colSpan, text }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan}>
        <Typography color="text.secondary">{text}</Typography>
      </TableCell>
    </TableRow>
  );
}

function TelemetrySummary({ telemetry }) {
  if (!telemetry) {
    return <Alert severity="warning">Телеметрия по рейсу ещё не поступала</Alert>;
  }

  return (
    <Stack className="mission-context-card" spacing={1}>
      <Typography variant="h3">Последняя телеметрия</Typography>
      <Stack className="mission-context-row" direction="row">
        <Typography color="text.secondary">Координаты</Typography>
        <Typography fontWeight={700}>{coordinateText(telemetry)}</Typography>
      </Stack>
      <Stack className="mission-context-row" direction="row">
        <Typography color="text.secondary">Оборудование</Typography>
        <Typography fontWeight={700}>{telemetry.equipmentStatus || '—'}</Typography>
      </Stack>
      <Stack className="mission-context-row" direction="row">
        <Typography color="text.secondary">Статус данных</Typography>
        <Chip size="small" color={telemetry.freshnessStatus === 'ACCEPTED' ? 'success' : 'warning'} label={statusText(telemetry.freshnessStatus)} />
      </Stack>
      <Stack className="mission-context-row" direction="row">
        <Typography color="text.secondary">Время события</Typography>
        <Typography fontWeight={700}>{fmtDate(telemetry.eventTime)}</Typography>
      </Stack>
      <Stack className="mission-context-row" direction="row">
        <Typography color="text.secondary">Получено</Typography>
        <Typography fontWeight={700}>{fmtDate(telemetry.receivedAt)}</Typography>
      </Stack>
    </Stack>
  );
}

function IncidentsTable({ incidents, focusIncidentId, now }) {
  return (
    <Box className="mission-context-table">
      <Typography variant="h3" gutterBottom>Связанные инциденты</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Критичность</TableCell>
            <TableCell>НВР</TableCell>
            <TableCell>Эвакуация</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {incidents.length === 0 && <EmptyRow colSpan={5} text="Связанных инцидентов нет" />}
          {incidents.map((incident) => {
            const incidentSla = slaState(incident, now);
            return (
              <TableRow key={incident.id} selected={incident.id === focusIncidentId}>
                <TableCell>INC-{incident.id}</TableCell>
                <TableCell><Chip size="small" color={statusColor(incident.status)} label={statusText(incident.status)} /></TableCell>
                <TableCell><Chip size="small" color={severityColor(incident.severity)} label={severityText(incident.severity)} /></TableCell>
                <TableCell><Chip size="small" color={incidentSla.tone === 'primary' ? 'default' : incidentSla.tone} label={incidentSla.label} /></TableCell>
                <TableCell>{incident.evacuationCommand ? statusText(incident.evacuationCommand.status) : '—'}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </Box>
  );
}

function RiskHistoryTable({ riskHistory }) {
  return (
    <Box className="mission-context-table">
      <Typography variant="h3" gutterBottom>История риска</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Время</TableCell>
            <TableCell>Risk-score</TableCell>
            <TableCell>P(attack)</TableCell>
            <TableCell>Решение</TableCell>
            <TableCell>Качество</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {riskHistory.length === 0 && <EmptyRow colSpan={5} text="Расчётов риска нет" />}
          {riskHistory.slice(0, 6).map((risk) => (
            <TableRow key={risk.id}>
              <TableCell>{fmtDate(risk.calculatedAt)}</TableCell>
              <TableCell>
                <Stack spacing={0.25}>
                  <Typography fontWeight={700}>{risk.riskScore}/100</Typography>
                  {risk.stale && <Chip size="small" color="warning" label="Устарел" />}
                </Stack>
              </TableCell>
              <TableCell>{fmtNumber(risk.pAttack * 100, 1)}%</TableCell>
              <TableCell>{riskDecisionText(risk.decisionZone)}</TableCell>
              <TableCell>{statusText(risk.dataQuality)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

function DecisionsTable({ decisions }) {
  return (
    <Box className="mission-context-table">
      <Typography variant="h3" gutterBottom>Решения штаба</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Время</TableCell>
            <TableCell>Событие</TableCell>
            <TableCell>Оператор</TableCell>
            <TableCell>Детали</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {decisions.length === 0 && <EmptyRow colSpan={4} text="Решений штаба по рейсу нет" />}
          {decisions.slice(0, 8).map((event) => (
            <TableRow key={event.id}>
              <TableCell>{fmtDate(event.createdAt)}</TableCell>
              <TableCell>{realtimeText(event.action)}</TableCell>
              <TableCell>{event.actorLogin || '—'}</TableCell>
              <TableCell className="mission-context-details-cell">{detailsText(event.details)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

export function MissionOperationalContext({ error, focusIncidentId, loading, mission, timeline }) {
  const now = useNow();
  const effectiveMission = timeline?.mission || mission;
  const telemetry = timeline?.telemetry || [];
  const incidents = timeline?.incidents || [];
  const riskHistory = timeline?.riskHistory || (effectiveMission?.risk ? [effectiveMission.risk] : []);
  const decisions = (timeline?.audit || []).filter((event) => securityDecisionActions.has(event.action));
  const latestTelemetry = latestAcceptedTelemetry(telemetry);

  if (!effectiveMission) {
    return <Typography color="text.secondary">Нет выбранного рейса</Typography>;
  }

  return (
    <Stack className="mission-context" spacing={2}>
      {loading && !timeline && <Alert severity="info">Контекст рейса загружается</Alert>}
      {error && <Alert severity="warning">Контекст рейса недоступен: {error.message}</Alert>}
      <Grid container spacing={1.5}>
        <Kpi title="Телеметрия" value={latestTelemetry ? fmtDate(latestTelemetry.eventTime) : '—'} icon={<CellTower />} tone={latestTelemetry ? 'success' : 'warning'} />
        <Kpi title="Инциденты" value={incidents.length} icon={<GppMaybe />} tone={incidents.length ? 'warning' : 'success'} />
        <Kpi title="Решения штаба" value={decisions.length} icon={<Rule />} tone={decisions.length ? 'warning' : 'success'} />
        <Kpi title="Расчёты риска" value={riskHistory.length} icon={<History />} />
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 4 }}>
          <TelemetrySummary telemetry={latestTelemetry} />
        </Grid>
        <Grid size={{ xs: 12, lg: 8 }}>
          <IncidentsTable incidents={incidents} focusIncidentId={focusIncidentId} now={now} />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <RiskHistoryTable riskHistory={riskHistory} />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <DecisionsTable decisions={decisions} />
        </Grid>
      </Grid>
    </Stack>
  );
}
