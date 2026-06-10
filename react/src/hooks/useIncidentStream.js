import { useEffect } from 'react';
import { API_BASE } from '../shared/constants';
import { realtimeSeverity, realtimeText } from '../shared/realtime';

const MAX_RECONNECT_DELAY_MS = 30000;
const BASE_RECONNECT_DELAY_MS = 1000;

function parseEventBlock(block) {
  const lines = block.split('\n');
  const event = lines.find((line) => line.startsWith('event:'))?.slice(6).trim();
  const dataLine = lines.find((line) => line.startsWith('data:'))?.slice(5).trim();
  if (!dataLine) return null;
  const payload = JSON.parse(dataLine);
  return { action: payload.action || event };
}

export function useIncidentStream({ sessionToken, refresh, notify, setStreamState }) {
  useEffect(() => {
    if (!sessionToken) return undefined;

    const controller = new AbortController();
    let reconnectTimer = null;
    let reconnectAttempt = 0;
    let buffer = '';

    const handleBlock = (block) => {
      const parsed = parseEventBlock(block);
      if (!parsed) return;
      setStreamState('online');
      if (parsed.action === 'CONNECTED') return;
      notify(realtimeSeverity(parsed.action), realtimeText(parsed.action));
      refresh().catch(() => undefined);
    };

    async function connect() {
      try {
        buffer = '';
        const response = await fetch(`${API_BASE}/incidents/stream`, {
          headers: { Authorization: `Bearer ${sessionToken}` },
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
          for (const block of blocks) {
            try {
              handleBlock(block);
            } catch {
              setStreamState('degraded');
            }
          }
        }

        if (!controller.signal.aborted) scheduleReconnect();
      } catch {
        if (!controller.signal.aborted) scheduleReconnect();
      }
    }

    function scheduleReconnect() {
      setStreamState('degraded');
      reconnectAttempt += 1;
      const delay = Math.min(MAX_RECONNECT_DELAY_MS, BASE_RECONNECT_DELAY_MS * 2 ** Math.min(reconnectAttempt, 5));
      reconnectTimer = window.setTimeout(connect, delay);
    }

    connect();
    return () => {
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      controller.abort();
    };
  }, [notify, refresh, sessionToken, setStreamState]);
}
