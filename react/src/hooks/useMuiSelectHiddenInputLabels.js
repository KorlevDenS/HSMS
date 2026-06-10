import { useLayoutEffect } from 'react';

export function useMuiSelectHiddenInputLabels() {
  useLayoutEffect(() => {
    const labelHiddenInputs = () => {
      document.querySelectorAll('.MuiSelect-nativeInput[aria-hidden="true"]').forEach((input) => {
        const display = input.parentElement?.querySelector('[role="combobox"]');
        if (!display) return;

        const labelledBy = display.getAttribute('aria-labelledby');
        if (labelledBy) {
          input.setAttribute('aria-labelledby', labelledBy);
          return;
        }

        const label = display.getAttribute('aria-label');
        if (label) {
          input.setAttribute('aria-label', label);
        }
      });
    };

    labelHiddenInputs();
    const observer = new MutationObserver(labelHiddenInputs);
    observer.observe(document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);
}
