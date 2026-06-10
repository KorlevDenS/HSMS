import { useEffect, useMemo, useState } from 'react';
import { Button, FormControl, Grid, InputLabel, MenuItem, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField } from '@mui/material';
import { Save } from '@mui/icons-material';
import { emptyNewUser, shortRole } from '../../app/navigation';
import { Panel } from '../../shared/components/Panel';
import { RoleSelect } from '../../shared/components/RoleSelect';
import { detailsText, fmtDate } from '../../shared/formatters';
import { objectLabel } from '../../shared/status';

export function AdminView({ api, run, users, policy, audit, missions, incidents, cases }) {
  const [warningThreshold, setWarningThreshold] = useState(policy?.warningThreshold || 50);
  const [blockThreshold, setBlockThreshold] = useState(policy?.blockThreshold || 75);
  const [changeReason, setChangeReason] = useState('');
  const [validatedScenarios, setValidatedScenarios] = useState('normal launch, warning launch, blocking risk, degraded telemetry, incident, CHOAM insurance');
  const [choamImpact, setChoamImpact] = useState('Risk policy version remains traceable in risk snapshots and insurance decisions');
  const [newUser, setNewUser] = useState(emptyNewUser);
  const [roleDrafts, setRoleDrafts] = useState({});
  const [historyTarget, setHistoryTarget] = useState('');

  useEffect(() => {
    if (policy) {
      setWarningThreshold(policy.warningThreshold);
      setBlockThreshold(policy.blockThreshold);
    }
  }, [policy]);

  useEffect(() => {
    setRoleDrafts(Object.fromEntries(users.map((user) => [user.id, user.roles])));
  }, [users]);

  const historyTargets = useMemo(() => [
    ...(missions || []).map((mission) => ({ value: `mission:${mission.id}`, label: `HRV-${mission.id} · ${mission.title}` })),
    ...(incidents || []).map((incident) => ({ value: `incident:${incident.id}`, label: `INC-${incident.id} · HRV-${incident.missionId}` })),
    ...(cases || []).map((item) => ({ value: `insurance_case:${item.id}`, label: `CLM-${item.id} · HRV-${item.missionId}` }))
  ], [missions, incidents, cases]);

  useEffect(() => {
    if ((!historyTarget || !historyTargets.some((target) => target.value === historyTarget)) && historyTargets.length) {
      setHistoryTarget(historyTargets[0].value);
    }
  }, [historyTarget, historyTargets]);

  const historyRows = useMemo(() => {
    if (!historyTarget) return [];
    const [type, idText] = historyTarget.split(':');
    const id = Number(idText);
    return [...(audit || [])]
      .filter((event) => {
        if (type === 'mission') {
          return Number(event.missionId) === id || (event.objectType === 'mission' && Number(event.objectId) === id);
        }
        if (type === 'incident') {
          return (event.objectType === 'incident' && Number(event.objectId) === id) || Number(event.details?.incidentId) === id;
        }
        return event.objectType === type && Number(event.objectId) === id;
      })
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }, [audit, historyTarget]);

  function updateNewUser(field, value) {
    setNewUser((current) => ({ ...current, [field]: value }));
  }

  async function createUser() {
    const created = await run('Пользователь создан', () => api('/users', {
      method: 'POST',
      body: JSON.stringify(newUser)
    }));
    if (created) {
      setNewUser(emptyNewUser);
    }
  }

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Политика риска">
          <Stack spacing={2}>
            <TextField id="risk-warning-threshold" name="warningThreshold" type="number" label="Порог предупреждения" value={warningThreshold} onChange={(event) => setWarningThreshold(Number(event.target.value))} />
            <TextField id="risk-block-threshold" name="blockThreshold" type="number" label="Порог блокировки" value={blockThreshold} onChange={(event) => setBlockThreshold(Number(event.target.value))} />
            <TextField id="risk-policy-reason" name="changeReason" label="Основание изменения" value={changeReason} onChange={(event) => setChangeReason(event.target.value)} multiline minRows={2} required />
            <TextField id="risk-policy-scenarios" name="validatedScenarios" label="Проверенные сценарии" value={validatedScenarios} onChange={(event) => setValidatedScenarios(event.target.value)} multiline minRows={2} required />
            <TextField id="risk-policy-choam-impact" name="choamImpact" label="Влияние на CHOAM" value={choamImpact} onChange={(event) => setChoamImpact(event.target.value)} multiline minRows={2} required />
            <Button startIcon={<Save />} variant="contained" onClick={() => run('Политика риска обновлена', () => api('/risk-policy', {
              method: 'PATCH',
              body: JSON.stringify({
                version: `policy-${Date.now()}`,
                warningThreshold,
                blockThreshold,
                formulaDescription: policy?.formulaDescription || 'Детерминированная формула P(attack) и risk-score',
                changeReason,
                validatedScenarios,
                choamImpact
              })
            }))} disabled={!changeReason.trim() || !validatedScenarios.trim() || !choamImpact.trim()}>Сохранить пороги</Button>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Новый пользователь">
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="new-user-login" name="newUserLogin" fullWidth label="Логин" value={newUser.login} onChange={(event) => updateNewUser('login', event.target.value)} />
              <TextField id="new-user-password" name="newUserPassword" fullWidth type="password" label="Пароль" value={newUser.password} onChange={(event) => updateNewUser('password', event.target.value)} />
            </Stack>
            <TextField id="new-user-display-name" name="newUserDisplayName" label="Имя" value={newUser.displayName} onChange={(event) => updateNewUser('displayName', event.target.value)} />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="new-user-email" name="newUserEmail" fullWidth label="Электронная почта" value={newUser.email} onChange={(event) => updateNewUser('email', event.target.value)} />
              <TextField id="new-user-phone" name="newUserPhone" fullWidth label="Телефон" value={newUser.phone} onChange={(event) => updateNewUser('phone', event.target.value)} />
            </Stack>
            <RoleSelect value={newUser.roles} onChange={(value) => updateNewUser('roles', value)} />
            <Button startIcon={<Save />} variant="contained" onClick={createUser}>Создать пользователя</Button>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 7 }}>
        <Panel title="Пользователи">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Логин</TableCell>
                <TableCell>Имя</TableCell>
                <TableCell>Роли</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.login}</TableCell>
                  <TableCell>{user.displayName}</TableCell>
                  <TableCell>
                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ alignItems: { md: 'center' } }}>
                      <RoleSelect value={roleDrafts[user.id] || user.roles} onChange={(value) => setRoleDrafts((current) => ({ ...current, [user.id]: value }))} compact />
                      <Button size="small" onClick={() => run('Роли обновлены', () => api(`/users/${user.id}/roles`, {
                        method: 'PATCH',
                        body: JSON.stringify({ roles: roleDrafts[user.id] || user.roles })
                      }))}>Сохранить</Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="История объекта">
          <Stack spacing={2}>
            <FormControl fullWidth>
              <InputLabel id="admin-history-target-label" htmlFor="admin-history-target-native">Объект</InputLabel>
              <Select id="admin-history-target" labelId="admin-history-target-label" name="historyTarget" label="Объект" value={historyTarget} onChange={(event) => setHistoryTarget(event.target.value)} inputProps={{ id: 'admin-history-target-native', 'aria-label': 'Объект' }}>
                {historyTargets.map((target) => <MenuItem key={target.value} value={target.value}>{target.label}</MenuItem>)}
              </Select>
            </FormControl>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Время</TableCell>
                  <TableCell>Действие</TableCell>
                  <TableCell>Объект</TableCell>
                  <TableCell>Пользователь</TableCell>
                  <TableCell>Детали</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {historyRows.map((event) => (
                  <TableRow key={event.id}>
                    <TableCell>{fmtDate(event.createdAt)}</TableCell>
                    <TableCell>{event.action}</TableCell>
                    <TableCell>{objectLabel(event.objectType, event.objectId)}</TableCell>
                    <TableCell>{event.actorLogin} · {shortRole(event.actorRole)}</TableCell>
                    <TableCell>{detailsText(event.details)}</TableCell>
                  </TableRow>
                ))}
                {!historyRows.length && (
                  <TableRow>
                    <TableCell colSpan={5}>История по выбранному объекту пуста</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Stack>
        </Panel>
      </Grid>
    </Grid>
  );
}
