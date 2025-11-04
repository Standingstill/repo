import axios from "axios";

export const API_BASE_URL = '/api';
export const TOKEN_STORAGE_KEY = 'ensureback_token';

export const buildAuthorizationHeader = (token: string | null | undefined): string | null => {
  if (!token) {
    return null;
  }

  const normalized = token.trim();
  if (!normalized) {
    return null;
  }

  if (normalized.toLowerCase().startsWith('bearer ')) {
    return normalized;
  }

  return `Bearer ${normalized}`;
};

export const readStoredToken = (): string | null => {
  if (typeof window === 'undefined') {
    return null;
  }

  const fromLocal = window.localStorage.getItem(TOKEN_STORAGE_KEY);
  if (fromLocal) {
    return fromLocal;
  }

  return window.sessionStorage.getItem(TOKEN_STORAGE_KEY);
};

export const persistToken = (token: string): void => {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } catch (error) {
    console.warn('Unable to persist token to localStorage', error);
  }

  try {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
  } catch (error) {
    console.warn('Unable to persist token to sessionStorage', error);
  }
};

export const clearStoredToken = (): void => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  window.sessionStorage.removeItem(TOKEN_STORAGE_KEY);
};

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

const readCookieToken = (): string | null => {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(/(?:^|; )EB_AUTH=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
};

axiosClient.interceptors.request.use((config) => {
  let token = readStoredToken();
  let authorization = buildAuthorizationHeader(token);
  if (!authorization) {
    token = readCookieToken();
    authorization = buildAuthorizationHeader(token);
  }
  if (authorization) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = authorization;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredToken();
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
