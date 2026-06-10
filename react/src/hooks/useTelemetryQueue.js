import { useCallback, useEffect, useState } from 'react';
import { loadTelemetryQueue, saveTelemetryQueue } from '../shared/storage';

const FLUSH_INTERVAL_MS = 8000;

export function useTelemetryQueue({ api, refresh, sessionToken }) {
  const [telemetryQueue, setTelemetryQueue] = useState(loadTelemetryQueue);

  const enqueueTelemetry = useCallback((missionId, payload) => {
    setTelemetryQueue((current) => {
      const nextQueue = [...current, { missionId, payload }];
      saveTelemetryQueue(nextQueue);
      return nextQueue;
    });
  }, []);

  const flushTelemetryQueue = useCallback(async () => {
    if (!sessionToken || telemetryQueue.length === 0) return;

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
  }, [api, refresh, sessionToken, telemetryQueue]);

  useEffect(() => {
    const onOnline = () => { flushTelemetryQueue().catch(() => undefined); };
    window.addEventListener('online', onOnline);
    const interval = window.setInterval(() => { flushTelemetryQueue().catch(() => undefined); }, FLUSH_INTERVAL_MS);
    return () => {
      window.removeEventListener('online', onOnline);
      window.clearInterval(interval);
    };
  }, [flushTelemetryQueue]);

  return { enqueueTelemetry, telemetryQueue };
}
