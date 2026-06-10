import { Alert, Snackbar } from '@mui/material';

export function MessageSnackbar({ message, setMessage }) {
  return (
    <Snackbar
      open={Boolean(message)}
      autoHideDuration={4000}
      onClose={() => setMessage(null)}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      sx={{ pointerEvents: 'none' }}
    >
      {message ? <Alert severity={message.severity} sx={{ pointerEvents: 'none' }}>{message.text}</Alert> : undefined}
    </Snackbar>
  );
}
