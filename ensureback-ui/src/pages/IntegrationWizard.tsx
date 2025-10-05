import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Progress } from '@/components/ui/progress';
import WizardStep from '@/components/WizardStep';
import { useAuth } from '@/hooks/useAuth';

const MOCK_STRIPE_ACCOUNT_ID = 'acct_mock_1234';
const MOCK_WEBHOOK_URL = 'https://hooks.ensureback.com/stripe/merchant/demo-account';

const IntegrationWizard = () => {
  const connectTimeoutRef = useRef<number | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { merchantStatus, setMerchantStatusManually } = useAuth();

  const locationState = location.state as { showSetupBanner?: boolean; setupMessage?: string } | undefined;
  const showSetupBanner = Boolean(locationState?.showSetupBanner);
  const setupBannerMessage = locationState?.setupMessage ?? "Let's set up your Stripe integration.";

  const [isConnecting, setIsConnecting] = useState(false);
  const [hasConnectedStripe, setHasConnectedStripe] = useState(Boolean(merchantStatus?.isIntegrated));
  const [webhookConfigured, setWebhookConfigured] = useState(false);
  const [testPaymentComplete, setTestPaymentComplete] = useState(false);
  const [hasCopiedWebhook, setHasCopiedWebhook] = useState(false);

  useEffect(() => {
    if (merchantStatus?.isIntegrated) {
      setHasConnectedStripe(true);
    }
  }, [merchantStatus?.isIntegrated]);

  useEffect(() => {
    if (!hasCopiedWebhook) {
      return;
    }
    const timeout = setTimeout(() => setHasCopiedWebhook(false), 2000);
    return () => clearTimeout(timeout);
  }, [hasCopiedWebhook]);

  const completedSteps = useMemo(
    () => [hasConnectedStripe, webhookConfigured, testPaymentComplete].filter(Boolean).length,
    [hasConnectedStripe, webhookConfigured, testPaymentComplete]
  );
  const progress = useMemo(() => Math.round((completedSteps / 3) * 100), [completedSteps]);

  const handleConnectStripe = useCallback(() => {
    if (connectTimeoutRef.current) {
      window.clearTimeout(connectTimeoutRef.current);
    }
    setIsConnecting(true);
    connectTimeoutRef.current = window.setTimeout(() => {
      setHasConnectedStripe(true);
      setMerchantStatusManually({ isIntegrated: true, stripeAccountId: MOCK_STRIPE_ACCOUNT_ID });
      setIsConnecting(false);
    }, 900);
  }, [setMerchantStatusManually]);

  const handleCopyWebhook = useCallback(() => {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      void navigator.clipboard.writeText(MOCK_WEBHOOK_URL).finally(() => setHasCopiedWebhook(true));
      return;
    }
    setHasCopiedWebhook(true);
  }, []);

  useEffect(() => {
    return () => {
      if (connectTimeoutRef.current) {
        window.clearTimeout(connectTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!hasConnectedStripe || !webhookConfigured || !testPaymentComplete) {
      return;
    }
    const timeout = setTimeout(() => {
      setMerchantStatusManually({ isIntegrated: true, stripeAccountId: MOCK_STRIPE_ACCOUNT_ID });
      navigate('/merchant/dashboard', { replace: true, state: { fromIntegrationWizard: true } });
    }, 800);
    return () => clearTimeout(timeout);
  }, [hasConnectedStripe, navigate, setMerchantStatusManually, testPaymentComplete, webhookConfigured]);

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-4 py-10">
      <div className="space-y-4">
        <Card className="border border-muted">
          <CardHeader className="space-y-2">
            <Badge variant="muted" className="w-max uppercase tracking-[0.3em] text-xs">Integration Wizard</Badge>
            <CardTitle className="text-3xl font-semibold">Launch your EnsureBack integration</CardTitle>
            <CardDescription>
              Follow the guided steps to prepare your EnsureBack environment. Once all steps are complete we will take you
              straight to your merchant dashboard.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>Progress</span>
              <span>
                {completedSteps} / 3 steps
              </span>
            </div>
            <Progress value={progress} />
          </CardContent>
        </Card>

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
          status={hasConnectedStripe ? 'complete' : 'active'}
        >
          <p>
            Launch the Stripe Connect OAuth flow and select the account you want to protect with EnsureBack. We will store
            the resulting Stripe account ID securely.
          </p>
          <div className="flex flex-wrap items-center gap-3">
            <Button onClick={handleConnectStripe} disabled={hasConnectedStripe || isConnecting}>
              {hasConnectedStripe ? 'Stripe connected' : isConnecting ? 'Connecting...' : 'Connect Stripe'}
            </Button>
            {!hasConnectedStripe && (
              <span className="text-xs text-muted-foreground">
                We are using a mock response here—no real Stripe data is changed.
              </span>
            )}
            {hasConnectedStripe && (
              <span className="text-xs text-emerald-600">Connected to mock account {MOCK_STRIPE_ACCOUNT_ID}.</span>
            )}
          </div>
        </WizardStep>

        <WizardStep
          step={2}
          title="Set up webhook endpoint"
          description="Forward Stripe dispute events to EnsureBack so we can automate your responses."
          status={!hasConnectedStripe ? 'pending' : webhookConfigured ? 'complete' : 'active'}
        >
          <p>
            Add the following webhook URL to your Stripe Dashboard with the events
            <strong className="ml-1">payment_intent.succeeded</strong> and
            <strong className="ml-1">charge.dispute.created</strong> enabled.
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input value={MOCK_WEBHOOK_URL} readOnly className="font-mono" />
            <Button variant="outline" onClick={handleCopyWebhook}>
              {hasCopiedWebhook ? 'Copied!' : 'Copy URL'}
            </Button>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Button
              onClick={() => setWebhookConfigured(true)}
              disabled={!hasConnectedStripe || webhookConfigured}
              variant="subtle"
            >
              {webhookConfigured ? 'Webhook ready' : 'I added the webhook'}
            </Button>
            {!hasConnectedStripe && (
              <span className="text-xs text-muted-foreground">Complete step 1 to enable this action.</span>
            )}
          </div>
        </WizardStep>

        <WizardStep
          step={3}
          title="Run a test payment"
          description="Verify your setup by simulating a protected transaction."
          status={webhookConfigured ? (testPaymentComplete ? 'complete' : 'active') : 'pending'}
        >
          <p>
            In test mode, charge the Stripe test card <strong>4242 4242 4242 4242</strong> for any amount. Wait a few
            seconds and confirm the event appears in your EnsureBack activity feed.
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>Use the Stripe Dashboard or CLI to create the charge.</li>
            <li>Ensure the charge uses the account you connected in step 1.</li>
            <li>Check that EnsureBack receives the webhook notification.</li>
          </ul>
          <Button
            onClick={() => setTestPaymentComplete(true)}
            disabled={!webhookConfigured || testPaymentComplete}
            size="lg"
          >
            {testPaymentComplete ? 'Test payment confirmed' : 'I ran the test payment'}
          </Button>
          {testPaymentComplete && (
            <p className="text-xs text-emerald-600">Awesome! We will redirect you to the dashboard in just a moment.</p>
          )}
        </WizardStep>
      </div>
    </div>
  );
};

export default IntegrationWizard;



