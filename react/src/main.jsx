import React, { useCallback, useEffect, useId, useLayoutEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Alert,
  AppBar,
  Badge,
  Box,
  Button,
  Checkbox,
  Chip,
  CssBaseline,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  LinearProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  ThemeProvider,
  Toolbar,
  Tooltip,
  Typography,
  createTheme
} from '@mui/material';
import {
  AdminPanelSettings,
  Assessment,
  Cached,
  CellTower,
  CheckCircle,
  Download,
  EmergencyRecording,
  GppMaybe,
  LocalPolice,
  Login,
  Logout,
  Map,
  NotificationsActive,
  PlayArrow,
  Policy,
  Radar,
  ReceiptLong,
  ReportGmailerrorred,
  Route,
  Save,
  Security,
  Shield,
  Tune
} from '@mui/icons-material';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const TELEMETRY_QUEUE_KEY = 'hsms.telemetry.queue.v1';
const SESSION_KEY = 'hsms.session.v1';
const terminalMissionStatuses = new Set(['CLOSED', 'LOST', 'CANCELLED', 'RISK_CANCELLED']);
const terminalIncidentStatuses = new Set(['CLOSED', 'EVACUATION_ACKNOWLEDGED', 'MONITORING']);
const terminalEvacuationStatuses = new Set(['ACKNOWLEDGED', 'DELIVERY_FAILED', 'EXPIRED', 'CANCELLED']);
const transientHttpStatuses = new Set([0, 408, 425, 429, 500, 502, 503, 504]);
const criticalRealtimeActions = new Set([
  'ALARM_RECEIVED',
  'INCIDENT_CREATED',
  'INCIDENT_SLA_BREACHED',
  'EVACUATION_COMMAND_SENT',
  'EVACUATION_DELIVERY_FAILED',
  'EVACUATION_ACK_EXPIRED',
  'MISSION_RISK_CANCELLED'
]);

function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function isTransientApiError(error) {
  return transientHttpStatuses.has(error?.status);
}

const roles = [
  ['dispatcher', 'Диспетчер', 'ROLE_SUPPLY_MANAGER'],
  ['crew', 'Экипаж', 'ROLE_HARVESTER_CREW'],
  ['security', 'Штаб безопасности', 'ROLE_SECURITY_HEADQUARTERS_OPERATOR'],
  ['insurance', 'Страхование', 'ROLE_INSURANCE_CONTOUR_OPERATOR'],
  ['management', 'Руководство', 'ROLE_OPERATIONS_MANAGEMENT'],
  ['admin', 'Администратор', 'ROLE_ADMINISTRATOR']
];

const roleOptions = [
  'ROLE_SUPPLY_MANAGER',
  'ROLE_HARVESTER_CREW',
  'ROLE_SECURITY_HEADQUARTERS_OPERATOR',
  'ROLE_INSURANCE_CONTOUR_OPERATOR',
  'ROLE_OPERATIONS_MANAGEMENT',
  'ROLE_ADMINISTRATOR'
];

const emptyNewUser = {
  login: '',
  password: '',
  displayName: '',
  email: '',
  phone: '',
  roles: ['ROLE_OPERATIONS_MANAGEMENT']
};

const sections = [
  ['overview', 'Обзор', Assessment, ['ROLE_SUPPLY_MANAGER', 'ROLE_OPERATIONS_MANAGEMENT', 'ROLE_ADMINISTRATOR']],
  ['missions', 'Рейсы', Route, ['ROLE_SUPPLY_MANAGER', 'ROLE_ADMINISTRATOR']],
  ['crew', 'Экипаж', CellTower, ['ROLE_HARVESTER_CREW', 'ROLE_ADMINISTRATOR']],
  ['security', 'НВР', LocalPolice, ['ROLE_SECURITY_HEADQUARTERS_OPERATOR', 'ROLE_ADMINISTRATOR']],
  ['insurance', 'Страхование', Policy, ['ROLE_INSURANCE_CONTOUR_OPERATOR', 'ROLE_ADMINISTRATOR']],
  ['reports', 'Отчёты', ReceiptLong, ['ROLE_OPERATIONS_MANAGEMENT', 'ROLE_ADMINISTRATOR']],
  ['admin', 'Админ', AdminPanelSettings, ['ROLE_ADMINISTRATOR']]
];

const theme = createTheme({
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

function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY)) || null;
  } catch {
    return null;
  }
}

function loadTelemetryQueue() {
  try {
    return JSON.parse(localStorage.getItem(TELEMETRY_QUEUE_KEY)) || [];
  } catch {
    return [];
  }
}

function saveTelemetryQueue(queue) {
  localStorage.setItem(TELEMETRY_QUEUE_KEY, JSON.stringify(queue));
}

function fmtDate(value) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function fmtNumber(value, digits = 0) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—';
  return new Intl.NumberFormat('ru-RU', { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(Number(value));
}

function nowPlus(hours) {
  return new Date(Date.now() + hours * 60 * 60 * 1000).toISOString();
}

function datetimeLocal(value) {
  if (!value) return '';
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function isoFromLocal(value) {
  return value ? new Date(value).toISOString() : null;
}

function parseRouteText(value) {
  return (value || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [lat, lon] = line.split(/[,\s;]+/).filter(Boolean).map(Number);
      if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
        throw new Error('Маршрут должен содержать строки формата lat, lon');
      }
      return { seqNo: index + 1, lat, lon };
    });
}

function routeTextFromMission(mission) {
  return (mission?.route || [])
    .map((point) => `${point.lat}, ${point.lon}`)
    .join('\n');
}

function blankToNull(value) {
  return value === '' || value === null || value === undefined ? null : value;
}

function numberOrNull(value) {
  const normalized = blankToNull(value);
  return normalized === null ? null : Number(normalized);
}

function detailsText(details) {
  if (!details || Object.keys(details).length === 0) return '—';
  return Object.entries(details)
    .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : String(value)}`)
    .join('; ');
}

function historyEventText(value) {
  return {
    RECALCULATION: 'Перерасчёт',
    TERMS_UPDATED: 'Изменение условий'
  }[value] || value || '—';
}

function objectLabel(type, id) {
  return {
    mission: `HRV-${id}`,
    incident: `INC-${id}`,
    insurance_case: `CLM-${id}`
  }[type] || `${type || 'object'}-${id}`;
}

function statusText(value) {
  return {
    DRAFT: 'Черновик',
    READY_FOR_RISK: 'Готов к риску',
    RISK_ASSESSED: 'Риск рассчитан',
    ACTIVE: 'Активен',
    RISK_CANCELLED: 'Риск-отмена',
    CANCELLED: 'Отменён',
    COMPLETED_PENDING_CLOSE: 'Ожидает закрытия',
    LOST: 'Потерян',
    CLOSED: 'Закрыт',
    OPEN: 'Новый',
    CLASSIFIED: 'Классифицирован',
    MONITORING: 'Мониторинг',
    EVACUATION_ORDERED: 'Эвакуация',
    EVACUATION_ACKNOWLEDGED: 'Подтверждена',
    WAITING_FOR_DATA: 'Ожидает данные',
    READY_FOR_RECALCULATION: 'К перерасчёту',
    RECALCULATED: 'Пересчитан',
    TERMS_UPDATED: 'Условия обновлены',
    REJECTED: 'Отклонён',
    READY: 'Готов',
    BUSY: 'Занят',
    MAINTENANCE: 'Обслуживание',
    CREATED: 'Создана',
    SENT: 'Отправлена',
    DELIVERED: 'Доставлена',
    ACKNOWLEDGED: 'Подтверждена',
    DELIVERY_FAILED: 'Ошибка доставки',
    EXPIRED: 'Просрочена',
    RISK_CANCELLATION: 'Риск-отмена',
    INCIDENT: 'Инцидент',
    SLA_BREACH: 'Нарушение НВР',
    MISSION_LOSS: 'Потеря рейса',
    MISSION_CLOSE: 'Закрытие рейса'
  }[value] || value || '—';
}

function severityColor(value) {
  return {
    LOW: 'success',
    MEDIUM: 'info',
    HIGH: 'warning',
    CRITICAL: 'error'
  }[value] || 'default';
}

function severityText(value) {
  return {
    LOW: 'Низкая',
    MEDIUM: 'Средняя',
    HIGH: 'Высокая',
    CRITICAL: 'Критическая'
  }[value] || value || '—';
}

function riskDecisionText(value) {
  return {
    ALLOWED: 'разрешён',
    WARNING: 'требует подтверждения',
    BLOCKING: 'блокирует запуск'
  }[value] || value || '—';
}

function missionStatusHint(mission) {
  if (!mission) return 'Нет выбранного рейса';
  if (mission.draftMissingFields) return 'Черновик неполный';
  if (mission.riskReviewRequiredAt && !terminalMissionStatuses.has(mission.status)) return 'Нужен новый risk snapshot';
  if (mission.risk?.stale && !terminalMissionStatuses.has(mission.status)) return 'Risk snapshot устарел';
  if (mission.status === 'RISK_ASSESSED') return `Риск ${mission.risk?.riskScore ?? '—'}/100, можно запускать по решению`;
  if (mission.status === 'ACTIVE' && !mission.plan?.acknowledgedAt) return 'План опубликован, экипаж не подтвердил';
  if (mission.status === 'ACTIVE') return 'Рейс выполняется';
  if (mission.status === 'COMPLETED_PENDING_CLOSE') return 'Ожидает итоговый отчёт и закрытие';
  if (terminalMissionStatuses.has(mission.status)) return 'Финальный статус';
  return 'Ожидает следующего действия';
}

function useNow(intervalMs = 1000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(timer);
  }, [intervalMs]);
  return now;
}

function useMuiSelectHiddenInputLabels() {
  useLayoutEffect(() => {
    const labelHiddenInputs = () => {
      document.querySelectorAll('.MuiSelect-nativeInput[aria-hidden="true"]').forEach((input) => {
        const display = input.parentElement?.querySelector('[role="combobox"]');
        if (!display) return;

        const labelledBy = display.getAttribute('aria-labelledby');
        if (labelledBy) {
          input.setAttribute('aria-labelledby', labelledBy);
          return;
        }

        const label = display.getAttribute('aria-label');
        if (label) {
          input.setAttribute('aria-label', label);
        }
      });
    };

    labelHiddenInputs();
    const observer = new MutationObserver(labelHiddenInputs);
    observer.observe(document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);
}

function slaState(incident, now = Date.now()) {
  if (!incident) return { label: '—', tone: 'primary' };
  if (incident.slaBreached) return { label: 'Нарушен', tone: 'error' };
  if (terminalIncidentStatuses.has(incident.status)) return { label: 'Соблюдён', tone: 'success' };
  if (!incident.slaDeadlineAt) return { label: '—', tone: 'primary' };
  if (now >= new Date(incident.slaDeadlineAt).getTime()) return { label: 'Нарушен', tone: 'error' };
  const secondsLeft = Math.floor((new Date(incident.slaDeadlineAt).getTime() - now) / 1000);
  return {
    label: timeLeft(incident.slaDeadlineAt, now),
    tone: secondsLeft <= 60 ? 'warning' : 'success'
  };
}

function statusColor(value) {
  if (['ACTIVE', 'DELIVERED', 'RISK_ASSESSED', 'RECALCULATED', 'TERMS_UPDATED'].includes(value)) return 'success';
  if (['RISK_CANCELLED', 'LOST', 'EXPIRED', 'DELIVERY_FAILED', 'REJECTED'].includes(value)) return 'error';
  if (['OPEN', 'CLASSIFIED', 'EVACUATION_ORDERED', 'READY_FOR_RECALCULATION', 'WAITING_FOR_DATA', 'COMPLETED_PENDING_CLOSE', 'CREATED', 'SENT'].includes(value)) return 'warning';
  return 'default';
}

function App() {
  useMuiSelectHiddenInputLabels();
  const [session, setSession] = useState(loadSession);
  const [loginName, setLoginName] = useState(loadSession()?.user?.login || 'dispatcher');
  const [password, setPassword] = useState('');
  const [data, setData] = useState(null);
  const [section, setSection] = useState('overview');
  const [selectedMissionId, setSelectedMissionId] = useState(null);
  const [selectedIncidentId, setSelectedIncidentId] = useState(null);
  const [selectedCaseId, setSelectedCaseId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [telemetryQueue, setTelemetryQueue] = useState(loadTelemetryQueue);
  const [streamState, setStreamState] = useState('offline');

  const api = useCallback(async (path, options = {}) => {
    let response;
    try {
      response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
          ...(options.headers || {})
        }
      });
    } catch (error) {
      error.status = 0;
      throw error;
    }
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      const apiError = new Error(error.action ? `${error.message}. ${error.action}` : error.message || `Ошибка API ${response.status}`);
      apiError.status = response.status;
      apiError.path = path;
      throw apiError;
    }
    if (response.headers.get('content-type')?.includes('text/csv')) {
      return response.text();
    }
    return response.json();
  }, [session?.token]);

  const refresh = useCallback(async (nextMessage) => {
    if (!session?.token) return;
    let lastError = null;
    for (let attempt = 0; attempt < 5; attempt += 1) {
      try {
        const bootstrap = await api('/bootstrap');
        setData(bootstrap);
        setStreamState((current) => current === 'offline' ? 'online' : current);
        if (bootstrap.missions?.length) {
          const latestMissionId = [...bootstrap.missions].sort((a, b) => b.id - a.id)[0].id;
          setSelectedMissionId((current) => current || latestMissionId);
        }
        if (nextMessage) setMessage({ severity: 'success', text: nextMessage });
        return bootstrap;
      } catch (error) {
        lastError = error;
        if (!isTransientApiError(error) || attempt === 4) break;
        setStreamState('degraded');
        await sleep(Math.min(8000, 700 * 2 ** attempt));
      }
    }
    throw lastError;
  }, [api, session?.token]);

  useEffect(() => {
    if (session?.token) {
      refresh().catch((error) => {
        setMessage({
          severity: isTransientApiError(error) ? 'warning' : 'error',
          text: isTransientApiError(error)
            ? 'Сервер временно недоступен. Данные обновятся автоматически.'
            : error.message
        });
      });
    }
  }, [refresh, session?.token]);

  useEffect(() => {
    if (!session?.token) return undefined;
    const controller = new AbortController();
    let reconnectTimer = null;
    let reconnectAttempt = 0;
    let buffer = '';
    function handleBlock(block) {
      const event = block.split('\n').find((line) => line.startsWith('event:'))?.slice(6).trim();
      const dataLine = block.split('\n').find((line) => line.startsWith('data:'))?.slice(5).trim();
      if (!dataLine) return;
      const payload = JSON.parse(dataLine);
      const action = payload.action || event;
      setStreamState('online');
      if (action === 'CONNECTED') return;
      setMessage({ severity: realtimeSeverity(action), text: realtimeText(action) });
      refresh().catch(() => undefined);
    }
    async function connect() {
      try {
        buffer = '';
        const response = await fetch(`${API_BASE}/incidents/stream`, {
          headers: { Authorization: `Bearer ${session.token}` },
          signal: controller.signal
        });
        if (!response.ok || !response.body) throw new Error('Поток событий недоступен');
        reconnectAttempt = 0;
        setStreamState('online');
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        while (!controller.signal.aborted) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const blocks = buffer.split('\n\n');
          buffer = blocks.pop() || '';
          blocks.forEach((block) => {
            try {
              handleBlock(block);
            } catch {
              setStreamState('degraded');
            }
          });
        }
        if (!controller.signal.aborted) scheduleReconnect();
      } catch {
        if (!controller.signal.aborted) scheduleReconnect();
      }
    }
    function scheduleReconnect() {
      setStreamState('degraded');
      reconnectAttempt += 1;
      const delay = Math.min(30000, 1000 * 2 ** Math.min(reconnectAttempt, 5));
      reconnectTimer = window.setTimeout(connect, delay);
    }
    connect();
    return () => {
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      controller.abort();
    };
  }, [refresh, session?.token]);

  const flushTelemetryQueue = useCallback(async () => {
    if (!session?.token || telemetryQueue.length === 0) return;
    const remaining = [];
    for (const item of telemetryQueue) {
      try {
        await api(`/missions/${item.missionId}/telemetry`, {
          method: 'POST',
          body: JSON.stringify(item.payload)
        });
      } catch {
        remaining.push(item);
      }
    }
    setTelemetryQueue(remaining);
    saveTelemetryQueue(remaining);
    if (remaining.length === 0) {
      await refresh('Буфер телеметрии отправлен');
    }
  }, [api, refresh, session?.token, telemetryQueue]);

  useEffect(() => {
    const onOnline = () => flushTelemetryQueue();
    window.addEventListener('online', onOnline);
    const interval = window.setInterval(() => flushTelemetryQueue(), 8000);
    return () => {
      window.removeEventListener('online', onOnline);
      window.clearInterval(interval);
    };
  }, [flushTelemetryQueue]);

  async function signIn() {
    setBusy(true);
    try {
      const loginResponse = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ login: loginName, password })
      });
      if (!loginResponse.ok) {
        const error = await loginResponse.json().catch(() => ({}));
        throw new Error(error.message || 'Вход не выполнен');
      }
      const nextSession = await loginResponse.json();
      setSession(nextSession);
      localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
      setSection(defaultSection(nextSession.user.roles));
      setMessage({ severity: 'success', text: 'Вход выполнен' });
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  }

  function signOut() {
    setSession(null);
    setData(null);
    localStorage.removeItem(SESSION_KEY);
    setStreamState('offline');
  }

  async function run(label, operation) {
    setBusy(true);
    try {
      const result = await operation();
      try {
        await refresh(label);
      } catch (error) {
        setStreamState('degraded');
        setMessage({
          severity: 'warning',
          text: isTransientApiError(error)
            ? 'Операция выполнена. Данные обновятся автоматически после восстановления связи.'
            : error.message
        });
      }
      return result;
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
      return null;
    } finally {
      setBusy(false);
    }
  }

  function ask(title, body, action, danger = false) {
    setConfirm({ title, body, action, danger });
  }

  const orderedMissions = useMemo(() => [...(data?.missions || [])].sort((a, b) => b.id - a.id), [data]);
  const selectedMission = useMemo(
    () => orderedMissions.find((mission) => mission.id === selectedMissionId) || orderedMissions[0],
    [orderedMissions, selectedMissionId]
  );
  const selectedIncident = useMemo(
    () => data?.incidents?.find((incident) => incident.id === selectedIncidentId) ||
      data?.incidents?.find((incident) => selectedMission?.incidentIds?.includes(incident.id)) ||
      data?.incidents?.[0],
    [data, selectedIncidentId, selectedMission]
  );
  const selectedCase = useMemo(
    () => data?.insuranceCases?.find((item) => item.id === selectedCaseId) ||
      data?.insuranceCases?.find((item) => item.id === selectedMission?.insuranceCaseId) ||
      data?.insuranceCases?.[0],
    [data, selectedCaseId, selectedMission]
  );

  const visibleSections = useMemo(() => {
    const roleSet = new Set(session?.user?.roles || []);
    return sections.filter(([, , , allowed]) => allowed.some((role) => roleSet.has(role)));
  }, [session?.user?.roles]);

  if (!session?.token) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
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
                <Select id="login-user" labelId="login-user-label" name="loginName" value={loginName} label="Пользователь" onChange={(event) => setLoginName(event.target.value)} inputProps={{ id: 'login-user-native', autoComplete: 'username', 'aria-label': 'Пользователь' }}>
                  {roles.map(([value, label]) => (
                    <MenuItem key={value} value={value}>{label}</MenuItem>
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
      </ThemeProvider>
    );
  }

  const dashboard = data?.dashboard || {};
  const isAdmin = session.user.roles.includes('ROLE_ADMINISTRATOR');

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box className="app-shell">
        <Drawer variant="permanent" className="app-drawer" slotProps={{ paper: { className: 'drawer-paper' } }}>
          <Stack spacing={2} className="drawer-content">
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }} className="brand">
                <Shield />
                <Box>
                  <Typography variant="h3">HSMS</Typography>
                  <Typography variant="caption">Операции Арракиса</Typography>
                </Box>
              </Stack>
            <List>
              {visibleSections.map(([key, label, Icon]) => (
                <ListItemButton key={key} selected={section === key} onClick={() => setSection(key)}>
                  <ListItemIcon><Icon /></ListItemIcon>
                  <ListItemText primary={label} />
                  {key === 'security' && dashboard.openIncidents > 0 && <Badge color="error" badgeContent={dashboard.openIncidents} />}
                </ListItemButton>
              ))}
            </List>
            <Box sx={{ flex: 1 }} />
            <Stack spacing={1}>
              <Chip size="small" icon={<NotificationsActive />} label={streamState === 'online' ? 'События онлайн' : streamState === 'degraded' ? 'События нестабильны' : 'События офлайн'} color={streamState === 'online' ? 'success' : 'warning'} />
              <Chip size="small" icon={<CellTower />} label={`Буфер: ${telemetryQueue.length}`} color={telemetryQueue.length ? 'warning' : 'default'} />
            </Stack>
          </Stack>
        </Drawer>

        <Box className="main-shell">
          <AppBar position="sticky" color="inherit" elevation={0} className="topbar">
            <Toolbar>
              <Box sx={{ flex: 1 }}>
                <Typography variant="h1">{sectionTitle(section)}</Typography>
                <Typography color="text.secondary">{session.user.displayName}</Typography>
              </Box>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                {isAdmin && orderedMissions.length > 0 && (
                  <FormControl size="small" className="admin-context-select">
                    <InputLabel id="admin-context-mission-label" htmlFor="admin-context-mission-native">Рейс / экипаж</InputLabel>
                    <Select
                      id="admin-context-mission"
                      labelId="admin-context-mission-label"
                      label="Рейс / экипаж"
                      value={selectedMission?.id || ''}
                      onChange={(event) => setSelectedMissionId(Number(event.target.value))}
                      inputProps={{ id: 'admin-context-mission-native', 'aria-label': 'Рейс / экипаж' }}
                    >
                      {orderedMissions.map((mission) => (
                        <MenuItem key={mission.id} value={mission.id}>
                          HRV-{mission.id} · {mission.crewName || 'без экипажа'} · {statusText(mission.status)}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
                <Tooltip title="Обновить данные">
                  <IconButton onClick={() => refresh('Данные обновлены')}><Cached /></IconButton>
                </Tooltip>
                <Chip icon={<Security />} label={session.user.roles.map(shortRole).join(', ')} />
                <Button variant="outlined" startIcon={<Logout />} onClick={signOut}>Выйти</Button>
              </Stack>
            </Toolbar>
            {busy && <LinearProgress />}
          </AppBar>

          <Box className="content">
            {section === 'overview' && <Overview dashboard={dashboard} missions={orderedMissions} incidents={data?.incidents || []} />}
            {section === 'missions' && <MissionsView api={api} run={run} ask={ask} missions={orderedMissions} selectedMission={selectedMission} setSelectedMissionId={setSelectedMissionId} zones={data?.zones || []} harvesters={data?.harvesters || []} crews={data?.crews || []} />}
            {section === 'crew' && <CrewView api={api} run={run} missions={orderedMissions} selectedMission={selectedMission} setSelectedMissionId={setSelectedMissionId} canSwitchMission={isAdmin} incidents={data?.incidents || []} telemetryQueue={telemetryQueue} setTelemetryQueue={setTelemetryQueue} />}
            {section === 'security' && <SecurityView run={run} ask={ask} incidents={data?.incidents || []} missions={orderedMissions} selectedIncident={selectedIncident} setSelectedIncidentId={setSelectedIncidentId} api={api} />}
            {section === 'insurance' && <InsuranceView run={run} ask={ask} cases={data?.insuranceCases || []} selectedCase={selectedCase} setSelectedCaseId={setSelectedCaseId} api={api} />}
            {section === 'reports' && <ReportsView api={api} run={run} dashboard={dashboard} />}
            {section === 'admin' && <AdminView api={api} run={run} users={data?.users || []} policy={data?.activeRiskPolicy} audit={data?.audit || []} missions={orderedMissions} incidents={data?.incidents || []} cases={data?.insuranceCases || []} />}
          </Box>
        </Box>
      </Box>
      <ConfirmDialog confirm={confirm} setConfirm={setConfirm} />
      <MessageSnackbar message={message} setMessage={setMessage} />
    </ThemeProvider>
  );
}

function Overview({ dashboard, missions, incidents }) {
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

function MissionsView({ api, run, ask, missions, selectedMission, setSelectedMissionId, zones, harvesters, crews }) {
  const [form, setForm] = useState(() => ({
    title: `Рейс добычи ${new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}`,
    zoneId: zones[0]?.id || 1,
    harvesterId: harvesters.find((item) => item.status === 'READY')?.id || harvesters[0]?.id || 1,
    crewId: crews.find((item) => item.status === 'READY')?.id || crews[0]?.id || 1,
    plannedStart: datetimeLocal(nowPlus(1)),
    plannedEnd: datetimeLocal(nowPlus(5)),
    routeText: '24.42, 54.20\n24.58, 54.33\n24.77, 54.48'
  }));
  const [launchForm, setLaunchForm] = useState({
    confirmWarning: false,
    reason: 'Проверены маршрут, окно добычи и готовность штаба безопасности'
  });
  const [closeForm, setCloseForm] = useState({
    finalStatus: 'CLOSED',
    reason: 'Рейс закрыт диспетчером после проверки отчёта экипажа'
  });

  useEffect(() => {
    setForm((current) => ({
      ...current,
      zoneId: current.zoneId === undefined ? zones[0]?.id || 1 : current.zoneId,
      harvesterId: current.harvesterId === undefined ? harvesters.find((item) => item.status === 'READY')?.id || harvesters[0]?.id || 1 : current.harvesterId,
      crewId: current.crewId === undefined ? crews.find((item) => item.status === 'READY')?.id || crews[0]?.id || 1 : current.crewId
    }));
  }, [zones, harvesters, crews]);

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function loadSelectedMission() {
    if (!selectedMission) return;
    setForm({
      title: selectedMission.title,
      zoneId: selectedMission.zoneId,
      harvesterId: selectedMission.harvesterId,
      crewId: selectedMission.crewId,
      plannedStart: datetimeLocal(selectedMission.plannedStart),
      plannedEnd: datetimeLocal(selectedMission.plannedEnd),
      routeText: routeTextFromMission(selectedMission)
    });
  }

  function missionPayload() {
    return {
      title: form.title,
      zoneId: numberOrNull(form.zoneId),
      harvesterId: numberOrNull(form.harvesterId),
      crewId: numberOrNull(form.crewId),
      plannedStart: isoFromLocal(form.plannedStart),
      plannedEnd: isoFromLocal(form.plannedEnd),
      route: parseRouteText(form.routeText)
    };
  }

  async function createMission() {
    const result = await run('Рейс создан', () => api('/missions', {
      method: 'POST',
      body: JSON.stringify(missionPayload())
    }));
    if (result?.id) setSelectedMissionId(result.id);
    return result;
  }

  async function updateMission() {
    if (!selectedMission) return null;
    return run('Черновик рейса обновлён', () => api(`/missions/${selectedMission.id}`, {
      method: 'PATCH',
      body: JSON.stringify(missionPayload())
    }));
  }

  async function launchMission() {
    if (!selectedMission) return null;
    return run('Рейс запущен', () => api(`/missions/${selectedMission.id}/launch`, {
      method: 'POST',
      body: JSON.stringify({
        confirmWarning: Boolean(launchForm.confirmWarning),
        reason: launchForm.reason
      })
    }));
  }

  async function closeMission() {
    if (!selectedMission) return null;
    return run('Рейс закрыт', () => api(`/missions/${selectedMission.id}/close`, {
      method: 'POST',
      body: JSON.stringify(closeForm)
    }));
  }

  const status = selectedMission?.status;
  const canEditMission = Boolean(selectedMission && !['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(status) && !terminalMissionStatuses.has(status));
  const canAssessRisk = Boolean(selectedMission && ['DRAFT', 'RISK_ASSESSED'].includes(status));
  const canLaunchMission = Boolean(selectedMission && status === 'RISK_ASSESSED');
  const canRiskCancelMission = Boolean(selectedMission && ['DRAFT', 'RISK_ASSESSED'].includes(status));
  const canCloseMission = Boolean(selectedMission && ['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(status));

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Планирование рейса">
          <Stack spacing={2}>
            <TextField id="mission-title" name="missionTitle" label="Название" value={form.title} onChange={(event) => updateForm('title', event.target.value)} />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <FormControl fullWidth>
                <InputLabel id="mission-zone-label" htmlFor="mission-zone-native">Зона</InputLabel>
                <Select id="mission-zone" labelId="mission-zone-label" name="zoneId" label="Зона" value={form.zoneId} onChange={(event) => updateForm('zoneId', event.target.value)} inputProps={{ id: 'mission-zone-native', 'aria-label': 'Зона' }}>
                  <MenuItem value="">Не задано</MenuItem>
                  {zones.map((zone) => <MenuItem key={zone.id} value={zone.id}>{zone.name}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel id="mission-harvester-label" htmlFor="mission-harvester-native">Харвестер</InputLabel>
                <Select id="mission-harvester" labelId="mission-harvester-label" name="harvesterId" label="Харвестер" value={form.harvesterId} onChange={(event) => updateForm('harvesterId', event.target.value)} inputProps={{ id: 'mission-harvester-native', 'aria-label': 'Харвестер' }}>
                  <MenuItem value="">Не задано</MenuItem>
                  {harvesters.map((harvester) => <MenuItem key={harvester.id} value={harvester.id}>{harvester.name} · {statusText(harvester.status)}</MenuItem>)}
                </Select>
              </FormControl>
            </Stack>
            <FormControl fullWidth>
              <InputLabel id="mission-crew-label" htmlFor="mission-crew-native">Экипаж</InputLabel>
              <Select id="mission-crew" labelId="mission-crew-label" name="crewId" label="Экипаж" value={form.crewId} onChange={(event) => updateForm('crewId', event.target.value)} inputProps={{ id: 'mission-crew-native', 'aria-label': 'Экипаж' }}>
                <MenuItem value="">Не задано</MenuItem>
                {crews.map((crew) => <MenuItem key={crew.id} value={crew.id}>{crew.name} · {statusText(crew.status)}</MenuItem>)}
              </Select>
            </FormControl>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="mission-planned-start" name="plannedStart" fullWidth type="datetime-local" label="Начало" value={form.plannedStart} onChange={(event) => updateForm('plannedStart', event.target.value)} />
              <TextField id="mission-planned-end" name="plannedEnd" fullWidth type="datetime-local" label="Окончание" value={form.plannedEnd} onChange={(event) => updateForm('plannedEnd', event.target.value)} />
            </Stack>
            <TextField id="mission-route" name="routeText" label="Маршрут, координаты по строкам" value={form.routeText} onChange={(event) => updateForm('routeText', event.target.value)} multiline minRows={4} />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <Button startIcon={<Save />} variant="contained" onClick={createMission}>Создать черновик</Button>
              <Button disabled={!selectedMission} onClick={loadSelectedMission}>Загрузить выбранный</Button>
              <Button disabled={!canEditMission} onClick={updateMission}>Сохранить изменения</Button>
            </Stack>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 7 }}>
        <Panel title="Команды рейса">
          <Stack spacing={1.5}>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button startIcon={<Radar />} disabled={!canAssessRisk} onClick={() => run('Риск рассчитан', () => api(`/missions/${selectedMission.id}/risk-assessments`, { method: 'POST', body: '{}' }))}>Рассчитать риск</Button>
          <Button startIcon={<PlayArrow />} disabled={!canLaunchMission} onClick={() => ask(
            'Запуск рейса',
            `Рейс HRV-${selectedMission.id}. Risk-score: ${selectedMission.risk?.riskScore ?? 'не рассчитан'}. При warning требуется явное подтверждение и основание.`,
            launchMission
          )}>Запустить</Button>
          <Button color="error" startIcon={<ReportGmailerrorred />} disabled={!canRiskCancelMission} onClick={() => ask(
            'Риск-отмена рейса',
            `Рейс HRV-${selectedMission.id} будет отменён, а данные уйдут в страховой контур.`,
            () => run('Рейс отменён по риску', () => api(`/missions/${selectedMission.id}/risk-cancel`, { method: 'POST', body: JSON.stringify({ reason: 'Риск выше допустимого порога' }) })),
            true
          )}>Риск-отмена</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ alignItems: { md: 'center' } }}>
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="mission-final-status-label" htmlFor="mission-final-status-native">Финальный статус</InputLabel>
              <Select id="mission-final-status" labelId="mission-final-status-label" name="finalStatus" label="Финальный статус" value={closeForm.finalStatus} onChange={(event) => setCloseForm((current) => ({ ...current, finalStatus: event.target.value }))} inputProps={{ id: 'mission-final-status-native', 'aria-label': 'Финальный статус' }}>
                <MenuItem value="CLOSED">Закрыт</MenuItem>
                <MenuItem value="CANCELLED">Отменён</MenuItem>
                <MenuItem value="LOST">Потерян</MenuItem>
              </Select>
            </FormControl>
            <TextField id="mission-close-reason" name="closeReason" sx={{ flex: 1 }} label="Основание закрытия" value={closeForm.reason} onChange={(event) => setCloseForm((current) => ({ ...current, reason: event.target.value }))} />
            <Button disabled={!canCloseMission} onClick={() => ask(
              'Закрытие рейса',
              `Рейс HRV-${selectedMission.id} будет переведён в статус ${statusText(closeForm.finalStatus)}.`,
              closeMission,
              closeForm.finalStatus !== 'CLOSED'
            )}>Закрыть рейс</Button>
          </Stack>
          <Stack spacing={0.5}>
            <FormControlLabel
              control={<Checkbox name="confirmWarning" checked={launchForm.confirmWarning} onChange={(event) => setLaunchForm((current) => ({ ...current, confirmWarning: event.target.checked }))} />}
              label="Подтвердить запуск при предупреждающем risk-score"
            />
            <TextField id="mission-launch-reason" name="launchReason" label="Основание запуска при предупреждении" value={launchForm.reason} onChange={(event) => setLaunchForm((current) => ({ ...current, reason: event.target.value }))} />
          </Stack>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 8 }}>
        <Panel title="Рейсы">
          <MissionTable missions={missions} selectedMission={selectedMission} onSelect={setSelectedMissionId} />
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 4 }}>
        <MissionCard mission={selectedMission} />
      </Grid>
    </Grid>
  );
}

function CrewView({ api, run, missions, selectedMission, setSelectedMissionId, canSwitchMission, incidents, telemetryQueue, setTelemetryQueue }) {
  const firstPoint = selectedMission?.route?.[0];
  const evacuationIncident = useMemo(() => [...(incidents || [])]
    .filter((incident) => incident.missionId === selectedMission?.id && incident.evacuationCommand)
    .sort((a, b) => b.id - a.id)[0], [incidents, selectedMission?.id]);
  const evacuationCommand = evacuationIncident?.evacuationCommand;
  const [telemetryForm, setTelemetryForm] = useState({
    lat: firstPoint?.lat || 24.42,
    lon: firstPoint?.lon || 54.20,
    equipmentStatus: 'NORMAL',
    eventTime: datetimeLocal(new Date().toISOString())
  });
  const [alarmReason, setAlarmReason] = useState('Акустическая и сейсмическая сигнатура угрозы');
  const [reportForm, setReportForm] = useState({
    actualEnd: datetimeLocal(new Date().toISOString()),
    spiceAmount: 0,
    harvesterFinalStatus: 'READY',
    abnormalSituations: 'Нет'
  });

  useEffect(() => {
    if (!firstPoint) return;
    setTelemetryForm((current) => ({
      ...current,
      lat: firstPoint.lat,
      lon: firstPoint.lon
    }));
  }, [firstPoint?.lat, firstPoint?.lon]);

  function updateTelemetry(field, value) {
    setTelemetryForm((current) => ({ ...current, [field]: value }));
  }

  async function submitTelemetry() {
    if (!selectedMission) return null;
    const payload = {
      externalEventId: `tel-${Date.now()}`,
      eventTime: isoFromLocal(telemetryForm.eventTime),
      lat: Number(telemetryForm.lat),
      lon: Number(telemetryForm.lon),
      equipmentStatus: telemetryForm.equipmentStatus
    };
    const result = await run('Телеметрия принята', () => api(`/missions/${selectedMission.id}/telemetry`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }));
    if (!result) {
      const queue = [...telemetryQueue, { missionId: selectedMission.id, payload }];
      setTelemetryQueue(queue);
      saveTelemetryQueue(queue);
    }
    return result;
  }

  async function sendAlarm() {
    if (!selectedMission) return null;
    return run('Тревога принята штабом', () => api(`/missions/${selectedMission.id}/alarms`, {
      method: 'POST',
      body: JSON.stringify({ externalEventId: `alarm-${Date.now()}`, eventTime: new Date().toISOString(), reason: alarmReason })
    }));
  }

  async function submitReport() {
    if (!selectedMission) return null;
    return run('Итоговый отчёт отправлен', () => api(`/missions/${selectedMission.id}/report`, {
      method: 'POST',
      body: JSON.stringify({
        actualStart: selectedMission.actualStart,
        actualEnd: isoFromLocal(reportForm.actualEnd),
        spiceAmount: Number(reportForm.spiceAmount),
        harvesterFinalStatus: reportForm.harvesterFinalStatus,
        abnormalSituations: reportForm.abnormalSituations
      })
    }));
  }

  async function markDelivered() {
    if (!evacuationIncident) return null;
    return run('Команда доставлена экипажу', () => api(`/incidents/${evacuationIncident.id}/evacuation/delivered`, {
      method: 'POST',
      body: '{}'
    }));
  }

  async function acknowledgeEvacuation() {
    if (!evacuationIncident) return null;
    return run('Эвакуация подтверждена', () => api(`/incidents/${evacuationIncident.id}/evacuation/ack`, {
      method: 'POST',
      body: '{}'
    }));
  }

  return (
    <Grid container spacing={2}>
      {canSwitchMission && (
        <Grid size={{ xs: 12 }}>
          <Panel title="Отображаемый экипаж">
            <FormControl fullWidth>
              <InputLabel id="crew-context-mission-label" htmlFor="crew-context-mission-native">Рейс / экипаж</InputLabel>
              <Select
                id="crew-context-mission"
                labelId="crew-context-mission-label"
                label="Рейс / экипаж"
                value={selectedMission?.id || ''}
                onChange={(event) => setSelectedMissionId(Number(event.target.value))}
                inputProps={{ id: 'crew-context-mission-native', 'aria-label': 'Рейс / экипаж' }}
              >
                {(missions || []).map((mission) => (
                  <MenuItem key={mission.id} value={mission.id}>
                    HRV-{mission.id} · {mission.crewName || 'без экипажа'} · {statusText(mission.status)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Panel>
        </Grid>
      )}
      <Grid size={{ xs: 12, lg: 7 }}>
        <Panel title="План и маршрут">
          <RouteMap mission={selectedMission} />
        </Panel>
      </Grid>
      <Grid size={{ xs: 12, lg: 5 }}>
        <Panel title="Связь экипажа">
          <Stack spacing={2}>
            <MissionCard mission={selectedMission} compact />
            {evacuationCommand && (
              <Alert severity={['CREATED', 'SENT'].includes(evacuationCommand.status) ? 'error' : 'warning'}>
                <Stack spacing={1}>
                  <Typography fontWeight={700}>Команда эвакуации INC-{evacuationIncident.id}: {statusText(evacuationCommand.status)}</Typography>
                  <Typography variant="body2">Отправлена: {fmtDate(evacuationCommand.sentAt)} · доставлена: {fmtDate(evacuationCommand.deliveredAt)} · подтверждена: {fmtDate(evacuationCommand.acknowledgedAt)}</Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <Button size="small" variant="contained" color="error" disabled={!['CREATED', 'SENT'].includes(evacuationCommand.status)} onClick={markDelivered}>Команда получена</Button>
                    <Button size="small" disabled={evacuationCommand.status !== 'DELIVERED'} onClick={acknowledgeEvacuation}>Подтвердить решение</Button>
                  </Stack>
                </Stack>
              </Alert>
            )}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="telemetry-lat" name="lat" fullWidth type="number" label="Широта" value={telemetryForm.lat} onChange={(event) => updateTelemetry('lat', event.target.value)} />
              <TextField id="telemetry-lon" name="lon" fullWidth type="number" label="Долгота" value={telemetryForm.lon} onChange={(event) => updateTelemetry('lon', event.target.value)} />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField id="telemetry-event-time" name="eventTime" fullWidth type="datetime-local" label="Время события" value={telemetryForm.eventTime} onChange={(event) => updateTelemetry('eventTime', event.target.value)} />
              <FormControl fullWidth>
                <InputLabel id="telemetry-equipment-label" htmlFor="telemetry-equipment-native">Оборудование</InputLabel>
                <Select id="telemetry-equipment" labelId="telemetry-equipment-label" name="equipmentStatus" label="Оборудование" value={telemetryForm.equipmentStatus} onChange={(event) => updateTelemetry('equipmentStatus', event.target.value)} inputProps={{ id: 'telemetry-equipment-native', 'aria-label': 'Оборудование' }}>
                  <MenuItem value="NORMAL">Норма</MenuItem>
                  <MenuItem value="DEGRADED">Снижение качества</MenuItem>
                  <MenuItem value="WARN_SENSOR_DRIFT">Дрейф датчиков</MenuItem>
                  <MenuItem value="CRITICAL_DRIVE_STRESS">Критическая нагрузка</MenuItem>
                  <MenuItem value="DAMAGED">Повреждение</MenuItem>
                </Select>
              </FormControl>
            </Stack>
            <TextField id="alarm-reason" name="alarmReason" label="Причина тревоги" value={alarmReason} onChange={(event) => setAlarmReason(event.target.value)} />
            <Button startIcon={<CellTower />} variant="contained" disabled={!selectedMission} onClick={submitTelemetry}>Передать телеметрию</Button>
            <Button startIcon={<NotificationsActive />} color="error" disabled={!selectedMission} onClick={sendAlarm}>Отправить тревогу</Button>
            <Alert severity={telemetryQueue.length ? 'warning' : 'success'}>
              Пакетов в буфере повторной отправки: {telemetryQueue.length}
            </Alert>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Итоговый отчёт экипажа">
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
            <TextField id="report-actual-end" name="actualEnd" type="datetime-local" label="Фактическое окончание" value={reportForm.actualEnd} onChange={(event) => setReportForm((current) => ({ ...current, actualEnd: event.target.value }))} />
            <TextField id="report-spice-amount" name="spiceAmount" type="number" label="Объём пряности" value={reportForm.spiceAmount} onChange={(event) => setReportForm((current) => ({ ...current, spiceAmount: event.target.value }))} />
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="report-harvester-status-label" htmlFor="report-harvester-status-native">Статус харвестера</InputLabel>
              <Select id="report-harvester-status" labelId="report-harvester-status-label" name="harvesterFinalStatus" label="Статус харвестера" value={reportForm.harvesterFinalStatus} onChange={(event) => setReportForm((current) => ({ ...current, harvesterFinalStatus: event.target.value }))} inputProps={{ id: 'report-harvester-status-native', 'aria-label': 'Статус харвестера' }}>
                <MenuItem value="READY">Готов</MenuItem>
                <MenuItem value="MAINTENANCE">Требует ремонта</MenuItem>
                <MenuItem value="LOST">Потерян</MenuItem>
              </Select>
            </FormControl>
            <TextField id="report-abnormal-situations" name="abnormalSituations" sx={{ flex: 1 }} label="Нештатные ситуации" value={reportForm.abnormalSituations} onChange={(event) => setReportForm((current) => ({ ...current, abnormalSituations: event.target.value }))} />
            <Button startIcon={<ReceiptLong />} disabled={!selectedMission || !['ACTIVE', 'COMPLETED_PENDING_CLOSE'].includes(selectedMission.status)} onClick={submitReport}>Отправить отчёт</Button>
          </Stack>
        </Panel>
      </Grid>
    </Grid>
  );
}

function SecurityView({ run, ask, incidents, missions, selectedIncident, setSelectedIncidentId, api }) {
  const now = useNow();
  const [classification, setClassification] = useState({
    severity: 'HIGH',
    reason: 'Сигнатура угрозы подтверждена по телеметрии'
  });
  const [evacuationReason, setEvacuationReason] = useState('Решение штаба безопасности');
  const [deliveryFailureReason, setDeliveryFailureReason] = useState('Канал связи не подтвердил доставку команды');
  const incidentMission = missions.find((mission) => mission.id === selectedIncident?.missionId);
  const selectedSla = slaState(selectedIncident, now);
  const selectedIncidentClosed = !selectedIncident || selectedIncident.status === 'CLOSED';
  const selectedEvacuationStatus = selectedIncident?.evacuationCommand?.status;
  const selectedEvacuationTerminal = terminalEvacuationStatuses.has(selectedEvacuationStatus);

  function updateClassification(field, value) {
    setClassification((current) => ({ ...current, [field]: value }));
  }

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12 }}>
        <Panel title="Решение штаба">
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
            <FormControl sx={{ minWidth: 180 }}>
              <InputLabel id="incident-severity-label" htmlFor="incident-severity-native">Критичность</InputLabel>
              <Select id="incident-severity" labelId="incident-severity-label" name="severity" label="Критичность" value={classification.severity} onChange={(event) => updateClassification('severity', event.target.value)} inputProps={{ id: 'incident-severity-native', 'aria-label': 'Критичность' }}>
                <MenuItem value="LOW">Низкая</MenuItem>
                <MenuItem value="MEDIUM">Средняя</MenuItem>
                <MenuItem value="HIGH">Высокая</MenuItem>
                <MenuItem value="CRITICAL">Критическая</MenuItem>
              </Select>
            </FormControl>
            <TextField id="classification-reason" name="classificationReason" sx={{ flex: 1 }} label="Причина классификации" value={classification.reason} onChange={(event) => updateClassification('reason', event.target.value)} />
            <Button startIcon={<Tune />} disabled={selectedIncidentClosed} onClick={() => run('Инцидент классифицирован', () => api(`/incidents/${selectedIncident.id}/classification`, { method: 'PATCH', body: JSON.stringify(classification) }))}>Классифицировать</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mt: 2 }}>
            <TextField id="evacuation-reason" name="evacuationReason" sx={{ flex: 1 }} label="Основание эвакуации" value={evacuationReason} onChange={(event) => setEvacuationReason(event.target.value)} />
          <Button color="error" startIcon={<EmergencyRecording />} disabled={selectedIncidentClosed} onClick={() => ask(
            'Команда эвакуации',
            `Команда будет отправлена экипажу рейса HRV-${selectedIncident.missionId}; таймер подтверждения начнёт отсчёт сразу.`,
            () => run('Команда эвакуации отправлена', () => api(`/incidents/${selectedIncident.id}/evacuation`, { method: 'POST', body: JSON.stringify({ reason: evacuationReason }) })),
            true
          )}>Эвакуация</Button>
          <Button disabled={selectedIncidentClosed} onClick={() => run('Инцидент закрыт', () => api(`/incidents/${selectedIncident.id}/close`, { method: 'POST', body: '{}' }))}>Закрыть</Button>
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mt: 2 }}>
            <TextField id="evacuation-delivery-failure-reason" name="deliveryFailureReason" sx={{ flex: 1 }} label="Причина сбоя доставки" value={deliveryFailureReason} onChange={(event) => setDeliveryFailureReason(event.target.value)} />
            <Button color="warning" disabled={selectedIncidentClosed || !selectedIncident?.evacuationCommand || selectedEvacuationTerminal} onClick={() => run('Сбой доставки зафиксирован', () => api(`/incidents/${selectedIncident.id}/evacuation/delivery-failed`, { method: 'POST', body: JSON.stringify({ reason: deliveryFailureReason }) }))}>Сбой доставки</Button>
          </Stack>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Контекст инцидента" className="security-context-panel">
          <Grid container spacing={1.5}>
            <Kpi title="Рейс" value={selectedIncident ? `HRV-${selectedIncident.missionId}` : '—'} icon={<Route />} />
            <Kpi title="Статус НВР" value={selectedSla.label} icon={<EmergencyRecording />} tone={selectedSla.tone} />
            <Kpi title="Критичность" value={severityText(selectedIncident?.severity)} icon={<GppMaybe />} tone={selectedIncident?.severity === 'CRITICAL' ? 'error' : 'warning'} />
            <Kpi title="Команда" value={statusText(selectedIncident?.evacuationCommand?.status)} icon={<NotificationsActive />} />
          </Grid>
          {selectedIncident?.classificationReason && (
            <Alert severity="info" className="security-context-note">
              {selectedIncident.classificationReason}
            </Alert>
          )}
          <Box className="security-context-details">
            {incidentMission && <MissionCard mission={incidentMission} compact embedded />}
          </Box>
        </Panel>
      </Grid>
      <Grid size={{ xs: 12 }}>
        <Panel title="Очередь инцидентов">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Рейс</TableCell>
                <TableCell>Критичность</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>НВР</TableCell>
                <TableCell>Эвакуация</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {incidents.map((incident) => {
                const incidentSla = slaState(incident, now);
                return (
                <TableRow key={incident.id} hover selected={incident.id === selectedIncident?.id} onClick={() => setSelectedIncidentId(incident.id)}>
                  <TableCell>INC-{incident.id}</TableCell>
                  <TableCell>HRV-{incident.missionId}</TableCell>
                  <TableCell><Chip size="small" color={severityColor(incident.severity)} label={severityText(incident.severity)} /></TableCell>
                  <TableCell><Chip size="small" color={statusColor(incident.status)} label={statusText(incident.status)} /></TableCell>
                  <TableCell><Chip size="small" color={incidentSla.tone === 'primary' ? 'default' : incidentSla.tone} label={incidentSla.label} /></TableCell>
                  <TableCell>{incident.evacuationCommand ? statusText(incident.evacuationCommand.status) : '—'}</TableCell>
                </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Panel>
      </Grid>
    </Grid>
  );
}

function InsuranceView({ run, ask, cases, selectedCase, setSelectedCaseId, api }) {
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
                    <TableRow><TableCell>НВР</TableCell><TableCell>{selectedCase.incidentSlaBreached === true ? 'Нарушен' : selectedCase.incidentSlaBreached === false ? 'Не нарушен' : '—'}</TableCell></TableRow>
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

function ReportsView({ api, run, dashboard }) {
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

function AdminView({ api, run, users, policy, audit, missions, incidents, cases }) {
  const [warningThreshold, setWarningThreshold] = useState(policy?.warningThreshold || 50);
  const [blockThreshold, setBlockThreshold] = useState(policy?.blockThreshold || 75);
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
            <Button startIcon={<Save />} variant="contained" onClick={() => run('Политика риска обновлена', () => api('/risk-policy', {
              method: 'PATCH',
              body: JSON.stringify({
                version: `policy-${Date.now()}`,
                warningThreshold,
                blockThreshold,
                formulaDescription: policy?.formulaDescription || 'Детерминированная формула P(attack) и risk-score'
              })
            }))}>Сохранить пороги</Button>
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

function RoleSelect({ value, onChange, compact }) {
  const id = useId();
  const labelId = `roles-label-${id}`;
  const selectId = `roles-select-${id}`;
  const nativeInputId = `roles-native-${id}`;
  return (
    <FormControl fullWidth={!compact} sx={compact ? { minWidth: 260 } : undefined}>
      <InputLabel id={labelId} htmlFor={nativeInputId}>Роли</InputLabel>
      <Select
        id={selectId}
        labelId={labelId}
        name="roles"
        multiple
        label="Роли"
        value={value || []}
        inputProps={{ id: nativeInputId, 'aria-label': 'Роли' }}
        onChange={(event) => {
          const nextValue = event.target.value;
          onChange(typeof nextValue === 'string' ? nextValue.split(',') : nextValue);
        }}
        renderValue={(selected) => selected.map(shortRole).join(', ')}
      >
        {roleOptions.map((role) => (
          <MenuItem key={role} value={role}>
            <Checkbox checked={(value || []).includes(role)} />
            <ListItemText primary={shortRole(role)} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

function MissionTable({ missions, selectedMission, onSelect }) {
  return (
    <Table size="small">
      <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Зона</TableCell>
            <TableCell>Окно</TableCell>
            <TableCell>Экипаж</TableCell>
            <TableCell>Мониторинг</TableCell>
            <TableCell>Риск</TableCell>
          </TableRow>
      </TableHead>
      <TableBody>
        {missions.map((mission) => (
          <TableRow key={mission.id} hover selected={mission.id === selectedMission?.id} onClick={() => onSelect(mission.id)}>
            <TableCell>HRV-{mission.id}</TableCell>
            <TableCell><MissionStatusCell mission={mission} /></TableCell>
            <TableCell>{mission.zoneName || '—'}</TableCell>
            <TableCell>{fmtDate(mission.plannedStart)} – {fmtDate(mission.plannedEnd)}</TableCell>
            <TableCell>{mission.crewName || '—'}</TableCell>
            <TableCell>{mission.monitoringPriority ? mission.monitoringPriority : '—'}</TableCell>
            <TableCell><MissionRiskCell mission={mission} /></TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function MissionStatusCell({ mission }) {
  return (
    <Stack spacing={0.25}>
      <Chip size="small" color={statusColor(mission.status)} label={statusText(mission.status)} />
      <Typography variant="caption" color="text.secondary">{missionStatusHint(mission)}</Typography>
    </Stack>
  );
}

function MissionRiskCell({ mission }) {
  if (mission.riskReviewRequiredAt && !terminalMissionStatuses.has(mission.status)) {
    return (
      <Stack spacing={0.25}>
        <Chip size="small" color="warning" label="Пересчитать" />
        <Typography variant="caption" color="text.secondary">
          {mission.risk ? `Последний: ${mission.risk.riskScore}/100` : mission.riskReviewReason || 'Нет актуального snapshot'}
        </Typography>
      </Stack>
    );
  }
  if (!mission.risk) return '—';
  return (
    <Stack spacing={0.25}>
      <Typography fontWeight={700}>{mission.risk.riskScore}/100</Typography>
      <Typography variant="caption" color="text.secondary">{riskDecisionText(mission.risk.decisionZone)}</Typography>
    </Stack>
  );
}

function MissionCardRow({ label, children }) {
  return (
    <Stack className="mission-card-row" direction="row">
      <Typography color="text.secondary">{label}</Typography>
      <Typography className="mission-card-value" fontWeight={700}>{children}</Typography>
    </Stack>
  );
}

function MissionCard({ mission, compact, embedded = false }) {
  if (!mission) {
    return embedded
      ? <Typography color="text.secondary">Нет выбранного рейса</Typography>
      : <Panel title="Рейс"><Typography color="text.secondary">Нет выбранного рейса</Typography></Panel>;
  }
  const terminal = terminalMissionStatuses.has(mission.status);
  const content = (
      <Stack className={`mission-card-body ${compact ? 'mission-card-compact' : ''}`} spacing={compact ? 1 : 1.5}>
        <MissionCardRow label="Статус"><Chip size="small" color={statusColor(mission.status)} label={statusText(mission.status)} /></MissionCardRow>
        <MissionCardRow label="Состояние">{missionStatusHint(mission)}</MissionCardRow>
        <MissionCardRow label="Зона">{mission.zoneName || '—'}</MissionCardRow>
        <MissionCardRow label="Харвестер">{mission.harvesterName || '—'}</MissionCardRow>
        <MissionCardRow label="Экипаж">{mission.crewName || '—'}</MissionCardRow>
        <MissionCardRow label="P(attack)">{mission.risk ? `${fmtNumber(mission.risk.pAttack * 100, 1)}%` : '—'}</MissionCardRow>
        <MissionCardRow label="Risk-score">{mission.risk?.riskScore ?? '—'}{mission.risk ? ` · ${riskDecisionText(mission.risk.decisionZone)}` : ''}</MissionCardRow>
        <MissionCardRow label="Приоритет мониторинга">{mission.monitoringPriority || 0}</MissionCardRow>
        {mission.closedBy && <MissionCardRow label="Закрыл">{mission.closedBy}</MissionCardRow>}
        {mission.draftMissingFields && <Alert severity="warning">Не хватает данных: {mission.draftMissingFields}</Alert>}
        {mission.riskReviewRequiredAt && !terminal && <Alert severity="warning">Риск требует пересчёта: {mission.riskReviewReason || 'изменились факторы'}</Alert>}
        {mission.monitoringContext && <Alert severity={mission.monitoringPriority >= 90 ? 'error' : 'warning'}>{mission.monitoringContext}</Alert>}
        {mission.risk?.stale && !terminal && <Alert severity="warning">Оценка риска устарела</Alert>}
      </Stack>
  );
  if (embedded) {
    return (
      <Box className="mission-card-embedded">
        <Typography variant="h3" gutterBottom>HRV-{mission.id}</Typography>
        {content}
      </Box>
    );
  }
  return (
    <Panel title={`HRV-${mission.id}`}>
      {content}
    </Panel>
  );
}

function routePoints(mission) {
  return [...(mission?.route || [])]
    .filter((point) => Number.isFinite(Number(point.lat)) && Number.isFinite(Number(point.lon)))
    .sort((a, b) => Number(a.seqNo || 0) - Number(b.seqNo || 0));
}

function routeDistanceKm(points) {
  const earthRadiusKm = 6371;
  let total = 0;
  for (let index = 1; index < points.length; index += 1) {
    const previous = points[index - 1];
    const current = points[index];
    const lat1 = Number(previous.lat) * Math.PI / 180;
    const lat2 = Number(current.lat) * Math.PI / 180;
    const deltaLat = (Number(current.lat) - Number(previous.lat)) * Math.PI / 180;
    const deltaLon = (Number(current.lon) - Number(previous.lon)) * Math.PI / 180;
    const a = Math.sin(deltaLat / 2) ** 2
      + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) ** 2;
    total += 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
  return total;
}

function normalizeRoutePoints(points) {
  const padding = 16;
  const lats = points.map((point) => Number(point.lat));
  const lons = points.map((point) => Number(point.lon));
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLon = Math.min(...lons);
  const maxLon = Math.max(...lons);
  const latSpan = Math.max(maxLat - minLat, 0.0001);
  const lonSpan = Math.max(maxLon - minLon, 0.0001);
  const usable = 100 - padding * 2;

  return points.map((point, index) => ({
    ...point,
    index,
    x: padding + ((Number(point.lon) - minLon) / lonSpan) * usable,
    y: padding + ((maxLat - Number(point.lat)) / latSpan) * usable
  }));
}

function routePointTitle(index, total) {
  if (index === 0) return 'Старт';
  if (index === total - 1) return 'Финиш';
  return `Точка ${index + 1}`;
}

function routePointCountText(count) {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) return `${count} точка`;
  if ([2, 3, 4].includes(mod10) && ![12, 13, 14].includes(mod100)) return `${count} точки`;
  return `${count} точек`;
}

function routeCoordinate(point) {
  return `${fmtNumber(point.lat, 4)}, ${fmtNumber(point.lon, 4)}`;
}

function RouteMap({ mission }) {
  const points = routePoints(mission);
  if (!points.length) {
    return (
      <Box className="route-map route-map-empty">
        <Box className="route-empty-icon"><Route /></Box>
        <Typography fontWeight={700}>Маршрут не задан</Typography>
        <Typography color="text.secondary">Нет координат для отображения.</Typography>
      </Box>
    );
  }
  const normalized = normalizeRoutePoints(points);
  const pathPoints = normalized.map((point) => `${point.x},${point.y}`).join(' ');
  const distance = routeDistanceKm(points);
  return (
    <Box className="route-map">
      <Stack className="route-map-header" direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary">Маршрут рейса</Typography>
          <Typography className="route-map-title" fontWeight={800}>
            {mission?.zoneName || mission?.title || `HRV-${mission?.id}`}
          </Typography>
        </Box>
        <Stack className="route-map-meta" direction="row" spacing={1}>
          <Chip size="small" color={statusColor(mission?.status)} label={statusText(mission?.status)} />
          <Chip size="small" icon={<Route />} label={routePointCountText(points.length)} />
          <Chip size="small" icon={<Map />} label={`${fmtNumber(distance, distance >= 10 ? 0 : 1)} км`} />
        </Stack>
      </Stack>
      <Box className="route-map-canvas">
        <svg className="route-map-svg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          {normalized.length > 1 && (
            <>
              <polyline className="route-path-shadow" points={pathPoints} />
              <polyline className="route-path" points={pathPoints} />
            </>
          )}
        </svg>
        {normalized.map((point, index) => {
          const title = routePointTitle(index, normalized.length);
          return (
            <Tooltip key={`${point.seqNo}-${index}`} title={`${title}: ${routeCoordinate(point)}`} arrow>
              <Box
                className={`route-waypoint ${index === 0 ? 'route-waypoint-start' : ''} ${index === normalized.length - 1 ? 'route-waypoint-finish' : ''}`.trim()}
                sx={{ left: `${point.x}%`, top: `${point.y}%` }}
              >
                <Typography className="route-waypoint-index" component="span">{index + 1}</Typography>
                {(index === 0 || index === normalized.length - 1) && (
                  <Typography className="route-waypoint-tag" component="span">{title}</Typography>
                )}
              </Box>
            </Tooltip>
          );
        })}
      </Box>
      <Box className="route-point-list">
        {normalized.map((point, index) => (
          <Box key={`list-${point.seqNo}-${index}`} className="route-point-row">
            <Typography variant="caption" color="text.secondary">{routePointTitle(index, normalized.length)}</Typography>
            <Typography fontWeight={700}>#{point.seqNo || index + 1}</Typography>
            <Typography className="route-point-coordinates">{routeCoordinate(point)}</Typography>
          </Box>
        ))}
      </Box>
    </Box>
  );
}

function Kpi({ title, value, icon, tone = 'primary' }) {
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

function Panel({ title, children, className = '' }) {
  return (
    <Paper className={`panel ${className}`.trim()} elevation={0}>
      <Typography variant="h3" gutterBottom>{title}</Typography>
      {children}
    </Paper>
  );
}

function ConfirmDialog({ confirm, setConfirm }) {
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

function MessageSnackbar({ message, setMessage }) {
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

function defaultSection(userRoles) {
  const roleSet = new Set(userRoles || []);
  return sections.find(([, , , allowed]) => allowed.some((role) => roleSet.has(role)))?.[0] || 'overview';
}

function sectionTitle(value) {
  return sections.find(([key]) => key === value)?.[1] || 'HSMS';
}

function shortRole(role) {
  return {
    ROLE_SUPPLY_MANAGER: 'Диспетчер',
    ROLE_HARVESTER_CREW: 'Экипаж',
    ROLE_INSURANCE_CONTOUR_OPERATOR: 'Страхование',
    ROLE_SECURITY_HEADQUARTERS_OPERATOR: 'Штаб',
    ROLE_ADMINISTRATOR: 'Администратор',
    ROLE_OPERATIONS_MANAGEMENT: 'Руководство'
  }[role] || role;
}

function realtimeText(action) {
  return {
    MISSION_CREATED: 'Создан рейс',
    MISSION_UPDATED: 'Рейс обновлён',
    MISSION_LAUNCHED: 'Рейс запущен',
    MISSION_RISK_CANCELLED: 'Рейс отменён по риску',
    MISSION_PLAN_ACKNOWLEDGED: 'План рейса подтверждён экипажем',
    RISK_ASSESSED: 'Риск рассчитан',
    RISK_MARKED_STALE: 'Риск требует пересчёта',
    RISK_RECALCULATED_AFTER_CHANGE: 'Риск пересчитан после изменения условий',
    RISK_POLICY_UPDATED: 'Политика риска обновлена',
    TELEMETRY_RECEIVED: 'Поступила телеметрия',
    MISSION_MONITORING_PRIORITY_RAISED: 'Повышен приоритет мониторинга',
    ALARM_RECEIVED: 'Поступила тревога',
    INCIDENT_CREATED: 'Создан инцидент',
    INCIDENT_CLASSIFIED: 'Инцидент классифицирован',
    INCIDENT_SLA_BREACHED: 'НВР нарушен',
    EVACUATION_COMMAND_CREATED: 'Команда эвакуации создана',
    EVACUATION_COMMAND_SENT: 'Команда эвакуации отправлена',
    EVACUATION_DELIVERED: 'Команда доставлена экипажу',
    EVACUATION_DELIVERY_FAILED: 'Сбой доставки эвакуации',
    EVACUATION_ACKNOWLEDGED: 'Эвакуация подтверждена',
    EVACUATION_ACK_EXPIRED: 'Подтверждение эвакуации просрочено',
    INCIDENT_CLOSED: 'Инцидент закрыт',
    MISSION_REPORT_SUBMITTED: 'Итоговый отчёт принят',
    MISSION_CLOSED: 'Рейс закрыт',
    INSURANCE_CASE_OPENED: 'Страховой кейс открыт',
    INSURANCE_CASE_WAITING_FOR_DATA: 'Кейс ожидает данные',
    INSURANCE_RECALCULATED: 'Страховой кейс пересчитан',
    INSURANCE_TERMS_UPDATED: 'Условия страхования обновлены',
    INSURANCE_RECALCULATION_REJECTED: 'Перерасчёт страхования отклонён',
    INSURANCE_CASE_CLOSED: 'Страховой кейс закрыт',
    USER_CREATED: 'Создан пользователь',
    USER_ROLES_UPDATED: 'Роли пользователя обновлены',
    HARVESTER_CREATED: 'Создан харвестер',
    CREW_CREATED: 'Создан экипаж'
  }[action] || 'Событие HSMS';
}

function realtimeSeverity(action) {
  return criticalRealtimeActions.has(action) ? 'error' : 'info';
}

function timeLeft(value, now = Date.now()) {
  if (!value) return '—';
  const seconds = Math.max(0, Math.floor((new Date(value).getTime() - now) / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

const rootElement = document.getElementById('root');
rootElement._hsmsRoot ||= createRoot(rootElement);
rootElement._hsmsRoot.render(<App />);
