import { useQuery } from '@tanstack/react-query';
import { ExternalLink, PlugZap } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/ui/page-header';

import { fetchMerchantProfile } from './data';

const Integrations = () => {
  const profileQuery = useQuery({ queryKey: ['merchant', 'profile'], queryFn: fetchMerchantProfile });
  const accountId = profileQuery.data?.stripeAccountId ?? 'acct_1234';

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Connections"
        title="Integrations"
        description="EnsureBack sits on top of Stripe. Manage the connection without touching backend code."
      />

      <Card className="max-w-xl border border-muted">
        <CardHeader className="flex flex-row items-start gap-4">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <PlugZap className="h-6 w-6" aria-hidden="true" />
          </span>
          <div>
            <CardTitle>Stripe — Connected</CardTitle>
            <CardDescription>Account ID: {accountId}</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            Webhooks, disputes, and charge events sync in real time. Manage roles directly in Stripe.
          </p>
          <Button type="button" variant="outline" size="sm" className="gap-2">
            <ExternalLink className="h-4 w-4" aria-hidden="true" /> Manage in Stripe
          </Button>
        </CardContent>
      </Card>
    </div>
  );
};

export default Integrations;
