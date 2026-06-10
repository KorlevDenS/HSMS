import { Button, Grid, Stack } from '@mui/material';
import { CheckCircle, Download, EmergencyRecording, ReportGmailerrorred } from '@mui/icons-material';
import { Kpi } from '../../shared/components/Kpi';
import { Panel } from '../../shared/components/Panel';
import { fmtNumber } from '../../shared/formatters';

export function ReportsView({ api, run, dashboard }) {
  async function download(path, filename) {
    await run('Отчёт выгружен', async () => {
      const csv = await api(path);
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      link.click();
      URL.revokeObjectURL(link.href);
      return {};
    });
  }

  return (
    <Grid container spacing={2}>
      <Kpi title="Закрытые рейсы" value={dashboard.closedMissions} icon={<CheckCircle />} />
      <Kpi title="Отменённые рейсы" value={dashboard.cancelledMissions} icon={<ReportGmailerrorred />} tone="warning" />
      <Kpi title="Средняя реакция" value={`${fmtNumber(dashboard.averageReactionSeconds, 0)} с`} icon={<EmergencyRecording />} />
      <Grid size={{ xs: 12 }}>
        <Panel title="Выгрузки">
          <Stack direction="row" spacing={1}>
            <Button startIcon={<Download />} onClick={() => download('/reports/missions.csv', 'missions.csv')}>Рейсы CSV</Button>
            <Button startIcon={<Download />} onClick={() => download('/reports/incidents.csv', 'incidents.csv')}>Инциденты CSV</Button>
          </Stack>
        </Panel>
      </Grid>
    </Grid>
  );
}
