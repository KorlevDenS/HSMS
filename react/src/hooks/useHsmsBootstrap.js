import { useCallback, useEffect, useState } from 'react';
import { isTransientApiError, sleep } from '../shared/realtime';

const MAX_BOOTSTRAP_ATTEMPTS = 5;
const BASE_RETRY_DELAY_MS = 700;
const MAX_RETRY_DELAY_MS = 8000;

export function useHsmsBootstrap({ api, sessionToken, notify }) {
  const [data, setData] = useState(null);
  const [dataVersion, setDataVersion] = useState(0);
  const [streamState, setStreamState] = useState('offline');
  const [selectedMissionId, setSelectedMissionId] = useState(null);

  const refresh = useCallback(async (nextMessage) => {
    if (!sessionToken) return null;
    let lastError = null;

    for (let attempt = 0; attempt < MAX_BOOTSTRAP_ATTEMPTS; attempt += 1) {
      try {
        const bootstrap = await api('/bootstrap');
        setData(bootstrap);
        setDataVersion((current) => current + 1);
        setStreamState((current) => current === 'offline' ? 'online' : current);

        if (bootstrap.missions?.length) {
          const latestMissionId = [...bootstrap.missions].sort((a, b) => b.id - a.id)[0].id;
          setSelectedMissionId((current) => current || latestMissionId);
        }

        if (nextMessage) notify('success', nextMessage);
        return bootstrap;
      } catch (error) {
        lastError = error;
        if (!isTransientApiError(error) || attempt === MAX_BOOTSTRAP_ATTEMPTS - 1) break;
        setStreamState('degraded');
        await sleep(Math.min(MAX_RETRY_DELAY_MS, BASE_RETRY_DELAY_MS * 2 ** attempt));
      }
    }

    throw lastError;
  }, [api, notify, sessionToken]);

  useEffect(() => {
    if (!sessionToken) return;
    refresh().catch((error) => {
      notify(
        isTransientApiError(error) ? 'warning' : 'error',
        isTransientApiError(error)
          ? 'Сервер временно недоступен. Данные обновятся автоматически.'
          : error.message
      );
    });
  }, [notify, refresh, sessionToken]);

  const reset = useCallback(() => {
    setData(null);
    setDataVersion(0);
    setStreamState('offline');
    setSelectedMissionId(null);
  }, []);

  return {
    data,
    dataVersion,
    refresh,
    reset,
    selectedMissionId,
    setSelectedMissionId,
    setStreamState,
    streamState
  };
}
