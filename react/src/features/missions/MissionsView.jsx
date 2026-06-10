import { useEffect, useState } from 'react';
import { Button, Checkbox, FormControl, FormControlLabel, Grid, InputLabel, MenuItem, Select, Stack, TextField } from '@mui/material';
import { PlayArrow, Radar, ReportGmailerrorred, Save } from '@mui/icons-material';
import { terminalMissionStatuses } from '../../shared/constants';
import { Panel } from '../../shared/components/Panel';
import { datetimeLocal, nowPlus, isoFromLocal } from '../../shared/formatters';
import { numberOrNull, parseRouteText, routeTextFromMission } from '../../shared/missionForms';
import { statusText } from '../../shared/status';
import { MissionCard } from './MissionCard';
import { MissionTable } from './MissionTable';

export function MissionsView({ api, run, ask, missions, selectedMission, setSelectedMissionId, zones, harvesters, crews }) {
  const [form, setForm] = useState(() => ({
    title: `Рейс добычи ${new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}`,
    zoneId: zones[0]?.id || 1,
    harvesterId: harvesters.find((item) => item.status === 'READY')?.id || harvesters[0]?.id || 1,
    crewId: crews.find((item) => item.status === 'READY')?.id || crews[0]?.id || 1,
    plannedStart: datetimeLocal(nowPlus(1)),
    plannedEnd: datetimeLocal(nowPlus(5)),
    routeText: '24.42, 54.20\n24.58, 54.33\n24.77, 54.48'
  }));
  const [launchForm, setLaunchForm] = useState({
    confirmWarning: false,
    reason: 'Проверены маршрут, окно добычи и готовность штаба безопасности'
  });
  const [closeForm, setCloseForm] = useState({
    finalStatus: 'CLOSED',
    reason: 'Рейс закрыт диспетчером после проверки отчёта экипажа'
  });

  useEffect(() => {
    setForm((current) => ({
      ...current,
      zoneId: current.zoneId === undefined ? zones[0]?.id || 1 : current.zoneId,
      harvesterId: current.harvesterId === undefined ? harvesters.find((item) => item.status === 'READY')?.id || harvesters[0]?.id || 1 : current.harvesterId,
      crewId: current.crewId === undefined ? crews.find((item) => item.status === 'READY')?.id || crews[0]?.id || 1 : current.crewId
    }));
  }, [zones, harvesters, crews]);

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function loadSelectedMission() {
    if (!selectedMission) return;
    setForm({
      title: selectedMission.title,
      zoneId: selectedMission.zoneId,
      harvesterId: selectedMission.harvesterId,
      crewId: selectedMission.crewId,
      plannedStart: datetimeLocal(selectedMission.plannedStart),
      plannedEnd: datetimeLocal(selectedMission.plannedEnd),
      routeText: routeTextFromMission(selectedMission)
    });
  }

  function missionPayload() {
    return {
      title: form.title,
      zoneId: numberOrNull(form.zoneId),
      harvesterId: numberOrNull(form.harvesterId),
      crewId: numberOrNull(form.crewId),
      plannedStart: isoFromLocal(form.plannedStart),
      plannedEnd: isoFromLocal(form.plannedEnd),
      route: parseRouteText(form.routeText)
    };
  }

  async function createMission() {
    const result = await run('Рейс создан', () => api('/missions', {
      method: 'POST',
      body: JSON.stringify(missionPayload())
    }));
    if (result?.id) setSelectedMissionId(result.id);
    return result;
  }

  async function updateMission() {
    if (!selectedMission) return null;
    return run('Черновик рейса обновлён', () => api(`/missions/${selectedMission.id}`, {
      method: 'PATCH',
      body: JSON.stringify(missionPayload())
    }));
  }

  async function launchMission() {
    if (!selectedMission) return null;
    return run('Рейс запущен', () => api(`/missions/${selectedMission.id}/launch`, {
      method: 'POST',
      body: JSON.stringify({
        confirmWarning: Boolean(launchForm.confirmWarning),
        reason: launchForm.reason
      })
    }));
  }

  async function closeMission() {
    if (!selectedMission) return null;
    return run('Рейс закрыт', () => api(`/missions/${selectedMission.id}/close`, {
      method: 'POST',
      body: JSON.stringify(closeForm)
    }));
  }

  const status = selectedMission?.status;
  const canEditMission = Boolean(selectedMission && !['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(status) && !terminalMissionStatuses.has(status));
  const canAssessRisk = Boolean(selectedMission && ['DRAFT', 'RISK_ASSESSED'].includes(status));
  const canLaunchMission = Boolean(selectedMission && status === 'RISK_ASSESSED');
  const canRiskCancelMission = Boolean(selectedMission && ['DRAFT', 'RISK_ASSESSED'].includes(status));
  const canCloseMission = Boolean(selectedMission && ['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(status));

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Планирование рейса">
          <Stack spacing={2}>
            <TextField id="mission-title" name="missionTitle" label="Название" value={form.title} onChange={(event) => updateForm('title', event.target.value)} />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <FormControl fullWidth>
                <InputLabel id="mission-zone-label" htmlFor="mission-zone-native">Зона</InputLabel>
                <Select id="mission-zone" labelId="mission-zone-label" name="zoneId" label="Зона" value={form.zoneId} onChange={(event) => updateForm('zoneId', event.target.value)} inputProps={{ id: 'mission-zone-native', 'aria-label': 'Зона' }}>
                  <MenuItem value="">Не задано</MenuItem>
                  {zones.map((zone) => <MenuItem key={zone.id} value={zone.id}>{zone.name}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel id="mission-harvester-label" htmlFor="mission-harvester-native">Харвестер</InputLabel>
                <Select id="mission-harvester" labelId="mission-harvester-label" name="harvesterId" label="Харвестер" value={form.harvesterId} onChange={(event) => updateForm('harvesterId', event.target.value)} inputProps={{ id: 'mission-harvester-native', 'aria-label': 'Харвестер' }}>
                  <MenuItem value="">Не задано</MenuItem>
                  {harvesters.map((harvester) => <MenuItem key={harvester.id} value={harvester.id}>{harvester.name} · {statusText(harvester.status)}</MenuItem>)}
                </Select>
              </FormControl>
            </Stack>
            <FormControl fullWidth>
              <InputLabel id="mission-crew-label" htmlFor="mission-crew-native">Экипаж</InputLabel>
              <Select id="mission-crew" labelId="mission-crew-label" name="crewId" label="Экипаж" value={form.crewId} onChange={(event) => updateForm('crewId', event.target.value)} inputProps={{ id: 'mission-crew-native', 'aria-label': 'Экипаж' }}>
                <MenuItem value="">Не задано</MenuItem>
                {crews.map((crew) => <MenuItem key={crew.id} value={crew.id}>{crew.name} · {statusText(crew.status)}</MenuItem>)}
              </Select>
            </FormControl>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="mission-planned-start" name="plannedStart" fullWidth type="datetime-local" label="Начало" value={form.plannedStart} onChange={(event) => updateForm('plannedStart', event.target.value)} />
              <TextField id="mission-planned-end" name="plannedEnd" fullWidth type="datetime-local" label="Окончание" value={form.plannedEnd} onChange={(event) => updateForm('plannedEnd', event.target.value)} />
            </Stack>
            <TextField id="mission-route" name="routeText" label="Маршрут, координаты по строкам" value={form.routeText} onChange={(event) => updateForm('routeText', event.target.value)} multiline minRows={4} />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <Button startIcon={<Save />} variant="contained" onClick={createMission}>Создать черновик</Button>
              <Button disabled={!selectedMission} onClick={loadSelectedMission}>Загрузить выбранный</Button>
              <Button disabled={!canEditMission} onClick={updateMission}>Сохранить изменения</Button>
            </Stack>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 7 }}>
        <Panel title="Команды рейса">
          <Stack spacing={1.5}>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button startIcon={<Radar />} disabled={!canAssessRisk} onClick={() => run('Риск рассчитан', () => api(`/missions/${selectedMission.id}/risk-assessments`, { method: 'POST', body: '{}' }))}>Рассчитать риск</Button>
          <Button startIcon={<PlayArrow />} disabled={!canLaunchMission} onClick={() => ask(
            'Запуск рейса',
            `Рейс HRV-${selectedMission.id}. Risk-score: ${selectedMission.risk?.riskScore ?? 'не рассчитан'}. При warning требуется явное подтверждение и основание.`,
            launchMission
          )}>Запустить</Button>
          <Button color="error" startIcon={<ReportGmailerrorred />} disabled={!canRiskCancelMission} onClick={() => ask(
            'Риск-отмена рейса',
            `Рейс HRV-${selectedMission.id} будет отменён, а данные уйдут в страховой контур.`,
            () => run('Рейс отменён по риску', () => api(`/missions/${selectedMission.id}/risk-cancel`, { method: 'POST', body: JSON.stringify({ reason: 'Риск выше допустимого порога' }) })),
            true
          )}>Риск-отмена</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ alignItems: { md: 'center' } }}>
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="mission-final-status-label" htmlFor="mission-final-status-native">Финальный статус</InputLabel>
              <Select id="mission-final-status" labelId="mission-final-status-label" name="finalStatus" label="Финальный статус" value={closeForm.finalStatus} onChange={(event) => setCloseForm((current) => ({ ...current, finalStatus: event.target.value }))} inputProps={{ id: 'mission-final-status-native', 'aria-label': 'Финальный статус' }}>
                <MenuItem value="CLOSED">Закрыт</MenuItem>
                <MenuItem value="CANCELLED">Отменён</MenuItem>
                <MenuItem value="LOST">Потерян</MenuItem>
              </Select>
            </FormControl>
            <TextField id="mission-close-reason" name="closeReason" sx={{ flex: 1 }} label="Основание закрытия" value={closeForm.reason} onChange={(event) => setCloseForm((current) => ({ ...current, reason: event.target.value }))} />
            <Button disabled={!canCloseMission} onClick={() => ask(
              'Закрытие рейса',
              `Рейс HRV-${selectedMission.id} будет переведён в статус ${statusText(closeForm.finalStatus)}.`,
              closeMission,
              closeForm.finalStatus !== 'CLOSED'
            )}>Закрыть рейс</Button>
          </Stack>
          <Stack spacing={0.5}>
            <FormControlLabel
              control={<Checkbox name="confirmWarning" checked={launchForm.confirmWarning} onChange={(event) => setLaunchForm((current) => ({ ...current, confirmWarning: event.target.checked }))} />}
              label="Подтвердить запуск при предупреждающем risk-score"
            />
            <TextField id="mission-launch-reason" name="launchReason" label="Основание запуска при предупреждении" value={launchForm.reason} onChange={(event) => setLaunchForm((current) => ({ ...current, reason: event.target.value }))} />
          </Stack>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 8 }}>
        <Panel title="Рейсы">
          <MissionTable missions={missions} selectedMission={selectedMission} onSelect={setSelectedMissionId} />
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 4 }}>
        <MissionCard mission={selectedMission} />
      </Grid>
    </Grid>
  );
}
