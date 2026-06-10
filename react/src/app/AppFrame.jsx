import {
  AppBar,
  Badge,
  Box,
  Button,
  Chip,
  Drawer,
  FormControl,
  IconButton,
  InputLabel,
  LinearProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  Toolbar,
  Tooltip,
  Typography
} from '@mui/material';
import { Cached, CellTower, Logout, NotificationsActive, Security, Shield } from '@mui/icons-material';
import { sectionTitle, shortRole } from './navigation';
import { statusText } from '../shared/status';

function streamStateLabel(streamState) {
  if (streamState === 'online') return 'События онлайн';
  if (streamState === 'degraded') return 'События нестабильны';
  return 'События офлайн';
}

export function AppFrame({
  busy,
  children,
  dashboard,
  isAdmin,
  orderedMissions,
  refresh,
  section,
  selectedMission,
  sessionUser,
  setSection,
  setSelectedMissionId,
  signOut,
  streamState,
  telemetryQueueLength,
  visibleSections
}) {
  const streamLabel = streamStateLabel(streamState);
  const streamColor = streamState === 'online' ? 'success' : 'warning';

  return (
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
            <Chip size="small" icon={<NotificationsActive />} label={streamLabel} color={streamColor} />
            <Chip size="small" icon={<CellTower />} label={`Буфер: ${telemetryQueueLength}`} color={telemetryQueueLength ? 'warning' : 'default'} />
          </Stack>
        </Stack>
      </Drawer>

      <Box className="main-shell">
        <AppBar position="sticky" color="inherit" elevation={0} className="topbar">
          <Toolbar>
            <Box sx={{ flex: 1 }}>
              <Typography variant="h1">{sectionTitle(section)}</Typography>
              <Typography color="text.secondary">{sessionUser.displayName}</Typography>
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
              <Chip icon={<Security />} label={sessionUser.roles.map(shortRole).join(', ')} />
              <Button variant="outlined" startIcon={<Logout />} onClick={signOut}>Выйти</Button>
            </Stack>
          </Toolbar>
          {busy && <LinearProgress />}
        </AppBar>

        <Box className="content">
          {children}
        </Box>
      </Box>
    </Box>
  );
}
