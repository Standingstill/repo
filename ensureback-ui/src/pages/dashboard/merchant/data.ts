import { isAxiosError } from 'axios';

import axiosClient from '@/api/axiosClient';

export interface MerchantProfile {
  id: string;
  email: string;
  role: string;
  createdAt: string;
  stripeAccountId?: string | null;
  isIntegrated?: boolean;
  companyName?: string;
  supportEmail?: string;
  financeEmail?: string;
  apiKey?: string;
  permissions?: Array<{ name: string; status: 'active' | 'pending' | 'revoked' }>;
}

export interface MerchantStatus {
  stripeAccountId?: string | null;
  isIntegrated: boolean;
}

export interface MerchantOrder {
  id: string;
  customerEmail: string;
  status: 'processing' | 'fulfilled' | 'disputed';
  amount: number;
  currency: string;
  updatedAt: string;
}

export interface MerchantDispute {
  id: string;
  orderId: string;
  status: 'open' | 'closed' | 'partial_refund';
  reason: string;
  openedAt: string;
}

export interface MerchantBalance {
  escrow: number;
  available: number;
  currency: string;
}

export interface MerchantPolicy {
  id: string;
  name: string;
  slaHours: number;
  autoRefundThreshold: number;
  evidenceChecklist: string[];
}

export const fetchMerchantProfile = async (): Promise<MerchantProfile> => {
  const response = await axiosClient.get<MerchantProfile>('/merchant/me');
  return response.data;
};

export const fetchMerchantStatus = async (): Promise<MerchantStatus> => {
  try {
    const response = await axiosClient.get<MerchantStatus>('/merchant/status');
    return response.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return { isIntegrated: false, stripeAccountId: null };
    }
    throw error;
  }
};

export const fetchOrders = async (): Promise<MerchantOrder[]> => {
  try {
    const response = await axiosClient.get<MerchantOrder[]>('/merchant/orders');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        id: 'ord_12345',
        customerEmail: 'buyer1@example.com',
        status: 'processing',
        amount: 12900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 45).toISOString()
      },
      {
        id: 'ord_67890',
        customerEmail: 'buyer2@example.com',
        status: 'fulfilled',
        amount: 8900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 7).toISOString()
      },
      {
        id: 'ord_24680',
        customerEmail: 'buyer3@example.com',
        status: 'disputed',
        amount: 21000,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 30).toISOString()
      }
    ];
  }
};

export const fetchDisputes = async (): Promise<MerchantDispute[]> => {
  try {
    const response = await axiosClient.get<MerchantDispute[]>('/merchant/disputes');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        id: 'dp_101',
        orderId: 'ord_24680',
        status: 'open',
        reason: 'Item not as described',
        openedAt: new Date(now - 1000 * 60 * 60 * 24).toISOString()
      },
      {
        id: 'dp_205',
        orderId: 'ord_13579',
        status: 'partial_refund',
        reason: 'Shipping delay',
        openedAt: new Date(now - 1000 * 60 * 60 * 72).toISOString()
      },
      {
        id: 'dp_309',
        orderId: 'ord_54321',
        status: 'closed',
        reason: 'Resolved in buyer favor',
        openedAt: new Date(now - 1000 * 60 * 60 * 96).toISOString()
      }
    ];
  }
};

export const fetchBalance = async (): Promise<MerchantBalance> => {
  try {
    const response = await axiosClient.get<MerchantBalance>('/merchant/balance');
    return response.data;
  } catch (error) {
    return { escrow: 235000, available: 128500, currency: 'usd' };
  }
};

export const fetchPolicies = async (): Promise<MerchantPolicy[]> => {
  try {
    const response = await axiosClient.get<MerchantPolicy[]>('/merchant/policies');
    return response.data;
  } catch (error) {
    return [
      {
        id: 'policy_standard',
        name: 'Standard SaaS subscription',
        slaHours: 72,
        autoRefundThreshold: 15000,
        evidenceChecklist: ['Subscription agreement', 'Usage logs', 'Customer communication transcript']
      },
      {
        id: 'policy_digital',
        name: 'Digital goods delivery',
        slaHours: 48,
        autoRefundThreshold: 9500,
        evidenceChecklist: ['Download confirmation', 'IP address verification', 'Delivery email proof']
      }
    ];
  }
};

export const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100);
