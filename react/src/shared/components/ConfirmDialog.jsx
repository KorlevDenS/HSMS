import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

export function ConfirmDialog({ confirm, setConfirm }) {
  return (
    <Dialog open={Boolean(confirm)} onClose={() => setConfirm(null)} maxWidth="sm" fullWidth>
      <DialogTitle>{confirm?.title}</DialogTitle>
      <DialogContent><Typography>{confirm?.body}</Typography></DialogContent>
      <DialogActions>
        <Button onClick={() => setConfirm(null)}>Отмена</Button>
        <Button color={confirm?.danger ? 'error' : 'primary'} variant="contained" onClick={async () => {
          const action = confirm?.action;
          setConfirm(null);
          await action?.();
        }}>Подтвердить</Button>
      </DialogActions>
    </Dialog>
  );
}
