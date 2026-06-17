import { useEffect, useState } from 'react';

export function useMissionTimeline({ api, missionId, refreshKey }) {
  const [timeline, setTimeline] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!missionId) {
      setTimeline(null);
      setLoading(false);
      setError(null);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    api(`/missions/${missionId}/timeline`)
      .then((result) => {
        if (!cancelled) setTimeline(result);
      })
      .catch((nextError) => {
        if (!cancelled) {
          setTimeline(null);
          setError(nextError);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [api, missionId, refreshKey]);

  return { error, loading, timeline };
}
