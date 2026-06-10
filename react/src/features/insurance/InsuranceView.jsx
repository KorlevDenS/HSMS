import { useEffect, useState } from 'react';
import { Alert, Button, Chip, Grid, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { Cached, CheckCircle, GppMaybe, Policy, Radar, Save } from '@mui/icons-material';
import { Kpi } from '../../shared/components/Kpi';
import { Panel } from '../../shared/components/Panel';
import { fmtDate, fmtNumber } from '../../shared/formatters';
import { historyEventText, severityText, statusColor, statusText } from '../../shared/status';

function slaBreachText(value) {
  if (value === true) return 'Нарушен';
  if (value === false) return 'Не нарушен';
  return '—';
}

export function InsuranceView({ run, ask, cases, selectedCase, setSelectedCaseId, api }) {
  const [terms, setTerms] = useState({
    premium: selectedCase?.finalPremium || '',
    reason: 'Ручное обновление условий после проверки данных'
  });
  const [rejectReason, setRejectReason] = useState('Данные кейса требуют повторной проверки');
  const [closeReason, setCloseReason] = useState('Финальное страховое решение');

  useEffect(() => {
    setTerms((current) => ({ ...current, premium: selectedCase?.finalPremium || '' }));
  }, [selectedCase?.id, selectedCase?.finalPremium]);

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12 }}>
        <Panel title="Действия страхового контура">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
          <Button startIcon={<Cached />} disabled={!selectedCase} onClick={() => run('Страховой кейс пересчитан', () => api(`/insurance-cases/${selectedCase.id}/recalculate`, { method: 'POST', body: JSON.stringify({ reason: 'Перерасчёт по связанным данным рейса и инцидента' }) }))}>Пересчитать</Button>
          <TextField id="insurance-premium" name="premium" type="number" label="Премия" value={terms.premium} onChange={(event) => setTerms((current) => ({ ...current, premium: event.target.value }))} />
          <TextField id="insurance-terms-reason" name="termsReason" sx={{ flex: 1 }} label="Причина обновления" value={terms.reason} onChange={(event) => setTerms((current) => ({ ...current, reason: event.target.value }))} />
          <Button startIcon={<Save />} disabled={!selectedCase || !terms.premium} onClick={() => run('Условия обновлены', () => api(`/insurance-cases/${selectedCase.id}/terms`, { method: 'PATCH', body: JSON.stringify({ premium: Number(terms.premium), reason: terms.reason }) }))}>Обновить условия</Button>
        </Stack>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mt: 2 }}>
          <TextField id="insurance-reject-reason" name="rejectReason" sx={{ flex: 1 }} label="Причина отклонения" value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} />
          <Button color="warning" disabled={!selectedCase} onClick={() => run('Перерасчёт отклонён', () => api(`/insurance-cases/${selectedCase.id}/reject-recalculation`, { method: 'POST', body: JSON.stringify({ reason: rejectReason }) }))}>Отклонить</Button>
          <TextField id="insurance-close-reason" name="insuranceCloseReason" sx={{ flex: 1 }} label="Причина закрытия" value={closeReason} onChange={(event) => setCloseReason(event.target.value)} />
          <Button startIcon={<CheckCircle />} disabled={!selectedCase} onClick={() => ask(
            'Закрытие страхового кейса',
            `Кейс CLM-${selectedCase.id} будет закрыт с финальной премией ${fmtNumber(selectedCase.finalPremium, 2)}.`,
            () => run('Страховой кейс закрыт', () => api(`/insurance-cases/${selectedCase.id}/close`, { method: 'POST', body: JSON.stringify({ reason: closeReason }) })),
            true
          )}>Закрыть кейс</Button>
        </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Страховые кейсы">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Кейс</TableCell>
                <TableCell>Рейс</TableCell>
                <TableCell>Инцидент</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Risk-score</TableCell>
                <TableCell>Премия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {cases.map((item) => (
                <TableRow key={item.id} hover selected={item.id === selectedCase?.id} onClick={() => setSelectedCaseId(item.id)}>
                  <TableCell>CLM-{item.id}</TableCell>
                  <TableCell>HRV-{item.missionId}</TableCell>
                  <TableCell>{item.incidentId ? `INC-${item.incidentId}` : '—'}</TableCell>
                  <TableCell><Chip size="small" color={statusColor(item.status)} label={statusText(item.status)} /></TableCell>
                  <TableCell>{item.finalRiskScore ?? '—'}</TableCell>
                  <TableCell>{item.finalPremium !== null && item.finalPremium !== undefined ? `${fmtNumber(item.finalPremium, 2)} ₴` : '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Страховой payload">
          {selectedCase ? (
            <Grid container spacing={1.5}>
              <Kpi title="Триггер" value={statusText(selectedCase.triggerType)} icon={<Policy />} />
              <Kpi title="P(attack)" value={selectedCase.triggerPAttack !== null && selectedCase.triggerPAttack !== undefined ? `${fmtNumber(selectedCase.triggerPAttack * 100, 1)}%` : '—'} icon={<Radar />} />
              <Kpi title="Risk-score" value={selectedCase.triggerRiskScore ?? '—'} icon={<GppMaybe />} />
              <Kpi title="Решение" value={selectedCase.triggerDecisionBy || '—'} icon={<CheckCircle />} />
              <Grid size={{ xs: 12 }}>
                <Table size="small">
                  <TableBody>
                    <TableRow><TableCell>Причина</TableCell><TableCell>{selectedCase.triggerReason || '—'}</TableCell></TableRow>
                    <TableRow><TableCell>Время решения</TableCell><TableCell>{fmtDate(selectedCase.triggerDecisionAt)}</TableCell></TableRow>
                    <TableRow><TableCell>Инцидент</TableCell><TableCell>{selectedCase.incidentId ? `INC-${selectedCase.incidentId}` : '—'}</TableCell></TableRow>
                    <TableRow><TableCell>Критичность</TableCell><TableCell>{severityText(selectedCase.incidentSeverity)}</TableCell></TableRow>
                    <TableRow><TableCell>Регистрация / закрытие</TableCell><TableCell>{fmtDate(selectedCase.incidentRegisteredAt)} – {fmtDate(selectedCase.incidentClosedAt)}</TableCell></TableRow>
                    <TableRow><TableCell>НВР</TableCell><TableCell>{slaBreachText(selectedCase.incidentSlaBreached)}</TableCell></TableRow>
                    <TableRow><TableCell>Оператор</TableCell><TableCell>{selectedCase.incidentOperator || '—'}</TableCell></TableRow>
                  </TableBody>
                </Table>
              </Grid>
              {selectedCase.missingData && (
                <Grid size={{ xs: 12 }}>
                  <Alert severity="warning">Недостающие сведения: {selectedCase.missingData}</Alert>
                </Grid>
              )}
            </Grid>
          ) : <Typography color="text.secondary">Нет выбранного страхового кейса</Typography>}
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="История кейса">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Время</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Risk-score</TableCell>
                <TableCell>Премия</TableCell>
                <TableCell>Основание</TableCell>
                <TableCell>Оператор</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(selectedCase?.history || []).map((item) => (
                <TableRow key={item.id}>
                  <TableCell>{fmtDate(item.calculatedAt)}</TableCell>
                  <TableCell>{historyEventText(item.eventType)}</TableCell>
                  <TableCell>{item.oldRiskScore ?? '—'} → {item.newRiskScore}</TableCell>
                  <TableCell>{item.oldPremium !== null && item.oldPremium !== undefined ? fmtNumber(item.oldPremium, 2) : '—'} → {fmtNumber(item.newPremium, 2)}</TableCell>
                  <TableCell>{item.rejectedReason || item.reason}</TableCell>
                  <TableCell>{item.calculatedBy}</TableCell>
                </TableRow>
              ))}
              {!(selectedCase?.history || []).length && (
                <TableRow>
                  <TableCell colSpan={6}>Истории кейса пока нет</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </Panel>
      </Grid>
    </Grid>
  );
}
