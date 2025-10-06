import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';

import { clearSession } from '../api/auth';
import axiosClient, { TOKEN_STORAGE_KEY, clearStoredToken, persistToken, readStoredToken } from '../api/axiosClient';

interface SessionState {
  token: string;
  role: string;
  stripeAccountId: string | null;
  expiresAt?: number;
}

interface StripeOnboardResponse {
  redirectUrl: string;
  alreadyConnected: boolean;
}

type IntegrationCheckResult = boolean | null;

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
  const stripeAccountId = typeof payload.stripe_account_id === 'string' ? payload.stripe_account_id : null;
  const expiresAt = typeof payload.exp === 'number' ? payload.exp * 1000 : undefined;
  if (!role) {
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
  isIntegrated: boolean | null;
  isCheckingIntegration: boolean;
  hasCheckedIntegration: boolean;
  integrationError: string | null;
  checkIntegrationStatus: (options?: { force?: boolean }) => Promise<IntegrationCheckResult>;
  setSessionFromToken: (token: string) => void;
  setIntegrationStatusManually: (status: boolean | null) => void;
}

export const useAuth = (): UseAuthResult => {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<SessionState | null>(getInitialSession);
  const sessionRef = useRef<SessionState | null>(session);
  const [isInitiating, setIsInitiating] = useState(false);
  const integrationRequestRef = useRef<Promise<IntegrationCheckResult> | null>(null);
  const [isIntegrated, setIsIntegrated] = useState<boolean | null>(null);
  const [isCheckingIntegration, setIsCheckingIntegration] = useState(false);
  const [hasCheckedIntegration, setHasCheckedIntegration] = useState(false);
  const [integrationError, setIntegrationError] = useState<string | null>(null);

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
        integrationRequestRef.current = null;
        setIsIntegrated(null);
        setIntegrationError(null);
        setHasCheckedIntegration(false);
      } else {
        clearStoredToken();
        setSession(null);
        sessionRef.current = null;
        integrationRequestRef.current = null;
        setIsIntegrated(null);
        setIntegrationError(null);
        setHasCheckedIntegration(false);
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
        integrationRequestRef.current = null;
        setIsIntegrated(null);
        setIntegrationError(null);
        setHasCheckedIntegration(false);
      }
    };

    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const resetIntegrationState = useCallback(() => {
    integrationRequestRef.current = null;
    setIsIntegrated(null);
    setHasCheckedIntegration(false);
    setIntegrationError(null);
    setIsCheckingIntegration(false);
  }, []);

  const setSessionFromToken = useCallback((token: string) => {
    if (typeof window === 'undefined') {
      return;
    }

    if (!token) {
      clearStoredToken();
      setSession(null);
      sessionRef.current = null;
      resetIntegrationState();
      return;
    }

    persistToken(token);
    const nextSession = toSessionState(token);
    if (!nextSession) {
      clearStoredToken();
      setSession(null);
      sessionRef.current = null;
      resetIntegrationState();
      return;
    }

    setSession(nextSession);
    sessionRef.current = nextSession;
    resetIntegrationState();
  }, [resetIntegrationState]);

  const setIntegrationStatusManually = useCallback(
    (status: boolean | null) => {
      integrationRequestRef.current = null;
      setIsIntegrated(status);
      setHasCheckedIntegration(true);
      setIntegrationError(null);
      setIsCheckingIntegration(false);
    },
    []
  );

  const checkIntegrationStatus = useCallback(
    async (options?: { force?: boolean }): Promise<IntegrationCheckResult> => {
      const force = options?.force ?? false;
      const currentSession = sessionRef.current;

      if (!currentSession?.token || currentSession.role !== 'MERCHANT') {
        integrationRequestRef.current = null;
        setIsIntegrated(null);
        setIntegrationError(null);
        setHasCheckedIntegration(true);
        setIsCheckingIntegration(false);
        return null;
      }

      if (!force) {
        if (integrationRequestRef.current) {
          return integrationRequestRef.current;
        }

        if (hasCheckedIntegration) {
          return isIntegrated;
        }
      }

      const request = (async () => {
        setIsCheckingIntegration(true);
        try {
          const response = await axiosClient.get<{ isIntegrated: boolean }>('/merchant/status');
          const integrated = Boolean(response.data?.isIntegrated);
          setIsIntegrated(integrated);
          setIntegrationError(null);
          return integrated;
        } catch (error) {
          console.error('Unable to load merchant integration status', error);
          if (isAxiosError(error)) {
            const status = error.response?.status;
            if (status === 401 || status === 403) {
              setIntegrationError('Session expired, please reconnect Stripe.');
              setIsIntegrated(false);
            } else if (!error.response) {
              setIntegrationError('We are having trouble reaching EnsureBack. Check your connection and try again.');
            } else {
              setIntegrationError('Unable to load merchant status. Please try again later.');
            }
          } else {
            setIntegrationError('Unable to load merchant status. Please try again later.');
          }
          return null;
        } finally {
          setHasCheckedIntegration(true);
          setIsCheckingIntegration(false);
          integrationRequestRef.current = null;
        }
      })();

      integrationRequestRef.current = request;
      return request;
    },
    [hasCheckedIntegration, isIntegrated]
  );

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
    sessionRef.current = null;
    resetIntegrationState();
    queryClient.clear();
  }, [queryClient, resetIntegrationState]);

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
    if (!session?.token || session.role !== 'MERCHANT') {
      resetIntegrationState();
      return;
    }

    if (!hasCheckedIntegration && !isCheckingIntegration) {
      void checkIntegrationStatus().catch(() => undefined);
    }
  }, [checkIntegrationStatus, hasCheckedIntegration, isCheckingIntegration, resetIntegrationState, session?.role, session?.token]);

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
      isIntegrated,
      isCheckingIntegration,
      hasCheckedIntegration,
      integrationError,
      checkIntegrationStatus,
      setSessionFromToken,
      setIntegrationStatusManually,
    }),
    [
      initiateConnect,
      isAuthenticated,
      isInitiating,
      logout,
      isIntegrated,
      isCheckingIntegration,
      hasCheckedIntegration,
      integrationError,
      checkIntegrationStatus,
      setSessionFromToken,
      setIntegrationStatusManually,
      session?.role,
      session?.stripeAccountId,
      session?.token,
    ]
  );
};





