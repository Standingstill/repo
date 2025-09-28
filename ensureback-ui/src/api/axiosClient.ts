import axios from 'axios';

export const API_BASE_URL = '/api';
export const TOKEN_STORAGE_KEY = 'ensureback_token';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: false
});

axiosClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = window.localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
