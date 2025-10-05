import axiosClient from '@/api/axiosClient';

export interface BuyerOrder {
  orderId: string;
  merchantName: string;
  status: 'protected' | 'action_required' | 'resolved';
  total: number;
  currency: string;
  updatedAt: string;
  timeline: string[];
}

export const fetchBuyerOrders = async (): Promise<BuyerOrder[]> => {
  try {
    const response = await axiosClient.get<BuyerOrder[]>('/buyer/orders');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        orderId: 'ORDER-12345',
        merchantName: 'LaunchPad SaaS',
        status: 'protected',
        total: 12500,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 15).toISOString(),
        timeline: ['Created', 'Payment received', 'Protection active']
      },
      {
        orderId: 'ORDER-67890',
        merchantName: 'DigitalCraft Stores',
        status: 'action_required',
        total: 18900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 18).toISOString(),
        timeline: ['Created', 'Evidence requested', 'Awaiting buyer response']
      },
      {
        orderId: 'ORDER-24680',
        merchantName: 'Headline Tools',
        status: 'resolved',
        total: 32900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 42).toISOString(),
        timeline: ['Created', 'Action taken', 'Resolved']
      }
    ];
  }
};

export const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100);
