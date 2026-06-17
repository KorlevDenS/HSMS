import { Box, Button, FormControl, InputLabel, LinearProgress, MenuItem, Paper, Select, Stack, TextField, Typography } from '@mui/material';
import { Login, Shield } from '@mui/icons-material';
import { roles } from '../../app/navigation';
import { MessageSnackbar } from '../../shared/components/MessageSnackbar';

export function LoginScreen({ busy, loginName, message, password, setLoginName, setMessage, setPassword, signIn }) {
  return (
    <Box className="login-shell">
      <Paper className="login-panel" elevation={3}>
        <Stack
          component="form"
          spacing={3}
          onSubmit={(event) => {
            event.preventDefault();
            signIn();
          }}
        >
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Shield color="primary" />
            <Box>
              <Typography variant="h1">HSMS</Typography>
              <Typography color="text.secondary">Система управления снабжением Харконненов</Typography>
            </Box>
          </Stack>
          <FormControl fullWidth>
            <InputLabel id="login-user-label" htmlFor="login-user-native">Пользователь</InputLabel>
            <Select
              id="login-user"
              labelId="login-user-label"
              name="loginName"
              label="Пользователь"
              value={loginName}
              onChange={(event) => setLoginName(event.target.value)}
              inputProps={{ id: 'login-user-native', 'aria-label': 'Пользователь' }}
            >
              {roles.map(([value, label]) => (
                <MenuItem key={value} value={value}>
                  {label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            id="login-password"
            name="password"
            fullWidth
            label="Пароль"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
          <Button type="submit" variant="contained" startIcon={<Login />} disabled={busy}>
            Войти
          </Button>
          {busy && <LinearProgress />}
        </Stack>
      </Paper>
      <MessageSnackbar message={message} setMessage={setMessage} />
    </Box>
  );
}
