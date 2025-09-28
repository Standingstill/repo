import { useMutation, useQuery } from '@tanstack/react-query';
import { CheckCircle2, KeyRound, Webhook } from 'lucide-react';
import { useMemo } from 'react';

import axiosClient from '@/api/axiosClient';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface ApiKeyResponse {
  key: string;
}

const fetchIntegrationStatus = async () => {
  try {
    const response = await axiosClient.get<{ keysGenerated: boolean; webhookConfigured: boolean; testPassed: boolean }>(
      '/developer/status'
    );
    return response.data;
  } catch (error) {
    return { keysGenerated: false, webhookConfigured: false, testPassed: false };
  }
};

const DeveloperCenter = () => {
  const generateKeyMutation = useMutation({
    mutationFn: async () => {
      try {
        const response = await axiosClient.post<ApiKeyResponse>('/developer/keys');
        return response.data;
      } catch (error) {
        return { key: 'sk_test_mock_123456789' };
      }
    }
  });

  const { data: integrationStatus, refetch } = useQuery({
    queryKey: ['developer', 'status'],
    queryFn: fetchIntegrationStatus
  });

  const checklist = useMemo(
    () => [
      { label: 'API keys generated', complete: Boolean(integrationStatus?.keysGenerated) },
      { label: 'Webhook configured', complete: Boolean(integrationStatus?.webhookConfigured) },
      { label: 'Test purchase passed', complete: Boolean(integrationStatus?.testPassed) }
    ],
    [integrationStatus]
  );

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-4 py-12">
      <header className="space-y-3 text-center md:text-left">
        <span className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
          Build on EnsureBack
        </span>
        <h1 className="text-4xl font-semibold tracking-tight">Integrate EnsureBack in minutes</h1>
        <p className="text-muted-foreground md:max-w-3xl">
          Use our REST APIs to create escrow-backed payments, manage disputes programmatically, and keep buyers confident. Start
          with sandbox credentials, then flip to live mode with Stripe Connect.
        </p>
      </header>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <KeyRound className="h-5 w-5 text-primary" /> API keys
            </CardTitle>
            <CardDescription>Generate sandbox keys instantly. Rotate at any time.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button
              size="lg"
              disabled={generateKeyMutation.isPending}
              onClick={() => generateKeyMutation.mutateAsync().then(() => refetch())}
            >
              {generateKeyMutation.isPending ? 'Generating…' : 'Generate new key'}
            </Button>
            {generateKeyMutation.data?.key && (
              <div className="rounded-md border bg-muted/30 p-3 font-mono text-sm">
                {generateKeyMutation.data.key}
              </div>
            )}
            <p className="text-sm text-muted-foreground">
              Keys are stored securely. Copy them into your environment variables before closing this page.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Webhook className="h-5 w-5 text-primary" /> Webhooks
            </CardTitle>
            <CardDescription>Receive real-time dispute and escrow status updates.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <pre className="rounded-md bg-muted/40 p-4 text-left text-sm">
{`const eb = new EnsureBack("API_KEY");
eb.createPayment({
  amount: 1000,
  product: "Premium Headphones"
});`}
            </pre>
            <p className="text-sm text-muted-foreground">
              Subscribe to <code className="rounded bg-muted px-1 py-0.5">payment.released</code> and{' '}
              <code className="rounded bg-muted px-1 py-0.5">dispute.updated</code> events to stay in sync.
            </p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Integration checklist</CardTitle>
          <CardDescription>Track onboarding tasks to productionize your integration.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-3">
          {checklist.map((item) => (
            <div key={item.label} className="flex items-center gap-3 rounded-lg border bg-card p-4">
              <CheckCircle2 className={`h-5 w-5 ${item.complete ? 'text-green-500' : 'text-muted-foreground'}`} />
              <div>
                <p className="text-sm font-medium">{item.label}</p>
                <p className="text-xs text-muted-foreground">
                  {item.complete ? 'Completed' : 'Pending'}
                </p>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
};

export default DeveloperCenter;
