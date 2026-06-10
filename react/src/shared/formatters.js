export function fmtDate(value) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

export function fmtNumber(value, digits = 0) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—';
  return new Intl.NumberFormat('ru-RU', { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(Number(value));
}

export function nowPlus(hours) {
  return new Date(Date.now() + hours * 60 * 60 * 1000).toISOString();
}

export function datetimeLocal(value) {
  if (!value) return '';
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function isoFromLocal(value) {
  return value ? new Date(value).toISOString() : null;
}

export function detailsText(details) {
  if (!details || Object.keys(details).length === 0) return '—';
  return Object.entries(details)
    .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : String(value)}`)
    .join('; ');
}

export function timeLeft(value, now = Date.now()) {
  if (!value) return '—';
  const seconds = Math.max(0, Math.floor((new Date(value).getTime() - now) / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}
