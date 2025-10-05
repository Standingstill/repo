import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight, ShieldCheck } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/ui/page-header';
import { StatPill } from '@/components/ui/StatPill';

import { fetchBuyerOrders, formatCurrency, type BuyerOrder } from './data';

const STATUS_LABEL: Record<BuyerOrder['status'], { label: string; tone: 'info' | 'danger' | 'success' }> = {
  protected: { label: 'Protected', tone: 'info' },
  action_required: { label: 'Action required', tone: 'danger' },
  resolved: { label: 'Resolved', tone: 'success' }
};

const Overview = () => {
  const ordersQuery = useQuery({ queryKey: ['buyer', 'orders'], queryFn: fetchBuyerOrders });
  const orders = ordersQuery.data ?? [];

  const sections = useMemo(() => ({
    protected: orders.filter((order) => order.status === 'protected'),
    actionRequired: orders.filter((order) => order.status === 'action_required'),
    resolved: orders.filter((order) => order.status === 'resolved')
  }), [orders]);

  const renderCard = (order: BuyerOrder) => {
    const statusMeta = STATUS_LABEL[order.status];
    return (
      <Card key={order.orderId} className="border border-muted">
        <CardHeader className="space-y-2">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">{order.merchantName}</CardTitle>
            <StatPill tone={statusMeta.tone}>{statusMeta.label}</StatPill>
          </div>
          <CardDescription>Order {order.orderId}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4 text-sm text-muted-foreground">
          <div className="flex items-center justify-between text-foreground">
            <span className="font-semibold">{formatCurrency(order.total, order.currency)}</span>
            <span>{new Date(order.updatedAt).toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-2 text-xs">
            <ShieldCheck className="h-4 w-4 text-primary" aria-hidden="true" />
            <span>{order.timeline[order.timeline.length - 1]}</span>
          </div>
          <div className="flex justify-end">
            <Button asChild size="sm" variant="outline">
              <Link to={`/dashboard/buyer/cases/${order.orderId}`} className="flex items-center gap-2">
                View timeline <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  };

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Buyer view"
        title="Protected purchases"
        description="Track all Stripe payments protected by EnsureBack, take action on open cases, and review resolved outcomes."
      />

      <section className="space-y-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.3em] text-muted-foreground">Protected purchases</h2>
        {sections.protected.length ? (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{sections.protected.map(renderCard)}</div>
        ) : (
          <p className="text-sm text-muted-foreground">No protected purchases right now.</p>
        )}
      </section>

      <section className="space-y-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.3em] text-muted-foreground">Open cases</h2>
        {sections.actionRequired.length ? (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{sections.actionRequired.map(renderCard)}</div>
        ) : (
          <p className="text-sm text-muted-foreground">No cases require your attention.</p>
        )}
      </section>

      <section className="space-y-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.3em] text-muted-foreground">Resolved</h2>
        {sections.resolved.length ? (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{sections.resolved.map(renderCard)}</div>
        ) : (
          <p className="text-sm text-muted-foreground">No resolved cases yet.</p>
        )}
      </section>
    </div>
  );
};

export default Overview;
