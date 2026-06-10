import { useId } from 'react';
import { Checkbox, FormControl, InputLabel, ListItemText, MenuItem, Select } from '@mui/material';
import { roleOptions, shortRole } from '../../app/navigation';

export function RoleSelect({ value, onChange, compact }) {
  const id = useId();
  const labelId = `roles-label-${id}`;
  const selectId = `roles-select-${id}`;
  const nativeInputId = `roles-native-${id}`;
  return (
    <FormControl fullWidth={!compact} sx={compact ? { minWidth: 260 } : undefined}>
      <InputLabel id={labelId} htmlFor={nativeInputId}>Роли</InputLabel>
      <Select
        id={selectId}
        labelId={labelId}
        name="roles"
        multiple
        label="Роли"
        value={value || []}
        inputProps={{ id: nativeInputId, 'aria-label': 'Роли' }}
        onChange={(event) => {
          const nextValue = event.target.value;
          onChange(typeof nextValue === 'string' ? nextValue.split(',') : nextValue);
        }}
        renderValue={(selected) => selected.map(shortRole).join(', ')}
      >
        {roleOptions.map((role) => (
          <MenuItem key={role} value={role}>
            <Checkbox checked={(value || []).includes(role)} />
            <ListItemText primary={shortRole(role)} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
