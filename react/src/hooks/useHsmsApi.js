import { useCallback } from 'react';
import { API_BASE } from '../shared/constants';

async function parseErrorResponse(response) {
  return response.json().catch(() => ({}));
}

export function useHsmsApi(token) {
  return useCallback(async (apiPath, options = {}) => {
    let response;
    try {
      response = await fetch(`${API_BASE}${apiPath}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...(options.headers || {})
        }
      });
    } catch (error) {
      const networkError = new Error(error.message || 'Сервер недоступен', { cause: error });
      networkError.status = 0;
      networkError.path = apiPath;
      throw networkError;
    }

    if (!response.ok) {
      const error = await parseErrorResponse(response);
      const apiError = new Error(error.action ? `${error.message}. ${error.action}` : error.message || `Ошибка API ${response.status}`);
      apiError.status = response.status;
      apiError.path = apiPath;
      throw apiError;
    }

    if (response.headers.get('content-type')?.includes('text/csv')) {
      return response.text();
    }
    return response.json();
  }, [token]);
}
