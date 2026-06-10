import { Box, Chip, Stack, Tooltip, Typography } from '@mui/material';
import { Map, Route } from '@mui/icons-material';
import { fmtNumber } from '../../shared/formatters';
import { statusColor, statusText } from '../../shared/status';

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

export function RouteMap({ mission }) {
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
