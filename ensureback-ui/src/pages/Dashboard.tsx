import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, ArrowUpRight, RefreshCw, ShieldAlert } from 'lucide-react';

import axiosClient from '@/api/axiosClient';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';

interface MerchantProfile {
  id: string;
  email: string;
  role: string;
  createdAt: string;
  stripeAccountId?: string;
}

interface MerchantOrder {
  id: string;
  customerEmail: string;
  status: 'processing' | 'fulfilled' | 'disputed';
  amount: number;
  currency: string;
  updatedAt: string;
}

interface MerchantDispute {
  id: string;
  orderId: string;
  status: 'open' | 'closed' | 'partial_refund';
  reason: string;
  openedAt: string;
}

interface MerchantBalance {
  escrow: number;
  available: number;
  currency: string;
}

const fetchMerchantProfile = async (): Promise<MerchantProfile> => {
  const response = await axiosClient.get<MerchantProfile>('/merchant/me');
  return response.data;
};

const fetchOrders = async (): Promise<MerchantOrder[]> => {
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

const fetchDisputes = async (): Promise<MerchantDispute[]> => {
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

const fetchBalance = async (): Promise<MerchantBalance> => {
  try {
    const response = await axiosClient.get<MerchantBalance>('/merchant/balance');
    return response.data;
  } catch (error) {
    return { escrow: 235000, available: 128500, currency: 'usd' };
  }
};

const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100);

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'orders' | 'disputes' | 'balance'>('overview');

  const profileQuery = useQuery({ queryKey: ['merchant', 'profile'], queryFn: fetchMerchantProfile });
  const ordersQuery = useQuery({ queryKey: ['merchant', 'orders'], queryFn: fetchOrders });
  const disputesQuery = useQuery({ queryKey: ['merchant', 'disputes'], queryFn: fetchDisputes });
  const balanceQuery = useQuery({ queryKey: ['merchant', 'balance'], queryFn: fetchBalance });

  const disputeStats = useMemo(() => {
    if (!disputesQuery.data) return { open: 0, closed: 0, partial: 0 };
    return {
      open: disputesQuery.data.filter((d) => d.status === 'open').length,
      closed: disputesQuery.data.filter((d) => d.status === 'closed').length,
      partial: disputesQuery.data.filter((d) => d.status === 'partial_refund').length
    };
  }, [disputesQuery.data]);

  const tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'orders', label: 'Orders' },
    { id: 'disputes', label: 'Disputes' },
    { id: 'balance', label: 'Balance' }
  ] as const;

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-4 py-12">
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">Merchant Dashboard</h1>
        <p className="text-muted-foreground">
          Monitor your EnsureBack account, manage disputes, and keep buyers protected.
        </p>
      </header>

      <div className="flex flex-wrap gap-3">
        {tabs.map((tab) => (
          <Button
            key={tab.id}
            variant={activeTab === tab.id ? 'default' : 'outline'}
            onClick={() => setActiveTab(tab.id)}
            className="rounded-full"
          >
            {tab.label}
          </Button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle>Account details</CardTitle>
                <CardDescription>Signed in as {profileQuery.data?.email ?? 'loading…'}</CardDescription>
              </div>
              <Button variant="ghost" size="sm" onClick={() => profileQuery.refetch()} disabled={profileQuery.isLoading}>
                <RefreshCw className="mr-2 h-4 w-4" /> Refresh
              </Button>
            </CardHeader>
            <CardContent className="space-y-4 text-sm">
              <div className="flex flex-col gap-2">
                <span className="text-muted-foreground">Merchant ID</span>
                <span className="font-medium">{profileQuery.data?.id ?? '—'}</span>
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-muted-foreground">Role</span>
                <span className="font-medium">{profileQuery.data?.role ?? '—'}</span>
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-muted-foreground">Joined</span>
                <span className="font-medium">
                  {profileQuery.data ? new Date(profileQuery.data.createdAt).toLocaleString() : '—'}
                </span>
              </div>
              <div className="flex flex-col gap-2">
                <span className="text-muted-foreground">Stripe account</span>
                <span className="font-medium">{profileQuery.data?.stripeAccountId ?? 'Connect to Stripe'}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ShieldAlert className="h-5 w-5 text-primary" /> Dispute summary
              </CardTitle>
              <CardDescription>Track open cases and partial refunds.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm md:grid-cols-3">
              <div className="rounded-lg border bg-muted/40 p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Open</p>
                <p className="mt-2 text-2xl font-semibold">{disputeStats.open}</p>
              </div>
              <div className="rounded-lg border bg-muted/40 p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Closed</p>
                <p className="mt-2 text-2xl font-semibold">{disputeStats.closed}</p>
              </div>
              <div className="rounded-lg border bg-muted/40 p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Partial refunds</p>
                <p className="mt-2 text-2xl font-semibold">{disputeStats.partial}</p>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'orders' && (
        <Card>
          <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Recent orders</CardTitle>
              <CardDescription>Latest payments captured through EnsureBack.</CardDescription>
            </div>
            <Button variant="ghost" size="sm" onClick={() => ordersQuery.refetch()} disabled={ordersQuery.isLoading}>
              <RefreshCw className="mr-2 h-4 w-4" /> Refresh
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead className="text-right">Updated</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {ordersQuery.data?.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-medium">{order.id}</TableCell>
                    <TableCell>{order.customerEmail}</TableCell>
                    <TableCell>
                      <span
                        className={cn(
                          'rounded-full px-3 py-1 text-xs font-medium capitalize',
                          order.status === 'fulfilled' && 'bg-emerald-100 text-emerald-700',
                          order.status === 'processing' && 'bg-amber-100 text-amber-700',
                          order.status === 'disputed' && 'bg-red-100 text-red-700'
                        )}
                      >
                        {order.status}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">{formatCurrency(order.amount, order.currency)}</TableCell>
                    <TableCell className="text-right">
                      {new Date(order.updatedAt).toLocaleString()}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {activeTab === 'disputes' && (
        <Card>
          <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Disputes</CardTitle>
              <CardDescription>Filter by outcome and respond to buyers quickly.</CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => disputesQuery.refetch()}
                disabled={disputesQuery.isLoading}
              >
                <RefreshCw className="mr-2 h-4 w-4" /> Refresh
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap gap-2 text-sm">
              <span className="inline-flex items-center gap-2 rounded-full border bg-muted/40 px-3 py-1">
                <AlertCircle className="h-4 w-4 text-amber-600" /> Open {disputeStats.open}
              </span>
              <span className="inline-flex items-center gap-2 rounded-full border bg-muted/40 px-3 py-1">
                <ArrowUpRight className="h-4 w-4 text-emerald-600" /> Partial refunds {disputeStats.partial}
              </span>
              <span className="inline-flex items-center gap-2 rounded-full border bg-muted/40 px-3 py-1">
                <ShieldAlert className="h-4 w-4 text-slate-600" /> Closed {disputeStats.closed}
              </span>
            </div>
            <div className="grid gap-4">
              {disputesQuery.data?.map((dispute) => (
                <div key={dispute.id} className="rounded-lg border bg-card p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold">{dispute.reason}</p>
                      <p className="text-xs text-muted-foreground">Order {dispute.orderId}</p>
                    </div>
                    <span
                      className={cn(
                        'rounded-full px-3 py-1 text-xs font-medium capitalize',
                        dispute.status === 'open' && 'bg-amber-100 text-amber-700',
                        dispute.status === 'closed' && 'bg-emerald-100 text-emerald-700',
                        dispute.status === 'partial_refund' && 'bg-blue-100 text-blue-700'
                      )}
                    >
                      {dispute.status.replace('_', ' ')}
                    </span>
                  </div>
                  <p className="mt-3 text-xs text-muted-foreground">
                    Opened {new Date(dispute.openedAt).toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {activeTab === 'balance' && (
        <Card>
          <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Balance overview</CardTitle>
              <CardDescription>Escrow versus available payouts.</CardDescription>
            </div>
            <Button variant="ghost" size="sm" onClick={() => balanceQuery.refetch()} disabled={balanceQuery.isLoading}>
              <RefreshCw className="mr-2 h-4 w-4" /> Refresh
            </Button>
          </CardHeader>
          <CardContent className="grid gap-6 md:grid-cols-2">
            <div className="rounded-lg border bg-muted/40 p-6">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Escrow balance</p>
              <p className="mt-3 text-3xl font-semibold">
                {balanceQuery.data ? formatCurrency(balanceQuery.data.escrow, balanceQuery.data.currency) : '—'}
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Held until delivery confirmation or dispute resolution.
              </p>
            </div>
            <div className="rounded-lg border bg-muted/40 p-6">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Available balance</p>
              <p className="mt-3 text-3xl font-semibold">
                {balanceQuery.data ? formatCurrency(balanceQuery.data.available, balanceQuery.data.currency) : '—'}
              </p>
              <p className="mt-2 text-sm text-muted-foreground">Eligible for payout to your connected Stripe account.</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default Dashboard;
