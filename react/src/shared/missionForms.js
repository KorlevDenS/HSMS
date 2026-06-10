export function parseRouteText(value) {
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

export function routeTextFromMission(mission) {
  return (mission?.route || [])
    .map((point) => `${point.lat}, ${point.lon}`)
    .join('\n');
}

export function blankToNull(value) {
  return value === '' || value === null || value === undefined ? null : value;
}

export function numberOrNull(value) {
  const normalized = blankToNull(value);
  return normalized === null ? null : Number(normalized);
}
