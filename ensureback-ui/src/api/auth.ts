import axiosClient, { clearStoredToken } from './axiosClient';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  role: string;
  stripeAccountId: string;
  redirectPath: string | null;
}

export interface StripeConnectStartRequest {
  role?: 'ADMIN' | 'MERCHANT' | 'BUYER';
  returnPath?: string;
}

export interface StripeConnectStartResponse {
  authorizationUrl: string;
}

export interface StripeConnectCallbackRequest {
  state: string;
  code?: string;
  error?: string;
  errorDescription?: string;
}

export const startStripeConnect = async (
  payload?: StripeConnectStartRequest
): Promise<StripeConnectStartResponse> => {
  const response = await axiosClient.post<StripeConnectStartResponse>('/auth/stripe/start', payload ?? {});
  return response.data;
};

export const completeStripeConnect = async (
  payload: StripeConnectCallbackRequest
): Promise<LoginResponse> => {
  const response = await axiosClient.post<LoginResponse>('/auth/stripe/callback', payload);
  return response.data;
};

export const clearSession = (): void => {
  clearStoredToken();
};

