import {
  AdminPanelSettings,
  Assessment,
  CellTower,
  LocalPolice,
  Policy,
  ReceiptLong,
  Route
} from '@mui/icons-material';

export const roles = [
  ['dispatcher', 'Диспетчер снабжения', 'ROLE_SUPPLY_MANAGER'],
  ['crew1', 'Экипаж №1 Альфа', 'ROLE_HARVESTER_CREW'],
  ['crew2', 'Экипаж №2 Бетта', 'ROLE_HARVESTER_CREW'],
  ['crew3', 'Экипаж №3 Гамма', 'ROLE_HARVESTER_CREW'],
  ['security', 'Оператор штаба', 'ROLE_SECURITY_HEADQUARTERS_OPERATOR'],
  ['insurance', 'Оператор страхового контура', 'ROLE_INSURANCE_CONTOUR_OPERATOR'],
  ['management', 'Руководство операций', 'ROLE_OPERATIONS_MANAGEMENT'],
  ['admin', 'Администратор', 'ROLE_ADMINISTRATOR']
];

export const roleOptions = [
  'ROLE_SUPPLY_MANAGER',
  'ROLE_HARVESTER_CREW',
  'ROLE_SECURITY_HEADQUARTERS_OPERATOR',
  'ROLE_INSURANCE_CONTOUR_OPERATOR',
  'ROLE_OPERATIONS_MANAGEMENT',
  'ROLE_ADMINISTRATOR'
];

export const emptyNewUser = {
  login: '',
  password: '',
  displayName: '',
  email: '',
  phone: '',
  roles: ['ROLE_OPERATIONS_MANAGEMENT']
};

export const sections = [
  ['overview', 'Обзор', Assessment, ['ROLE_SUPPLY_MANAGER', 'ROLE_OPERATIONS_MANAGEMENT', 'ROLE_ADMINISTRATOR']],
  ['missions', 'Рейсы', Route, ['ROLE_SUPPLY_MANAGER', 'ROLE_ADMINISTRATOR']],
  ['crew', 'Экипаж', CellTower, ['ROLE_HARVESTER_CREW', 'ROLE_ADMINISTRATOR']],
  ['security', 'НВР', LocalPolice, ['ROLE_SECURITY_HEADQUARTERS_OPERATOR', 'ROLE_ADMINISTRATOR']],
  ['insurance', 'Страхование', Policy, ['ROLE_INSURANCE_CONTOUR_OPERATOR', 'ROLE_ADMINISTRATOR']],
  ['reports', 'Отчёты', ReceiptLong, ['ROLE_OPERATIONS_MANAGEMENT', 'ROLE_ADMINISTRATOR']],
  ['admin', 'Админ', AdminPanelSettings, ['ROLE_ADMINISTRATOR']]
];

export function defaultSection(userRoles) {
  const roleSet = new Set(userRoles || []);
  return sections.find(([, , , allowed]) => allowed.some((role) => roleSet.has(role)))?.[0] || 'overview';
}

export function sectionTitle(value) {
  return sections.find(([key]) => key === value)?.[1] || 'HSMS';
}

export function shortRole(role) {
  return {
    ROLE_SUPPLY_MANAGER: 'Диспетчер',
    ROLE_HARVESTER_CREW: 'Экипаж',
    ROLE_INSURANCE_CONTOUR_OPERATOR: 'Страхование',
    ROLE_SECURITY_HEADQUARTERS_OPERATOR: 'Штаб',
    ROLE_ADMINISTRATOR: 'Администратор',
    ROLE_OPERATIONS_MANAGEMENT: 'Руководство'
  }[role] || role;
}
