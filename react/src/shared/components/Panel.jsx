import { Paper, Typography } from '@mui/material';

export function Panel({ title, children, className = '' }) {
  return (
    <Paper className={`panel ${className}`.trim()} elevation={0}>
      <Typography variant="h3" gutterBottom>{title}</Typography>
      {children}
    </Paper>
  );
}
