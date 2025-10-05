import { useMemo, useState } from 'react';
import { AlertCircle, Check, Filter, MailPlus } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/ui/page-header';
import { StatPill } from '@/components/ui/StatPill';

const ALERTS = [
  {
    id: 'alert_101',
    type: 'Payment risk',
    headline: 'High-risk BIN detected',
    body: 'Card ending in ••92 triggered friendly fraud heuristics. Review evidence before dispute deadline.',
    openedAt: '2025-10-01T14:32:00.000Z',
    owner: 'Disputes',
    status: 'Open'
  },
  {
    id: 'alert_205',
    type: 'Friendly fraud',
    headline: 'Buyer requested double refund',
    body: 'EnsureBack prevented a duplicate refund on order ord_24680. Confirm resolution with buyer support.',
    openedAt: '2025-09-30T09:18:00.000Z',
    owner: 'Support',
    status: 'Action required'
  },
  {
    id: 'alert_309',
    type: 'Delivery mismatch',
    headline: 'Tracking suggests late delivery',
    body: 'Two shipments have mismatched delivery timestamps. Confirm proof of delivery before case escalates.',
    openedAt: '2025-09-29T19:54:00.000Z',
    owner: 'Operations',
    status: 'Open'
  }
];

const FILTERS = ['Payment risk', 'Friendly fraud', 'Delivery mismatch'] as const;

const STATUS_TONE: Record<string, 'info' | 'danger' | 'success'> = {
  Open: 'info',
  'Action required': 'danger',
  Resolved: 'success'
};

const Alerts = () => {
  const [activeFilter, setActiveFilter] = useState<string>('Payment risk');
  const filteredAlerts = useMemo(
    () => ALERTS.filter((alert) => alert.type === activeFilter),
    [activeFilter]
  );

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Risk signals"
        title="Alerts"
        description="Filter and respond to Stripe-sourced alerts before they become chargebacks."
        actions={
          <Button variant="outline" size="sm">
            <MailPlus className="mr-2 h-4 w-4" /> Subscribe team digest
          </Button>
        }
      />

      <div className="flex flex-wrap gap-2">
        {FILTERS.map((filter) => {
          const isActive = filter === activeFilter;
          return (
            <Button
              key={filter}
              type="button"
              size="sm"
              variant={isActive ? 'default' : 'outline'}
              className="rounded-full"
              onClick={() => setActiveFilter(filter)}
            >
              {isActive ? <Check className="mr-2 h-4 w-4" /> : <Filter className="mr-2 h-4 w-4" />}
              {filter}
            </Button>
          );
        })}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {filteredAlerts.map((alert) => (
          <Card key={alert.id} className="border border-muted">
            <CardHeader className="space-y-3">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <StatPill tone={STATUS_TONE[alert.status] ?? 'info'}>{alert.status}</StatPill>
                  <CardTitle className="mt-3 text-lg">{alert.headline}</CardTitle>
                </div>
                <AlertCircle className="h-6 w-6 text-primary" aria-hidden="true" />
              </div>
              <CardDescription>{alert.body}</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
              <span>Owner: {alert.owner}</span>
              <span>{new Date(alert.openedAt).toLocaleString()}</span>
              <div className="flex gap-2">
                <Button size="sm" variant="outline">
                  View
                </Button>
                <Button size="sm" variant="ghost">
                  Add note
                </Button>
                <Button size="sm">
                  Resolve
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default Alerts;
