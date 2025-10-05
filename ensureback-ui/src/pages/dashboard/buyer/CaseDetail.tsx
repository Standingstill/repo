import { useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, UploadCloud } from 'lucide-react';

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

const CaseDetail = () => {
  const params = useParams<{ caseId: string }>();
  const navigate = useNavigate();
  const ordersQuery = useQuery({ queryKey: ['buyer', 'orders'], queryFn: fetchBuyerOrders });
  const order = useMemo(() => ordersQuery.data?.find((item) => item.orderId === params.caseId) ?? null, [ordersQuery.data, params.caseId]);

  if (!order) {
    return (
      <div className="space-y-6">
        <PageHeader eyebrow="Cases" title="Case not found" description="The requested case could not be located." />
        <Button onClick={() => navigate('/dashboard/buyer')} variant="outline">
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" /> Back to overview
        </Button>
      </div>
    );
  }

  const statusMeta = STATUS_LABEL[order.status];

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Case detail"
        title={order.merchantName}
        description={`Order ${order.orderId} · ${formatCurrency(order.total, order.currency)}`}
        actions={
          <Button variant="outline" onClick={() => navigate('/dashboard/buyer')}>
            <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" /> Back to overview
          </Button>
        }
      />

      <Card className="border border-muted">
        <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Status</CardTitle>
            <CardDescription>Latest case update from EnsureBack.</CardDescription>
          </div>
          <StatPill tone={statusMeta.tone}>{statusMeta.label}</StatPill>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-3">
            <h3 className="text-sm font-semibold uppercase tracking-[0.3em] text-muted-foreground">Timeline</h3>
            <ol className="space-y-4 border-l border-muted pl-5 text-sm text-muted-foreground">
              {order.timeline.map((event, index) => (
                <li key={`${order.orderId}-${event}-${index}`}>
                  <div className="ml-[-1.25rem] flex items-center gap-3">
                    <span className="flex h-2.5 w-2.5 items-center justify-center rounded-full bg-primary" aria-hidden="true" />
                    <span className="font-medium text-foreground">{event}</span>
                  </div>
                </li>
              ))}
            </ol>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button size="sm" variant="outline" className="gap-2">
              <UploadCloud className="h-4 w-4" aria-hidden="true" /> Upload evidence
            </Button>
            <Button size="sm" variant="outline" asChild>
              <Link to="mailto:support@ensureback.com">Contact support</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default CaseDetail;
