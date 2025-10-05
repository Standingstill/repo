import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, BarChart3, Clock, Users } from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { KPICard } from '@/components/ui/KPICard';
import { PageHeader } from '@/components/ui/page-header';
import { StatPill } from '@/components/ui/StatPill';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { TrendChart } from '@/components/ui/TrendChart';

import {
  fetchAdminDisputes,
  fetchAdminLogs,
  fetchAdminMerchants,
  fetchAdminMetrics,
  fetchAdminTransactions,
  formatCurrency,
  type AdminTransaction
} from './data';

const STATUS_TONE: Record<AdminTransaction['status'], 'info' | 'success' | 'danger'> = {
  held: 'info',
  released: 'success',
  refunded: 'danger'
};

const AdminOverview = () => {
  const metricsQuery = useQuery({ queryKey: ['admin', 'metrics'], queryFn: fetchAdminMetrics });
  const transactionsQuery = useQuery({ queryKey: ['admin', 'transactions'], queryFn: fetchAdminTransactions });
  const merchantsQuery = useQuery({ queryKey: ['admin', 'merchants'], queryFn: fetchAdminMerchants });
  const disputesQuery = useQuery({ queryKey: ['admin', 'disputes'], queryFn: fetchAdminDisputes });
  const logsQuery = useQuery({ queryKey: ['admin', 'logs'], queryFn: fetchAdminLogs });

  const [transactionStatus, setTransactionStatus] = useState<'all' | AdminTransaction['status']>('all');
  const [merchantSearch, setMerchantSearch] = useState('');

  const kpis = useMemo(() => {
    const metrics = metricsQuery.data;
    if (!metrics) {
      return [];
    }

    return [
      {
        label: 'Active merchants',
        value: metrics.activeMerchants.toString(),
        helper: 'Sending protected payments this month.',
        icon: <Users className="h-6 w-6" />
      },
      {
        label: 'Protected payments (MTD)',
        value: formatCurrency(metrics.totalVolume, 'usd'),
        helper: 'Stripe payments under buyer protection.',
        icon: <BarChart3 className="h-6 w-6" />
      },
      {
        label: 'Dispute rate',
        value: `${metrics.refundRatio.toFixed(1)}%`,
        helper: 'Across the last 30 days.',
        icon: <Activity className="h-6 w-6" />
      },
      {
        label: 'Median resolution time',
        value: '2.9 days',
        helper: 'Based on resolved cases this quarter.',
        icon: <Clock className="h-6 w-6" />
      }
    ];
  }, [metricsQuery.data]);

  const trendData = useMemo(() => {
    const data = transactionsQuery.data ?? [];
    const bucket = new Map<string, { disputed: number; total: number }>();
    data.forEach((txn) => {
      const day = new Date(txn.createdAt).toISOString().slice(0, 10);
      if (!bucket.has(day)) {
        bucket.set(day, { disputed: 0, total: 0 });
      }
      const cell = bucket.get(day)!;
      cell.total += 1;
      if (txn.status === 'refunded') {
        cell.disputed += 1;
      }
    });
    return Array.from(bucket.entries())
      .sort(([a], [b]) => (a < b ? -1 : 1))
      .slice(-12)
      .map(([date, value]) => ({ date, rate: value.total ? Number(((value.disputed / value.total) * 100).toFixed(2)) : 0 }));
  }, [transactionsQuery.data]);

  const filteredTransactions = useMemo(() => {
    if (transactionStatus === 'all') {
      return transactionsQuery.data ?? [];
    }
    return (transactionsQuery.data ?? []).filter((txn) => txn.status === transactionStatus);
  }, [transactionsQuery.data, transactionStatus]);

  const filteredMerchants = useMemo(() => {
    const term = merchantSearch.toLowerCase();
    return (merchantsQuery.data ?? []).filter((merchant) => merchant.name.toLowerCase().includes(term));
  }, [merchantsQuery.data, merchantSearch]);

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Platform overview"
        title="Admin dashboard"
        description="Monitor global buyer protection performance, spot anomalies, and keep systems healthy."
      />

      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        {kpis.map((kpi, index) => (
          <KPICard key={kpi.label} label={kpi.label} value={kpi.value} helper={kpi.helper} icon={kpi.icon} delay={index * 0.05} />
        ))}
      </div>

      <Card className="border border-muted">
        <CardHeader>
          <CardTitle>Dispute rate over time</CardTitle>
          <CardDescription>Percentage of Stripe transactions that escalated into disputes each day.</CardDescription>
        </CardHeader>
        <CardContent>
          <TrendChart data={trendData} xKey="date" yKey="rate" yTickFormatter={(value) => `${value}%`} />
        </CardContent>
      </Card>

      <Card className="border border-muted">
        <CardHeader className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <CardTitle>Merchants</CardTitle>
            <CardDescription>Search for merchants to review performance.</CardDescription>
          </div>
          <Input
            value={merchantSearch}
            onChange={(event) => setMerchantSearch(event.target.value)}
            placeholder="Search merchants"
            className="max-w-xs"
            aria-label="Search merchants"
          />
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>GMV</TableHead>
                <TableHead>Active protections</TableHead>
                <TableHead>Dispute rate</TableHead>
                <TableHead className="text-right">Joined</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredMerchants.map((merchant) => (
                <TableRow key={merchant.id} className="text-sm">
                  <TableCell className="font-semibold">{merchant.name}</TableCell>
                  <TableCell>{formatCurrency(merchant.gmv, 'usd')}</TableCell>
                  <TableCell>{merchant.activeGuarantees}</TableCell>
                  <TableCell>{merchant.disputeRate}%</TableCell>
                  <TableCell className="text-right text-xs text-muted-foreground">
                    {new Date(merchant.createdAt).toLocaleDateString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card className="border border-muted">
        <CardHeader className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <CardTitle>Transactions</CardTitle>
            <CardDescription>Filter by settlement outcome to audit flows.</CardDescription>
          </div>
          <div className="flex gap-2">
            {(['all', 'held', 'released', 'refunded'] as const).map((status) => (
              <button
                key={status}
                type="button"
                onClick={() => setTransactionStatus(status)}
                className={`rounded-full border px-3 py-1 text-xs font-medium ${
                  transactionStatus === status ? 'border-primary bg-primary/10 text-primary' : 'border-muted text-muted-foreground'
                }`}
              >
                {status === 'all' ? 'All' : status}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Merchant</TableHead>
                <TableHead>Buyer</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead className="text-right">Created</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredTransactions.map((txn) => (
                <TableRow key={txn.id} className="text-sm">
                  <TableCell className="font-semibold">{txn.id}</TableCell>
                  <TableCell>{txn.merchant}</TableCell>
                  <TableCell>{txn.buyer}</TableCell>
                  <TableCell>
                    <StatPill tone={STATUS_TONE[txn.status]}>{txn.status}</StatPill>
                  </TableCell>
                  <TableCell>{formatCurrency(txn.amount, txn.currency)}</TableCell>
                  <TableCell className="text-right text-xs text-muted-foreground">
                    {new Date(txn.createdAt).toLocaleString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card className="border border-muted">
        <CardHeader>
          <CardTitle>Cases / Disputes</CardTitle>
          <CardDescription>EnsureBack case pipeline grouped by outcome.</CardDescription>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Merchant</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Opened</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(disputesQuery.data ?? []).map((dispute) => (
                <TableRow key={dispute.id} className="text-sm">
                  <TableCell className="font-semibold">{dispute.id}</TableCell>
                  <TableCell>{dispute.merchant}</TableCell>
                  <TableCell>
                    <StatPill tone={dispute.status === 'open' ? 'info' : dispute.status === 'won' ? 'success' : 'danger'}>
                      {dispute.status}
                    </StatPill>
                  </TableCell>
                  <TableCell className="text-right text-xs text-muted-foreground">
                    {new Date(dispute.openedAt).toLocaleString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card className="border border-muted">
        <CardHeader>
          <CardTitle>System logs</CardTitle>
          <CardDescription>Operational events from automation, policies, and webhooks.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {(logsQuery.data ?? []).map((log) => (
            <div key={log.id} className="rounded-2xl border border-muted bg-muted/30 p-4 text-sm text-muted-foreground">
              <div className="flex items-center justify-between gap-3">
                <StatPill tone="info">{log.type}</StatPill>
                <span className="text-xs">{new Date(log.createdAt).toLocaleString()}</span>
              </div>
              <p className="mt-3 text-foreground">{log.message}</p>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
};

export default AdminOverview;
