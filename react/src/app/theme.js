import { createTheme } from '@mui/material';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1f6f68' },
    secondary: { main: '#8a4b2a' },
    error: { main: '#b3261e' },
    warning: { main: '#a66300' },
    success: { main: '#2e7d32' },
    background: { default: '#f6f7f3', paper: '#ffffff' }
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontSize: 28, fontWeight: 700 },
    h2: { fontSize: 20, fontWeight: 700 },
    h3: { fontSize: 17, fontWeight: 700 },
    button: { textTransform: 'none', fontWeight: 700, letterSpacing: 0 }
  },
  components: {
    MuiButton: { styleOverrides: { root: { minHeight: 38 } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } }
  }
});
