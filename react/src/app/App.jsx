import { useCallback, useMemo, useState } from 'react';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { API_BASE } from '../shared/constants';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { MessageSnackbar } from '../shared/components/MessageSnackbar';
import { useHsmsApi } from '../hooks/useHsmsApi';
import { useHsmsBootstrap } from '../hooks/useHsmsBootstrap';
import { useIncidentStream } from '../hooks/useIncidentStream';
import { useMuiSelectHiddenInputLabels } from '../hooks/useMuiSelectHiddenInputLabels';
import { useTelemetryQueue } from '../hooks/useTelemetryQueue';
import { clearSession, loadSession, saveSession } from '../shared/storage';
import { isTransientApiError } from '../shared/realtime';
import { defaultSection, sections } from './navigation';
import { theme } from './theme';
import { AppFrame } from './AppFrame';
import { LoginScreen } from '../features/auth/LoginScreen';
import { AdminView } from '../features/admin/AdminView';
import { CrewView } from '../features/crew/CrewView';
import { InsuranceView } from '../features/insurance/InsuranceView';
import { MissionsView } from '../features/missions/MissionsView';
import { Overview } from '../features/overview/Overview';
import { ReportsView } from '../features/reports/ReportsView';
import { SecurityView } from '../features/security/SecurityView';

export function App() {
  useMuiSelectHiddenInputLabels();

  const [session, setSession] = useState(loadSession);
  const [loginName, setLoginName] = useState(() => session?.user?.login || 'dispatcher');
  const [password, setPassword] = useState('');
  const [section, setSection] = useState(() => defaultSection(session?.user?.roles));
  const [selectedIncidentId, setSelectedIncidentId] = useState(null);
  const [selectedCaseId, setSelectedCaseId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const notify = useCallback((severity, text) => {
    setMessage({ severity, text });
  }, []);
  const api = useHsmsApi(session?.token);
  const {
    data,
    dataVersion,
    refresh,
    reset,
    selectedMissionId,
    setSelectedMissionId,
    setStreamState,
    streamState
  } = useHsmsBootstrap({ api, sessionToken: session?.token, notify });
  const { enqueueTelemetry, telemetryQueue } = useTelemetryQueue({ api, refresh, sessionToken: session?.token });

  useIncidentStream({
    notify,
    refresh,
    sessionToken: session?.token,
    setStreamState
  });

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
      saveSession(nextSession);
      setSection(defaultSection(nextSession.user.roles));
      notify('success', 'Вход выполнен');
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy(false);
    }
  }

  function signOut() {
    setSession(null);
    reset();
    clearSession();
  }

  async function run(label, operation) {
    setBusy(true);
    try {
      const result = await operation();
      try {
        await refresh(label);
      } catch (error) {
        setStreamState('degraded');
        notify(
          'warning',
          isTransientApiError(error)
            ? 'Операция выполнена. Данные обновятся автоматически после восстановления связи.'
            : error.message
        );
      }
      return result;
    } catch (error) {
      notify('error', error.message);
      return null;
    } finally {
      setBusy(false);
    }
  }

  function ask(title, body, action, danger = false) {
    setConfirm({ title, body, action, danger });
  }

  const dashboard = data?.dashboard || {};
  const isAdmin = Boolean(session?.user?.roles.includes('ROLE_ADMINISTRATOR'));

  if (!session?.token) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <LoginScreen
          busy={busy}
          loginName={loginName}
          message={message}
          password={password}
          setLoginName={setLoginName}
          setMessage={setMessage}
          setPassword={setPassword}
          signIn={signIn}
        />
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AppFrame
        busy={busy}
        dashboard={dashboard}
        isAdmin={isAdmin}
        orderedMissions={orderedMissions}
        refresh={refresh}
        section={section}
        selectedMission={selectedMission}
        sessionUser={session.user}
        setSection={setSection}
        setSelectedMissionId={setSelectedMissionId}
        signOut={signOut}
        streamState={streamState}
        telemetryQueueLength={telemetryQueue.length}
        visibleSections={visibleSections}
      >
        {section === 'overview' && <Overview dashboard={dashboard} missions={orderedMissions} incidents={data?.incidents || []} />}
        {section === 'missions' && <MissionsView api={api} run={run} ask={ask} dataVersion={dataVersion} missions={orderedMissions} selectedMission={selectedMission} setSelectedMissionId={setSelectedMissionId} zones={data?.zones || []} harvesters={data?.harvesters || []} crews={data?.crews || []} />}
        {section === 'crew' && <CrewView api={api} run={run} missions={orderedMissions} selectedMission={selectedMission} setSelectedMissionId={setSelectedMissionId} canSwitchMission={isAdmin} incidents={data?.incidents || []} telemetryQueue={telemetryQueue} enqueueTelemetry={enqueueTelemetry} />}
        {section === 'security' && <SecurityView run={run} ask={ask} dataVersion={dataVersion} incidents={data?.incidents || []} missions={orderedMissions} selectedIncident={selectedIncident} setSelectedIncidentId={setSelectedIncidentId} api={api} />}
        {section === 'insurance' && <InsuranceView run={run} ask={ask} cases={data?.insuranceCases || []} selectedCase={selectedCase} setSelectedCaseId={setSelectedCaseId} api={api} />}
        {section === 'reports' && <ReportsView api={api} run={run} dashboard={dashboard} />}
        {section === 'admin' && <AdminView api={api} run={run} users={data?.users || []} policy={data?.activeRiskPolicy} audit={data?.audit || []} missions={orderedMissions} incidents={data?.incidents || []} cases={data?.insuranceCases || []} />}
      </AppFrame>
      <ConfirmDialog confirm={confirm} setConfirm={setConfirm} />
      <MessageSnackbar message={message} setMessage={setMessage} />
    </ThemeProvider>
  );
}
