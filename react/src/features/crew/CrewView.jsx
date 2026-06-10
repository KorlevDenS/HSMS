import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, FormControl, Grid, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { CellTower, NotificationsActive, ReceiptLong } from '@mui/icons-material';
import { Panel } from '../../shared/components/Panel';
import { datetimeLocal, fmtDate, isoFromLocal } from '../../shared/formatters';
import { statusText } from '../../shared/status';
import { MissionCard } from '../missions/MissionCard';
import { RouteMap } from '../missions/RouteMap';

export function CrewView({ api, run, missions, selectedMission, setSelectedMissionId, canSwitchMission, incidents, telemetryQueue, enqueueTelemetry }) {
  const firstPoint = selectedMission?.route?.[0];
  const evacuationIncident = useMemo(() => [...(incidents || [])]
    .filter((incident) => incident.missionId === selectedMission?.id && incident.evacuationCommand)
    .sort((a, b) => b.id - a.id)[0], [incidents, selectedMission?.id]);
  const evacuationCommand = evacuationIncident?.evacuationCommand;
  const [telemetryForm, setTelemetryForm] = useState({
    lat: firstPoint?.lat || 24.42,
    lon: firstPoint?.lon || 54.20,
    equipmentStatus: 'NORMAL',
    eventTime: datetimeLocal(new Date().toISOString())
  });
  const [alarmReason, setAlarmReason] = useState('Акустическая и сейсмическая сигнатура угрозы');
  const [reportForm, setReportForm] = useState({
    actualEnd: datetimeLocal(new Date().toISOString()),
    spiceAmount: 0,
    harvesterFinalStatus: 'READY',
    abnormalSituations: 'Нет'
  });

  useEffect(() => {
    if (!firstPoint) return;
    setTelemetryForm((current) => ({
      ...current,
      lat: firstPoint.lat,
      lon: firstPoint.lon
    }));
  }, [firstPoint?.lat, firstPoint?.lon]);

  function updateTelemetry(field, value) {
    setTelemetryForm((current) => ({ ...current, [field]: value }));
  }

  async function submitTelemetry() {
    if (!selectedMission) return null;
    const payload = {
      externalEventId: `tel-${Date.now()}`,
      eventTime: isoFromLocal(telemetryForm.eventTime),
      lat: Number(telemetryForm.lat),
      lon: Number(telemetryForm.lon),
      equipmentStatus: telemetryForm.equipmentStatus
    };
    const result = await run('Телеметрия принята', () => api(`/missions/${selectedMission.id}/telemetry`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }));
    if (!result) {
      enqueueTelemetry(selectedMission.id, payload);
    }
    return result;
  }

  async function sendAlarm() {
    if (!selectedMission) return null;
    return run('Тревога принята штабом', () => api(`/missions/${selectedMission.id}/alarms`, {
      method: 'POST',
      body: JSON.stringify({ externalEventId: `alarm-${Date.now()}`, eventTime: new Date().toISOString(), reason: alarmReason })
    }));
  }

  async function submitReport() {
    if (!selectedMission) return null;
    return run('Итоговый отчёт отправлен', () => api(`/missions/${selectedMission.id}/report`, {
      method: 'POST',
      body: JSON.stringify({
        actualStart: selectedMission.actualStart,
        actualEnd: isoFromLocal(reportForm.actualEnd),
        spiceAmount: Number(reportForm.spiceAmount),
        harvesterFinalStatus: reportForm.harvesterFinalStatus,
        abnormalSituations: reportForm.abnormalSituations
      })
    }));
  }

  async function markDelivered() {
    if (!evacuationIncident) return null;
    return run('Команда доставлена экипажу', () => api(`/incidents/${evacuationIncident.id}/evacuation/delivered`, {
      method: 'POST',
      body: '{}'
    }));
  }

  async function acknowledgeEvacuation() {
    if (!evacuationIncident) return null;
    return run('Эвакуация подтверждена', () => api(`/incidents/${evacuationIncident.id}/evacuation/ack`, {
      method: 'POST',
      body: '{}'
    }));
  }

  return (
    <Grid container spacing={2}>
      {canSwitchMission && (
        <Grid size={{ xs: 12 }}>
          <Panel title="Отображаемый экипаж">
            <FormControl fullWidth>
              <InputLabel id="crew-context-mission-label" htmlFor="crew-context-mission-native">Рейс / экипаж</InputLabel>
              <Select
                id="crew-context-mission"
                labelId="crew-context-mission-label"
                label="Рейс / экипаж"
                value={selectedMission?.id || ''}
                onChange={(event) => setSelectedMissionId(Number(event.target.value))}
                inputProps={{ id: 'crew-context-mission-native', 'aria-label': 'Рейс / экипаж' }}
              >
                {(missions || []).map((mission) => (
                  <MenuItem key={mission.id} value={mission.id}>
                    HRV-{mission.id} · {mission.crewName || 'без экипажа'} · {statusText(mission.status)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Panel>
        </Grid>
      )}
      <Grid size={{ xs: 12, lg: 7 }}>
        <Panel title="План и маршрут">
          <RouteMap mission={selectedMission} />
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Связь экипажа">
          <Stack spacing={2}>
            <MissionCard mission={selectedMission} compact />
            {evacuationCommand && (
              <Alert severity={['CREATED', 'SENT'].includes(evacuationCommand.status) ? 'error' : 'warning'}>
                <Stack spacing={1}>
                  <Typography fontWeight={700}>Команда эвакуации INC-{evacuationIncident.id}: {statusText(evacuationCommand.status)}</Typography>
                  <Typography variant="body2">Отправлена: {fmtDate(evacuationCommand.sentAt)} · доставлена: {fmtDate(evacuationCommand.deliveredAt)} · подтверждена: {fmtDate(evacuationCommand.acknowledgedAt)}</Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <Button size="small" variant="contained" color="error" disabled={!['CREATED', 'SENT'].includes(evacuationCommand.status)} onClick={markDelivered}>Команда получена</Button>
                    <Button size="small" disabled={evacuationCommand.status !== 'DELIVERED'} onClick={acknowledgeEvacuation}>Подтвердить решение</Button>
                  </Stack>
                </Stack>
              </Alert>
            )}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="telemetry-lat" name="lat" fullWidth type="number" label="Широта" value={telemetryForm.lat} onChange={(event) => updateTelemetry('lat', event.target.value)} />
              <TextField id="telemetry-lon" name="lon" fullWidth type="number" label="Долгота" value={telemetryForm.lon} onChange={(event) => updateTelemetry('lon', event.target.value)} />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="telemetry-event-time" name="eventTime" fullWidth type="datetime-local" label="Время события" value={telemetryForm.eventTime} onChange={(event) => updateTelemetry('eventTime', event.target.value)} />
              <FormControl fullWidth>
                <InputLabel id="telemetry-equipment-label" htmlFor="telemetry-equipment-native">Оборудование</InputLabel>
                <Select id="telemetry-equipment" labelId="telemetry-equipment-label" name="equipmentStatus" label="Оборудование" value={telemetryForm.equipmentStatus} onChange={(event) => updateTelemetry('equipmentStatus', event.target.value)} inputProps={{ id: 'telemetry-equipment-native', 'aria-label': 'Оборудование' }}>
                  <MenuItem value="NORMAL">Норма</MenuItem>
                  <MenuItem value="DEGRADED">Снижение качества</MenuItem>
                  <MenuItem value="WARN_SENSOR_DRIFT">Дрейф датчиков</MenuItem>
                  <MenuItem value="CRITICAL_DRIVE_STRESS">Критическая нагрузка</MenuItem>
                  <MenuItem value="DAMAGED">Повреждение</MenuItem>
                </Select>
              </FormControl>
            </Stack>
            <TextField id="alarm-reason" name="alarmReason" label="Причина тревоги" value={alarmReason} onChange={(event) => setAlarmReason(event.target.value)} />
            <Button startIcon={<CellTower />} variant="contained" disabled={!selectedMission} onClick={submitTelemetry}>Передать телеметрию</Button>
            <Button startIcon={<NotificationsActive />} color="error" disabled={!selectedMission} onClick={sendAlarm}>Отправить тревогу</Button>
            <Alert severity={telemetryQueue.length ? 'warning' : 'success'}>
              Пакетов в буфере повторной отправки: {telemetryQueue.length}
            </Alert>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Итоговый отчёт экипажа">
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
            <TextField id="report-actual-end" name="actualEnd" type="datetime-local" label="Фактическое окончание" value={reportForm.actualEnd} onChange={(event) => setReportForm((current) => ({ ...current, actualEnd: event.target.value }))} />
            <TextField id="report-spice-amount" name="spiceAmount" type="number" label="Объём пряности" value={reportForm.spiceAmount} onChange={(event) => setReportForm((current) => ({ ...current, spiceAmount: event.target.value }))} />
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="report-harvester-status-label" htmlFor="report-harvester-status-native">Статус харвестера</InputLabel>
              <Select id="report-harvester-status" labelId="report-harvester-status-label" name="harvesterFinalStatus" label="Статус харвестера" value={reportForm.harvesterFinalStatus} onChange={(event) => setReportForm((current) => ({ ...current, harvesterFinalStatus: event.target.value }))} inputProps={{ id: 'report-harvester-status-native', 'aria-label': 'Статус харвестера' }}>
                <MenuItem value="READY">Готов</MenuItem>
                <MenuItem value="MAINTENANCE">Требует ремонта</MenuItem>
                <MenuItem value="LOST">Потерян</MenuItem>
              </Select>
            </FormControl>
            <TextField id="report-abnormal-situations" name="abnormalSituations" sx={{ flex: 1 }} label="Нештатные ситуации" value={reportForm.abnormalSituations} onChange={(event) => setReportForm((current) => ({ ...current, abnormalSituations: event.target.value }))} />
            <Button startIcon={<ReceiptLong />} disabled={!selectedMission || !['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(selectedMission.status)} onClick={submitReport}>Отправить отчёт</Button>
          </Stack>
        </Panel>
      </Grid>
    </Grid>
  );
}
