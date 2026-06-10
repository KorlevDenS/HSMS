import { Box, Grid, Paper, Stack, Typography } from '@mui/material';

export function Kpi({ title, value, icon, tone = 'primary' }) {
  return (
    <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
      <Paper className={`kpi tone-${tone}`} elevation={0}>
        <Stack direction="row" sx={{ alignItems: 'center' }} spacing={1.5}>
          <Box className="kpi-icon">{icon}</Box>
          <Box>
            <Typography color="text.secondary">{title}</Typography>
            <Typography variant="h2">{value ?? '—'}</Typography>
          </Box>
        </Stack>
      </Paper>
    </Grid>
  );
}
