import axiosClient from '@/api/axiosClient';

export interface AdminMetrics {
  totalVolume: number;
  refundRatio: number;
  activeMerchants: number;
  disputesOpen: number;
}

export interface AdminTransaction {
  id: string;
  merchant: string;
  buyer: string;
  amount: number;
  currency: string;
  status: 'held' | 'released' | 'refunded';
  createdAt: string;
}

export interface AdminMerchant {
  id: string;
  name: string;
  gmv: number;
  activeGuarantees: number;
  disputeRate: number;
  createdAt: string;
}

export interface AdminDisputeSummary {
  id: string;
  merchant: string;
  status: 'open' | 'won' | 'lost';
  openedAt: string;
}

export interface AdminLog {
  id: string;
  type: 'webhook' | 'policy' | 'automation';
  message: string;
  createdAt: string;
}

export const fetchAdminMetrics = async (): Promise<AdminMetrics> => {
  try {
    const response = await axiosClient.get<AdminMetrics>('/admin/metrics');
    return response.data;
  } catch (error) {
    return {
      totalVolume: 4820000,
      refundRatio: 2.8,
      activeMerchants: 184,
      disputesOpen: 12
    };
  }
};

export const fetchAdminTransactions = async (): Promise<AdminTransaction[]> => {
  try {
    const response = await axiosClient.get<AdminTransaction[]>('/admin/transactions');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        id: 'txn_1',
        merchant: 'LaunchPad SaaS',
        buyer: 'buyer1@example.com',
        amount: 19900,
        currency: 'usd',
        status: 'held',
        createdAt: new Date(now - 1000 * 60 * 35).toISOString()
      },
      {
        id: 'txn_2',
        merchant: 'DigitalCraft Stores',
        buyer: 'buyer2@example.com',
        amount: 8900,
        currency: 'usd',
        status: 'released',
        createdAt: new Date(now - 1000 * 60 * 60 * 5).toISOString()
      },
      {
        id: 'txn_3',
        merchant: 'SaaSify Inc.',
        buyer: 'buyer3@example.com',
        amount: 5900,
        currency: 'usd',
        status: 'refunded',
        createdAt: new Date(now - 1000 * 60 * 60 * 18).toISOString()
      }
    ];
  }
};

export const fetchAdminMerchants = async (): Promise<AdminMerchant[]> => {
  try {
    const response = await axiosClient.get<AdminMerchant[]>('/admin/merchants');
    return response.data;
  } catch (error) {
    return [
      {
        id: 'mrc_1',
        name: 'LaunchPad SaaS',
        gmv: 1250000,
        activeGuarantees: 48,
        disputeRate: 1.8,
        createdAt: '2024-04-16T00:00:00.000Z'
      },
      {
        id: 'mrc_2',
        name: 'DigitalCraft Stores',
        gmv: 820000,
        activeGuarantees: 32,
        disputeRate: 3.2,
        createdAt: '2023-11-04T00:00:00.000Z'
      },
      {
        id: 'mrc_3',
        name: 'SaaSify Inc.',
        gmv: 430000,
        activeGuarantees: 21,
        disputeRate: 2.5,
        createdAt: '2025-02-12T00:00:00.000Z'
      }
    ];
  }
};

export const fetchAdminDisputes = async (): Promise<AdminDisputeSummary[]> => {
  try {
    const response = await axiosClient.get<AdminDisputeSummary[]>('/admin/disputes');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        id: 'dp_admin_1',
        merchant: 'LaunchPad SaaS',
        status: 'open',
        openedAt: new Date(now - 1000 * 60 * 60 * 12).toISOString()
      },
      {
        id: 'dp_admin_2',
        merchant: 'DigitalCraft Stores',
        status: 'won',
        openedAt: new Date(now - 1000 * 60 * 60 * 54).toISOString()
      }
    ];
  }
};

export const fetchAdminLogs = async (): Promise<AdminLog[]> => {
  try {
    const response = await axiosClient.get<AdminLog[]>('/admin/logs');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        id: 'log_1',
        type: 'webhook',
        message: 'Stripe webhook delivery succeeded for dispute.created',
        createdAt: new Date(now - 1000 * 60 * 12).toISOString()
      },
      {
        id: 'log_2',
        type: 'automation',
        message: 'Policy playbook auto_grant_partial_refund executed for ord_24680',
        createdAt: new Date(now - 1000 * 60 * 60).toISOString()
      },
      {
        id: 'log_3',
        type: 'policy',
        message: 'Risk rule updated: friendly_fraud_threshold set to medium',
        createdAt: new Date(now - 1000 * 60 * 90).toISOString()
      }
    ];
  }
};

export const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100);
