import axiosClient, { TOKEN_STORAGE_KEY } from './axiosClient';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  role: string;
}

export interface StripeConnectStartRequest {
  email: string;
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
  payload: StripeConnectStartRequest
): Promise<StripeConnectStartResponse> => {
  const response = await axiosClient.post<StripeConnectStartResponse>('/auth/connect/start', payload);
  return response.data;
};

export const completeStripeConnect = async (
  payload: StripeConnectCallbackRequest
): Promise<LoginResponse> => {
  const response = await axiosClient.post<LoginResponse>('/auth/connect/callback', payload);
  return response.data;
};

export const logout = (): void => {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
};
