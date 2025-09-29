import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import {
  completeStripeConnect,
  LoginResponse,
  logout as logoutRequest,
  startStripeConnect,
  StripeConnectCallbackRequest,
  StripeConnectStartRequest,
  StripeConnectStartResponse,
} from '../api/auth';
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
  isInitiating: boolean;
  isCompleting: boolean;
  initiateError: unknown;
  completeError: unknown;
  initiateConnect: (payload: StripeConnectStartRequest) => Promise<StripeConnectStartResponse>;
  completeConnect: (payload: StripeConnectCallbackRequest) => Promise<LoginResponse>;
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

  const initiateMutation = useMutation({
    mutationFn: (payload: StripeConnectStartRequest) => startStripeConnect(payload),
  });

  const completeMutation = useMutation({
    mutationFn: (payload: StripeConnectCallbackRequest) => completeStripeConnect(payload),
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

  const initiateConnect = useCallback(
    (payload: StripeConnectStartRequest) => initiateMutation.mutateAsync(payload),
    [initiateMutation]
  );

  const completeConnect = useCallback(
    (payload: StripeConnectCallbackRequest) => completeMutation.mutateAsync(payload),
    [completeMutation]
  );

  return useMemo(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      isInitiating: initiateMutation.isPending,
      isCompleting: completeMutation.isPending,
      initiateError: initiateMutation.error,
      completeError: completeMutation.error,
      initiateConnect,
      completeConnect,
      logout,
    }),
    [
      completeConnect,
      completeMutation.error,
      completeMutation.isPending,
      initiateConnect,
      initiateMutation.error,
      initiateMutation.isPending,
      logout,
      token,
    ]
  );
};
