import { useState } from 'react';
import { Alert, Box, Button, Chip, FormControl, Grid, InputLabel, MenuItem, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField } from '@mui/material';
import { EmergencyRecording, GppMaybe, NotificationsActive, Route, Tune } from '@mui/icons-material';
import { terminalEvacuationStatuses } from '../../shared/constants';
import { Kpi } from '../../shared/components/Kpi';
import { Panel } from '../../shared/components/Panel';
import { useNow } from '../../hooks/useNow';
import { severityColor, severityText, slaState, statusColor, statusText } from '../../shared/status';
import { MissionCard } from '../missions/MissionCard';

export function SecurityView({ run, ask, incidents, missions, selectedIncident, setSelectedIncidentId, api }) {
  const now = useNow();
  const [classification, setClassification] = useState({
    severity: 'HIGH',
    reason: 'Сигнатура угрозы подтверждена по телеметрии'
  });
  const [evacuationReason, setEvacuationReason] = useState('Решение штаба безопасности');
  const [deliveryFailureReason, setDeliveryFailureReason] = useState('Канал связи не подтвердил доставку команды');
  const incidentMission = missions.find((mission) => mission.id === selectedIncident?.missionId);
  const selectedSla = slaState(selectedIncident, now);
  const selectedIncidentClosed = !selectedIncident || selectedIncident.status === 'CLOSED';
  const selectedEvacuationStatus = selectedIncident?.evacuationCommand?.status;
  const selectedEvacuationTerminal = terminalEvacuationStatuses.has(selectedEvacuationStatus);

  function updateClassification(field, value) {
    setClassification((current) => ({ ...current, [field]: value }));
  }

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12 }}>
        <Panel title="Решение штаба">
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="incident-severity-label" htmlFor="incident-severity-native">Критичность</InputLabel>
              <Select id="incident-severity" labelId="incident-severity-label" name="severity" label="Критичность" value={classification.severity} onChange={(event) => updateClassification('severity', event.target.value)} inputProps={{ id: 'incident-severity-native', 'aria-label': 'Критичность' }}>
                <MenuItem value="LOW">Низкая</MenuItem>
                <MenuItem value="MEDIUM">Средняя</MenuItem>
                <MenuItem value="HIGH">Высокая</MenuItem>
                <MenuItem value="CRITICAL">Критическая</MenuItem>
              </Select>
            </FormControl>
            <TextField id="classification-reason" name="classificationReason" sx={{ flex: 1 }} label="Причина классификации" value={classification.reason} onChange={(event) => updateClassification('reason', event.target.value)} />
            <Button startIcon={<Tune />} disabled={selectedIncidentClosed} onClick={() => run('Инцидент классифицирован', () => api(`/incidents/${selectedIncident.id}/classification`, { method: 'PATCH', body: JSON.stringify(classification) }))}>Классифицировать</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mt: 2 }}>
            <TextField id="evacuation-reason" name="evacuationReason" sx={{ flex: 1 }} label="Основание эвакуации" value={evacuationReason} onChange={(event) => setEvacuationReason(event.target.value)} />
          <Button color="error" startIcon={<EmergencyRecording />} disabled={selectedIncidentClosed} onClick={() => ask(
            'Команда эвакуации',
            `Команда будет отправлена экипажу рейса HRV-${selectedIncident.missionId}; таймер подтверждения начнёт отсчёт сразу.`,
            () => run('Команда эвакуации отправлена', () => api(`/incidents/${selectedIncident.id}/evacuation`, { method: 'POST', body: JSON.stringify({ reason: evacuationReason }) })),
            true
          )}>Эвакуация</Button>
          <Button disabled={selectedIncidentClosed} onClick={() => run('Инцидент закрыт', () => api(`/incidents/${selectedIncident.id}/close`, { method: 'POST', body: '{}' }))}>Закрыть</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mt: 2 }}>
            <TextField id="evacuation-delivery-failure-reason" name="deliveryFailureReason" sx={{ flex: 1 }} label="Причина сбоя доставки" value={deliveryFailureReason} onChange={(event) => setDeliveryFailureReason(event.target.value)} />
            <Button color="warning" disabled={selectedIncidentClosed || !selectedIncident?.evacuationCommand || selectedEvacuationTerminal} onClick={() => run('Сбой доставки зафиксирован', () => api(`/incidents/${selectedIncident.id}/evacuation/delivery-failed`, { method: 'POST', body: JSON.stringify({ reason: deliveryFailureReason }) }))}>Сбой доставки</Button>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Контекст инцидента" className="security-context-panel">
          <Grid container spacing={1.5}>
            <Kpi title="Рейс" value={selectedIncident ? `HRV-${selectedIncident.missionId}` : '—'} icon={<Route />} />
            <Kpi title="Статус НВР" value={selectedSla.label} icon={<EmergencyRecording />} tone={selectedSla.tone} />
            <Kpi title="Критичность" value={severityText(selectedIncident?.severity)} icon={<GppMaybe />} tone={selectedIncident?.severity === 'CRITICAL' ? 'error' : 'warning'} />
            <Kpi title="Команда" value={statusText(selectedIncident?.evacuationCommand?.status)} icon={<NotificationsActive />} />
          </Grid>
          {selectedIncident?.classificationReason && (
            <Alert severity="info" className="security-context-note">
              {selectedIncident.classificationReason}
            </Alert>
          )}
          <Box className="security-context-details">
            {incidentMission && <MissionCard mission={incidentMission} compact embedded />}
          </Box>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Очередь инцидентов">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Рейс</TableCell>
                <TableCell>Критичность</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>НВР</TableCell>
                <TableCell>Эвакуация</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {incidents.map((incident) => {
                const incidentSla = slaState(incident, now);
                return (
                <TableRow key={incident.id} hover selected={incident.id === selectedIncident?.id} onClick={() => setSelectedIncidentId(incident.id)}>
                  <TableCell>INC-{incident.id}</TableCell>
                  <TableCell>HRV-{incident.missionId}</TableCell>
                  <TableCell><Chip size="small" color={severityColor(incident.severity)} label={severityText(incident.severity)} /></TableCell>
                  <TableCell><Chip size="small" color={statusColor(incident.status)} label={statusText(incident.status)} /></TableCell>
                  <TableCell><Chip size="small" color={incidentSla.tone === 'primary' ? 'default' : incidentSla.tone} label={incidentSla.label} /></TableCell>
                  <TableCell>{incident.evacuationCommand ? statusText(incident.evacuationCommand.status) : '—'}</TableCell>
                </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Panel>
      </Grid>
    </Grid>
  );
}
