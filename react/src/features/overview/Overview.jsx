import { Chip, Divider, Grid, Stack, Typography } from '@mui/material';
import { EmergencyRecording, GppMaybe, Policy, Route } from '@mui/icons-material';
import { Kpi } from '../../shared/components/Kpi';
import { Panel } from '../../shared/components/Panel';
import { fmtNumber } from '../../shared/formatters';
import { severityColor, severityText, statusColor, statusText } from '../../shared/status';
import { MissionTable } from '../missions/MissionTable';

export function Overview({ dashboard, missions, incidents }) {
  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Kpi title="Активные рейсы" value={dashboard.activeMissions} icon={<Route />} />
        <Kpi title="Открытые инциденты" value={dashboard.openIncidents} icon={<GppMaybe />} tone="error" />
        <Kpi title="Соблюдение НВР" value={`${fmtNumber(dashboard.slaCompliancePercent, 1)}%`} icon={<EmergencyRecording />} tone="success" />
        <Kpi title="Открытые кейсы" value={dashboard.openInsuranceCases} icon={<Policy />} />
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Panel title="Последние рейсы">
            <MissionTable missions={missions.slice(0, 7)} onSelect={() => undefined} />
          </Panel>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Panel title="Критичность инцидентов">
            <Stack spacing={1.25}>
              {Object.entries(dashboard.incidentsBySeverity || {}).map(([severity, count]) => (
                <Stack key={severity} direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                  <Chip size="small" color={severityColor(severity)} label={severityText(severity)} />
                  <Typography fontWeight={700}>{count}</Typography>
                </Stack>
              ))}
              <Divider />
              {incidents.slice(0, 5).map((incident) => (
                <Stack key={incident.id} direction="row" sx={{ justifyContent: 'space-between' }}>
                  <Typography>INC-{incident.id}</Typography>
                  <Chip size="small" color={statusColor(incident.status)} label={statusText(incident.status)} />
                </Stack>
              ))}
            </Stack>
          </Panel>
        </Grid>
      </Grid>
    </Stack>
  );
}
