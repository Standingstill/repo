import axiosClient, { TOKEN_STORAGE_KEY } from './axiosClient';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  role: string;
}

export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const response = await axiosClient.post<LoginResponse>('/auth/login', payload);
  return response.data;
};

export const logout = (): void => {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
};
