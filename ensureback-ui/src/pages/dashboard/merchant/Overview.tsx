import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { ArrowUpRight, Clock, ShieldCheck, Zap } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { KPICard } from '@/components/ui/KPICard';
import { StatPill } from '@/components/ui/StatPill';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { TrendChart } from '@/components/ui/TrendChart';

import {
  fetchBalance,
  fetchDisputes,
  fetchMerchantProfile,
  fetchMerchantStatus,
  fetchOrders,
  formatCurrency,
  type MerchantDispute,
  type MerchantOrder
} from './data';

const STATUS_MAP: Record<MerchantDispute['status'], { label: string; tone: 'info' | 'success' | 'danger' }> = {
  open: { label: 'Open', tone: 'info' },
  partial_refund: { label: 'Action required', tone: 'danger' },
  closed: { label: 'Resolved', tone: 'success' }
};

const buildTrend = (orders: MerchantOrder[]) => {
  const buckets = new Map<string, { open: number; total: number }>();
  orders.forEach((order) => {
    const day = new Date(order.updatedAt).toISOString().slice(0, 10);
    if (!buckets.has(day)) {
      buckets.set(day, { open: 0, total: 0 });
    }
    const bucket = buckets.get(day)!;
    bucket.total += 1;
    if (order.status === 'disputed') {
      bucket.open += 1;
    }
  });

  const sorted = Array.from(buckets.entries()).sort(([a], [b]) => (a < b ? -1 : 1)).slice(-12);
  return sorted.map(([date, value]) => ({
    date,
    rate: value.total ? Number(((value.open / value.total) * 100).toFixed(2)) : 0
  }));
};

const getRecentCases = (disputes: MerchantDispute[]) =>
  disputes
    .map((dispute) => ({
      id: dispute.id,
      orderId: dispute.orderId,
      status: dispute.status,
      updatedAt: dispute.openedAt,
      reason: dispute.reason
    }))
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 6);

const MerchantOverview = () => {
  const profileQuery = useQuery({ queryKey: ['merchant', 'profile'], queryFn: fetchMerchantProfile });
  const statusQuery = useQuery({ queryKey: ['merchant', 'status'], queryFn: fetchMerchantStatus });
  const navigate = useNavigate();

  const ordersQuery = useQuery({ queryKey: ['merchant', 'orders'], queryFn: fetchOrders });
  const disputesQuery = useQuery({ queryKey: ['merchant', 'disputes'], queryFn: fetchDisputes });
  const balanceQuery = useQuery({ queryKey: ['merchant', 'balance'], queryFn: fetchBalance });

  const trendData = useMemo(() => buildTrend(ordersQuery.data ?? []), [ordersQuery.data]);
  const recentCases = useMemo(() => getRecentCases(disputesQuery.data ?? []), [disputesQuery.data]);

  const protectedAmount = balanceQuery.data ? formatCurrency(balanceQuery.data.escrow, balanceQuery.data.currency) : '—';
  const openCases = disputesQuery.data?.filter((item) => item.status !== 'closed').length ?? 0;
  const chargebacks = ordersQuery.data?.filter((order) => order.status === 'disputed').length ?? 0;

  const kpis = [
    {
      label: 'Protected payments',
      value: protectedAmount,
      helper: 'Active protections across Stripe charges.',
      icon: <ShieldCheck className="h-6 w-6" />
    },
    {
      label: 'Open cases',
      value: openCases.toString(),
      helper: 'Cases awaiting action today.',
      icon: <Zap className="h-6 w-6" />
    },
    {
      label: 'Chargebacks this month',
      value: chargebacks.toString(),
      helper: 'Synced from Stripe dispute feed.',
      icon: <ArrowUpRight className="h-6 w-6" />
    },
    {
      label: 'Avg. time-to-resolution',
      value: '3.4 days',
      helper: 'Median across resolved cases in the last 30 days.',
      icon: <Clock className="h-6 w-6" />
    }
  ];

  return (
    <div className="space-y-8">
      {!statusQuery.isLoading && !statusQuery.isError && statusQuery.data && !statusQuery.data.isIntegrated && (
        <Alert className="border-primary/40 bg-primary/10">
          <AlertTitle>Setup Required</AlertTitle>
          <AlertDescription>
            Your Stripe account is connected but integration is pending. Complete the remaining steps to unlock the dashboard.
          </AlertDescription>
          <div className="mt-4 flex justify-end">
            <Button onClick={() => navigate('/integration-wizard')}>Complete Integration</Button>
          </div>
        </Alert>
      )}
      <Card className="border border-muted bg-card">
        <CardHeader>
          <CardTitle className="text-2xl">Welcome back{profileQuery.data?.companyName ? `, ${profileQuery.data.companyName}` : ''}</CardTitle>
          <CardDescription className="text-base">
            Monitor protections, collaborate on cases, and keep Stripe chargebacks under control.
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        {kpis.map((kpi, index) => (
          <KPICard
            key={kpi.label}
            label={kpi.label}
            value={kpi.value}
            helper={kpi.helper}
            icon={kpi.icon}
            delay={index * 0.05}
          />
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Dispute rate (last 90 days)</CardTitle>
            <CardDescription>Computed from Stripe disputes vs. total protected payments.</CardDescription>
          </CardHeader>
          <CardContent>
            <TrendChart data={trendData} xKey="date" yKey="rate" yTickFormatter={(value) => `${value}%`} />
          </CardContent>
        </Card>

        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Case workload</CardTitle>
            <CardDescription>Status of buyer protection cases by owner.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4 text-sm text-muted-foreground">
            <div className="flex items-center justify-between">
              <span>Open</span>
              <StatPill tone="info">{openCases}</StatPill>
            </div>
            <div className="flex items-center justify-between">
              <span>Resolved this week</span>
              <StatPill tone="success">{disputesQuery.data?.filter((item) => item.status === 'closed').length ?? 0}</StatPill>
            </div>
            <div className="flex items-center justify-between">
              <span>Action required</span>
              <StatPill tone="danger">
                {disputesQuery.data?.filter((item) => item.status === 'partial_refund').length ?? 0}
              </StatPill>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="border border-muted">
        <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Recent cases</CardTitle>
            <CardDescription>Latest buyer protection activity synced from Stripe disputes.</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="text-xs uppercase tracking-[0.3em] text-muted-foreground">
                <TableHead>Case</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Order</TableHead>
                <TableHead className="hidden md:table-cell">Reason</TableHead>
                <TableHead className="text-right">Updated</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentCases.map((item) => {
                const statusMeta = STATUS_MAP[item.status];
                return (
                  <TableRow key={item.id} className="text-sm">
                    <TableCell className="font-medium">{item.id}</TableCell>
                    <TableCell>
                      <StatPill tone={statusMeta.tone}>{statusMeta.label}</StatPill>
                    </TableCell>
                    <TableCell>{item.orderId}</TableCell>
                    <TableCell className="hidden max-w-[220px] truncate text-muted-foreground md:table-cell">
                      {item.reason}
                    </TableCell>
                    <TableCell className="text-right text-xs text-muted-foreground">
                      {new Date(item.updatedAt).toLocaleString()}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

export default MerchantOverview;
