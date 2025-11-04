import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import axiosClient, { buildAuthorizationHeader, readStoredToken } from '@/api/axiosClient';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Progress } from '@/components/ui/progress';
import WizardStep from '@/components/WizardStep';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/components/ui/toast';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Copy, Check } from 'lucide-react';

type UUID = string;

interface ApiKeyDto { id: UUID; createdAt: string; revoked: boolean }
interface WebhookEventDto { id: UUID; eventType: string; timestamp: string; delivered: boolean; payload: string }
interface WebhookStatusDto { registered: boolean; verified: boolean; url?: string | null; recentEvents: WebhookEventDto[] }
interface StripeStatusDto { connected: boolean; accountId?: string | null; connectedAt?: string | null }
interface StepDto { id: string; label: string; completed: boolean; completedAt?: string | null; description: string }
interface WizardStatus {
  merchantId: UUID;
  stripeConnect: StepDto;
  apiKey: StepDto;
  webhook: StepDto;
  verification: StepDto;
  complete: boolean;
  updatedAt?: string;
  stripeStatus: StripeStatusDto;
  webhookStatus: WebhookStatusDto;
  apiKeys: ApiKeyDto[];
}

interface ApiKeyCreateResponse { id: UUID; apiKey: string; signingSecret: string; createdAt: string }
interface ApiKeyCreateResult { apiKey: ApiKeyCreateResponse; status: WizardStatus }

interface WebhookTestResponse { eventId: UUID; delivered: boolean; timestamp: string }
interface WebhookTestResult { delivery: WebhookTestResponse; status: WizardStatus }

const IntegrationWizard = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { initiateConnect } = useAuth();
  const { success: toastSuccess, error: toastError, info: toastInfo } = useToast();

  const locationState = location.state as { showSetupBanner?: boolean; setupMessage?: string } | undefined;
  const showSetupBanner = Boolean(locationState?.showSetupBanner);
  const setupBannerMessage = locationState?.setupMessage ?? "Let's set up your Stripe integration.";

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<WizardStatus | null>(null);
  const [webhookUrl, setWebhookUrl] = useState('');
  const [showReconnect, setShowReconnect] = useState(false);

  // Create key modal
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [createdApiKey, setCreatedApiKey] = useState<ApiKeyCreateResponse | null>(null);
  const [copiedField, setCopiedField] = useState<'apiKey' | 'signingSecret' | null>(null);
  const [revokingId, setRevokingId] = useState<string | null>(null);

  const refetchStatus = useCallback(async () => {
    setError(null);
    try {
      const res = await axiosClient.get<WizardStatus>('/developer/wizard/status');
      setStatus(res.data);
      setWebhookUrl(res.data.webhookStatus?.url || '');
    } catch (e) {
      console.error('Failed to load wizard status', e);
      if ((e as any)?.response?.status === 401) {
        setError('Session expired, please reconnect Stripe.');
        setShowReconnect(true);
      } else {
        setError('Unable to load wizard status.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refetchStatus();
  }, [refetchStatus]);

  useEffect(() => {
    if (status?.complete) {
      navigate('/merchant/dashboard', { replace: true, state: { fromIntegrationWizard: true } });
    }
  }, [navigate, status?.complete]);

  const stepsCompleted = useMemo(() => {
    if (!status) return 0;
    return [status.stripeConnect.completed, status.apiKey.completed, status.webhook.completed, status.verification.completed].filter(Boolean).length;
  }, [status]);
  const progress = useMemo(() => Math.round(((stepsCompleted || 0) / 4) * 100), [stepsCompleted]);

  const onConnectStripe = useCallback(() => {
    void initiateConnect('/integration-wizard');
  }, [initiateConnect]);

  const onGenerateKey = useCallback(async () => {
    setError(null);
    try {
      // Don't send any custom headers - let the cookie handle authentication
      const res = await axiosClient.post<ApiKeyCreateResult>('/developer/wizard/api-keys');
      setCreatedApiKey(res.data.apiKey);
      setShowKeyModal(true);
      setStatus(res.data.status);
      toastSuccess('API key created', 'Copy and store the credentials securely.');
    } catch (e) {
      console.error('Failed to create API key', e);
      setError('Unable to create API key.');
      toastError('Unable to create API key');
    }
  }, [toastSuccess, toastError]);

  const onRegisterWebhook = useCallback(async () => {
    setError(null);
    try {
      const res = await axiosClient.post<WizardStatus>('/developer/wizard/webhook/register', { url: webhookUrl });
      setStatus(res.data);
      const verified = res.data.webhookStatus?.verified;
      if (verified) {
        toastSuccess('Webhook verified');
      } else {
        toastInfo('Webhook saved', 'We could not verify delivery yet.');
      }
    } catch (e) {
      console.error('Failed to register webhook', e);
      setError('Unable to register webhook. Ensure the URL is valid and reachable.');
      toastError('Unable to register webhook');
    }
  }, [webhookUrl]);

  const onSendTest = useCallback(async () => {
    setError(null);
    try {
      const res = await axiosClient.get<WebhookTestResult>('/developer/wizard/webhook/test');
      setStatus(res.data.status);
      if (res.data.delivery?.delivered) {
        toastSuccess('Test event delivered');
      } else {
        toastInfo('Test event queued');
      }
    } catch (e) {
      console.error('Failed to send test event', e);
      setError('Unable to send test webhook event.');
      toastError('Unable to send test event');
    }
  }, []);

  const onRevokeKey = useCallback(async (id: string) => {
    setRevokingId(id);
    setError(null);
    try {
      const res = await axiosClient.delete<WizardStatus>(`/developer/wizard/api-keys/${id}`);
      setStatus(res.data);
      toastSuccess('API key revoked');
    } catch (e) {
      console.error('Failed to revoke API key', e);
      setError('Unable to revoke API key.');
      toastError('Unable to revoke API key');
    } finally {
      setRevokingId(null);
    }
  }, []);

  const copyToClipboard = useCallback(async (value: string, which: 'apiKey' | 'signingSecret') => {
    try {
      await navigator.clipboard.writeText(value);
      setCopiedField(which);
      toastSuccess(which === 'apiKey' ? 'API Key copied' : 'Signing Secret copied');
      setTimeout(() => setCopiedField(null), 1500);
    } catch (e) {
      console.error('Clipboard copy failed', e);
    }
  }, []);

  if (loading) {
    return (
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-10">
        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Preparing integration wizard…</CardTitle>
            <CardDescription>Loading your current setup.</CardDescription>
          </CardHeader>
          <CardContent>
            <Progress value={30} />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-4 py-10">
      <div className="space-y-4">
        <Card className="border border-muted">
          <CardHeader className="space-y-2">
            <Badge variant="muted" className="w-max uppercase tracking-[0.3em] text-xs">Integration Wizard</Badge>
            <CardTitle className="text-3xl font-semibold">Launch your EnsureBack integration</CardTitle>
            <CardDescription>
              Follow the guided steps to prepare your EnsureBack environment. Once all steps are complete we will take you straight to your merchant dashboard.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>Progress</span>
              <span>
                {stepsCompleted} / 4 steps
              </span>
            </div>
            <Progress value={progress} />
          </CardContent>
        </Card>

        {error && (
          <Alert variant="destructive">
            <AlertTitle>Something went wrong</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
            {showReconnect && (
              <div className="mt-3">
                <Button size="sm" onClick={() => initiateConnect('/developer')}>Reconnect Stripe</Button>
              </div>
            )}
          </Alert>
        )}

        {showSetupBanner && (
          <Alert className="border-primary/40 bg-primary/10">
            <AlertTitle>Setup required</AlertTitle>
            <AlertDescription>{setupBannerMessage}</AlertDescription>
          </Alert>
        )}
      </div>

      <div className="grid gap-6">
        <WizardStep
          step={1}
          title="Connect Stripe"
          description="Authorize EnsureBack to access your Stripe account using Stripe Connect."
          status={status?.stripeConnect.completed ? 'complete' : 'active'}
        >
          <p>Launch the Stripe Connect OAuth flow and select the account you want to protect with EnsureBack.</p>
          <div className="flex items-center gap-3">
            <Button onClick={onConnectStripe} disabled={Boolean(status?.stripeConnect.completed)}> {status?.stripeConnect.completed ? 'Stripe connected' : 'Connect Stripe'} </Button>
            {status?.stripeStatus.accountId && (
              <span className="text-xs text-muted-foreground">Connected as {status.stripeStatus.accountId}</span>
            )}
          </div>
        </WizardStep>

        <WizardStep
          step={2}
          title="Create API Key"
          description="Create an API key and signing secret for authenticating requests and verifying webhooks."
          status={status?.apiKey.completed ? 'complete' : (status?.stripeConnect.completed ? 'active' : 'pending')}
        >
          <div className="flex flex-wrap items-center gap-3">
            <Button onClick={onGenerateKey} disabled={!status?.stripeConnect.completed}>Generate API Key</Button>
            {!status?.apiKey.completed && (
              <span className="text-xs text-muted-foreground">Shown once. Store it securely.</span>
            )}
          </div>
          <div className="mt-3 text-sm">
            <p className="mb-1 font-medium">Existing keys</p>
            {status?.apiKeys?.length ? (
              <ul className="space-y-1 pl-0 text-muted-foreground">
                {status.apiKeys.map((k) => (
                  <li key={k.id} className="flex items-center justify-between gap-3">
                    <span>
                      {new Date(k.createdAt).toLocaleString()} 
                      · {k.revoked ? 'revoked' : 'active'} · <span className="font-mono">{k.id}</span>
                    </span>
                    {!k.revoked && (
                      <Button size="sm" variant="destructive" disabled={revokingId === k.id} onClick={() => onRevokeKey(k.id)}>
                        {revokingId === k.id ? 'Revoking…' : 'Revoke'}
                      </Button>
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-muted-foreground">No keys yet.</p>
            )}
          </div>
        </WizardStep>

        <WizardStep
          step={3}
          title="Register webhook"
          description="Provide your endpoint to receive EnsureBack events and verify delivery."
          status={status?.webhook.completed ? 'complete' : (status?.apiKey.completed ? 'active' : 'pending')}
        >
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input value={webhookUrl} onChange={(e) => setWebhookUrl(e.target.value)} placeholder="https://example.com/webhook" className="font-mono" />
            <Button variant="outline" onClick={onRegisterWebhook} disabled={!status?.apiKey.completed}>Save & Verify</Button>
            <Button onClick={onSendTest} disabled={!status?.webhookStatus?.registered}>Send test event</Button>
          </div>
          <div className="mt-3 text-sm text-muted-foreground">
            <p>Registered: {status?.webhookStatus?.registered ? 'yes' : 'no'} · Verified: {status?.webhookStatus?.verified ? 'yes' : 'no'}</p>
            {status?.webhookStatus?.recentEvents?.length ? (
              <ul className="mt-2 list-disc pl-4">
                {status.webhookStatus.recentEvents.map((ev) => (
                  <li key={ev.id}>{ev.eventType} · {new Date(ev.timestamp).toLocaleString()} · {ev.delivered ? 'delivered' : 'failed'}</li>
                ))}
              </ul>
            ) : null}
          </div>
        </WizardStep>

        <WizardStep
          step={4}
          title="Complete"
          description="All steps finished. Proceed to your dashboard."
          status={status?.complete ? 'complete' : 'pending'}
        >
          <Button onClick={() => navigate('/merchant/dashboard', { replace: true })} disabled={!status?.complete}>Go to Dashboard</Button>
        </WizardStep>
      </div>

      <Dialog open={showKeyModal} onOpenChange={setShowKeyModal}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Your new credentials</DialogTitle>
            <DialogDescription>These values are shown once. Copy and store them securely.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <p className="text-xs font-medium">API Key (use as Bearer token)</p>
              <div className="flex items-center gap-2">
                <Input readOnly value={createdApiKey?.apiKey ?? ''} className="font-mono" />
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-label="Copy API key"
                  onClick={() => createdApiKey?.apiKey && copyToClipboard(createdApiKey.apiKey, 'apiKey')}
                >
                  {copiedField === 'apiKey' ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </div>
            <div>
              <p className="text-xs font-medium">Signing Secret (verify ensureback-signature)</p>
              <div className="flex items-center gap-2">
                <Input readOnly value={createdApiKey?.signingSecret ?? ''} className="font-mono" />
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-label="Copy signing secret"
                  onClick={() => createdApiKey?.signingSecret && copyToClipboard(createdApiKey.signingSecret, 'signingSecret')}
                >
                  {copiedField === 'signingSecret' ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setShowKeyModal(false)}>Done</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default IntegrationWizard;
