import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { login as loginRequest, logout as logoutRequest, LoginRequest, LoginResponse } from '../api/auth';
import { TOKEN_STORAGE_KEY } from '../api/axiosClient';

const getInitialToken = (): string | null => {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
};

interface UseAuthResult {
  token: string | null;
  isAuthenticated: boolean;
  isLoggingIn: boolean;
  loginError: unknown;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => void;
}

export const useAuth = (): UseAuthResult => {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string | null>(getInitialToken);

  useEffect(() => {
    const handler = (event: StorageEvent) => {
      if (event.key === TOKEN_STORAGE_KEY) {
        setToken(event.newValue);
      }
    };

    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) => loginRequest(credentials),
    onSuccess: (data) => {
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(TOKEN_STORAGE_KEY, data.accessToken);
      }
      setToken(data.accessToken);
    },
  });

  const logout = useCallback(() => {
    logoutRequest();
    setToken(null);
    queryClient.clear();
  }, [queryClient]);

  const login = useCallback(
    (credentials: LoginRequest) => loginMutation.mutateAsync(credentials),
    [loginMutation]
  );

  return useMemo(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      isLoggingIn: loginMutation.isPending,
      loginError: loginMutation.error,
      login,
      logout,
    }),
    [login, loginMutation.error, loginMutation.isPending, logout, token]
  );
};
