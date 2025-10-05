import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';

import { clearSession } from '../api/auth';
import axiosClient, { TOKEN_STORAGE_KEY, clearStoredToken, persistToken, readStoredToken } from '../api/axiosClient';

interface SessionState {
  token: string;
  role: string;
  stripeAccountId: string;
  expiresAt?: number;
}

interface StripeOnboardResponse {
  redirectUrl: string;
  alreadyConnected: boolean;
}

interface MerchantStatusResponse {
  isIntegrated: boolean;
  stripeAccountId?: string | null;
}

interface MerchantStatusErrorState {
  message: string;
  status?: number;
  type: 'auth' | 'network' | 'unknown';
}

const decodeJwt = (token: string): Record<string, unknown> | null => {
  try {
    const [, payload] = token.split('.');
    if (!payload) {
      return null;
    }
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(normalized);
    const json = decodeURIComponent(
      decoded
        .split('')
        .map((char) => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
        .join('')
    );
    return JSON.parse(json) as Record<string, unknown>;
  } catch (error) {
    console.warn('Unable to decode JWT payload', error);
    return null;
  }
};

const toSessionState = (token: string | null): SessionState | null => {
  if (!token) {
    return null;
  }
  const payload = decodeJwt(token);
  if (!payload) {
    return null;
  }
  const role = typeof payload.role === 'string' ? payload.role : '';
  const stripeAccountId = typeof payload.stripe_account_id === 'string' ? payload.stripe_account_id : '';
  const expiresAt = typeof payload.exp === 'number' ? payload.exp * 1000 : undefined;
  if (!role || !stripeAccountId) {
    return null;
  }
  return {
    token,
    role,
    stripeAccountId,
    expiresAt,
  };
};

const getInitialSession = (): SessionState | null => {
  const stored = readStoredToken();
  return toSessionState(stored);
};

interface UseAuthResult {
  token: string | null;
  role: string | null;
  stripeAccountId: string | null;
  isAuthenticated: boolean;
  isInitiating: boolean;
  initiateConnect: (returnPath?: string) => Promise<void>;
  logout: () => void;
  merchantStatus: MerchantStatusResponse | null;
  isMerchantStatusLoading: boolean;
  merchantStatusError: MerchantStatusErrorState | null;
  refreshMerchantStatus: (options?: { bypassManual?: boolean }) => Promise<MerchantStatusResponse | null>;
  setSessionFromToken: (token: string) => void;
  setMerchantStatusManually: (status: MerchantStatusResponse | null) => void;
}

export const useAuth = (): UseAuthResult => {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<SessionState | null>(getInitialSession);
  const sessionRef = useRef<SessionState | null>(session);
  const manualMerchantStatusRef = useRef(false);
  const [isInitiating, setIsInitiating] = useState(false);
  const [merchantStatus, setMerchantStatus] = useState<MerchantStatusResponse | null>(null);
  const [isMerchantStatusLoading, setIsMerchantStatusLoading] = useState(false);
  const [merchantStatusError, setMerchantStatusError] = useState<MerchantStatusErrorState | null>(null);

  useEffect(() => {
    sessionRef.current = session;
  }, [session]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const url = new URL(window.location.href);
    const paramsToCapture = ['token', 'connected', 'stripeAccountId', 'stripe_error', 'stripe_error_description'];
    let shouldReplace = false;

    const tokenParam = url.searchParams.get('token');
    if (tokenParam) {
      persistToken(tokenParam);
      const nextSession = toSessionState(tokenParam);

      if (nextSession) {
        setSession(nextSession);
        sessionRef.current = nextSession;
        manualMerchantStatusRef.current = false;
        setMerchantStatusError(null);
      } else {
        clearStoredToken();
        setSession(null);
        sessionRef.current = null;
      }

      shouldReplace = true;
    }

    paramsToCapture.forEach((param) => {
      if (url.searchParams.has(param)) {
        url.searchParams.delete(param);
        shouldReplace = true;
      }
    });

    if (shouldReplace) {
      const nextUrl = `${url.pathname}${url.search ? `?${url.searchParams.toString()}` : ''}${url.hash}`;
      window.history.replaceState({}, '', nextUrl || '/');
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return undefined;
    }
    const handler = (event: StorageEvent) => {
      if (event.key === TOKEN_STORAGE_KEY) {
        const nextToken = readStoredToken();
        const nextSession = toSessionState(nextToken);
        setSession(nextSession);
        sessionRef.current = nextSession;
        manualMerchantStatusRef.current = false;
        setMerchantStatusError(null);
      }
    };

    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const setSessionFromToken = useCallback((token: string) => {
    if (typeof window === 'undefined') {
      return;
    }

    if (!token) {
      clearStoredToken();
      setSession(null);
      sessionRef.current = null;
      manualMerchantStatusRef.current = false;
      setMerchantStatusError(null);
      return;
    }

    persistToken(token);
    const nextSession = toSessionState(token);
    if (!nextSession) {
      clearStoredToken();
      setSession(null);
      sessionRef.current = null;
      manualMerchantStatusRef.current = false;
      setMerchantStatusError(null);
      return;
    }

    setSession(nextSession);
    sessionRef.current = nextSession;
    manualMerchantStatusRef.current = false;
    setMerchantStatusError(null);
  }, []);

  const setMerchantStatusManually = useCallback((status: MerchantStatusResponse | null) => {
    manualMerchantStatusRef.current = Boolean(status);
    setMerchantStatus(status);
    setMerchantStatusError(null);
    setIsMerchantStatusLoading(false);
  }, []);

  const refreshMerchantStatus = useCallback(
    async (options?: { bypassManual?: boolean }): Promise<MerchantStatusResponse | null> => {
      const currentSession = sessionRef.current;
      const bypassManual = options?.bypassManual ?? false;

      if (!currentSession?.token || currentSession.role !== 'MERCHANT') {
        manualMerchantStatusRef.current = false;
        setMerchantStatus(null);
        setMerchantStatusError(null);
        setIsMerchantStatusLoading(false);
        return null;
      }

      if (bypassManual) {
        manualMerchantStatusRef.current = false;
        setMerchantStatusError(null);
      } else if (manualMerchantStatusRef.current) {
        return merchantStatus;
      }

      setIsMerchantStatusLoading(true);
      setMerchantStatusError(null);

      try {
        const response = await axiosClient.get<MerchantStatusResponse>('/merchant/status');
        manualMerchantStatusRef.current = false;
        setMerchantStatus(response.data);
        setMerchantStatusError(null);
        return response.data;
      } catch (error) {
        console.error('Unable to load merchant status', error);
        manualMerchantStatusRef.current = true;

        if (isAxiosError(error)) {
          const status = error.response?.status;
          if (status === 401 || status === 403) {
            setMerchantStatusError({ message: 'Session expired, please reconnect Stripe.', status, type: 'auth' });
          } else if (!error.response) {
            setMerchantStatusError({ message: 'We are having trouble reaching EnsureBack. Check your connection and try again.', type: 'network' });
          } else {
            setMerchantStatusError({ message: 'Unable to load merchant status. Please try again later.', status, type: 'unknown' });
          }
        } else {
          setMerchantStatusError({ message: 'Unable to load merchant status. Please try again later.', type: 'unknown' });
        }

        setMerchantStatus(null);
        return null;
      } finally {
        setIsMerchantStatusLoading(false);
      }
    },
    [merchantStatus]
  );

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
    sessionRef.current = null;
    manualMerchantStatusRef.current = false;
    setMerchantStatus(null);
    setMerchantStatusError(null);
    setIsMerchantStatusLoading(false);
    queryClient.clear();
  }, [queryClient]);

  const initiateConnect = useCallback(async (returnPath = '/dashboard') => {
    if (typeof window === 'undefined') {
      return;
    }
    setIsInitiating(true);
    try {
      const response = await axiosClient.get<StripeOnboardResponse>('/stripe/onboard', {
        params: { returnPath },
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
      });
      const redirectUrl = response.data?.redirectUrl;
      if (redirectUrl) {
        window.location.href = redirectUrl;
      } else {
        throw new Error('Stripe Connect onboarding did not provide a redirect URL');
      }
    } catch (error) {
      console.error('Unable to initiate Stripe Connect onboarding', error);
      throw error;
    } finally {
      setIsInitiating(false);
    }
  }, []);

  useEffect(() => {
    if (session?.token && session.role === 'MERCHANT') {
      void refreshMerchantStatus({ bypassManual: false }).catch(() => {});
      return;
    }

    manualMerchantStatusRef.current = false;
    setMerchantStatus(null);
    setMerchantStatusError(null);
    setIsMerchantStatusLoading(false);
  }, [refreshMerchantStatus, session?.role, session?.token]);

  const isAuthenticated = Boolean(session?.token && (!session.expiresAt || session.expiresAt > Date.now()));

  return useMemo(
    () => ({
      token: session?.token ?? null,
      role: session?.role ?? null,
      stripeAccountId: session?.stripeAccountId ?? null,
      isAuthenticated,
      isInitiating,
      initiateConnect,
      logout,
      merchantStatus,
      isMerchantStatusLoading,
      merchantStatusError,
      refreshMerchantStatus,
      setSessionFromToken,
      setMerchantStatusManually,
    }),
    [
      initiateConnect,
      isAuthenticated,
      isInitiating,
      logout,
      merchantStatus,
      isMerchantStatusLoading,
      merchantStatusError,
      refreshMerchantStatus,
      setSessionFromToken,
      setMerchantStatusManually,
      session?.role,
      session?.stripeAccountId,
      session?.token,
    ]
  );
};





