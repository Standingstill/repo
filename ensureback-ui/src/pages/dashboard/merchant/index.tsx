import { useCallback, useMemo } from 'react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { BellRing, ClipboardList, CreditCard, FileText, LayoutDashboard, PlugZap, Settings } from 'lucide-react';

import { Sidebar } from '@/components/ui/Sidebar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { useAuth } from '@/hooks/useAuth';

import Alerts from './Alerts';
import Billing from './Billing';
import Integrations from './Integrations';
import MerchantOverview from './Overview';
import Policies from './Policies';
import Receipts from './Receipts';
import MerchantSettings from './Settings';

const MerchantShell = () => {
  const navItems = useMemo(
    () => [
      { label: 'Overview', to: '/merchant/dashboard', icon: <LayoutDashboard className="h-5 w-5" />, end: true },
      { label: 'Alerts', to: '/merchant/dashboard/alerts', icon: <BellRing className="h-5 w-5" /> },
      { label: 'Policies', to: '/merchant/dashboard/policies', icon: <ClipboardList className="h-5 w-5" /> },
      { label: 'Integrations', to: '/merchant/dashboard/integrations', icon: <PlugZap className="h-5 w-5" /> },
      { label: 'Digital Receipts', to: '/merchant/dashboard/receipts', icon: <FileText className="h-5 w-5" /> },
      { label: 'Billing', to: '/merchant/dashboard/billing', icon: <CreditCard className="h-5 w-5" /> },
      { label: 'Settings', to: '/merchant/dashboard/settings', icon: <Settings className="h-5 w-5" /> }
    ],
    []
  );

  const {
    merchantStatus,
    isMerchantStatusLoading,
    merchantStatusError,
    refreshMerchantStatus,
    initiateConnect,
    isInitiating
  } = useAuth();
  const errorMessage = merchantStatusError?.message ?? "We couldn't confirm your integration state. Please retry or contact support if the issue persists.";

  const handleConnect = useCallback(() => {
    void initiateConnect('/merchant/dashboard').catch((error) => {
      console.error('Unable to start Stripe Connect onboarding', error);
    });
  }, [initiateConnect]);

  const handleRetry = useCallback(() => {
    void refreshMerchantStatus({ bypassManual: true }).catch((error) => {
      console.error('Unable to refresh merchant status', error);
    });
  }, [refreshMerchantStatus]);

  const showIntegrationPrompt = !isMerchantStatusLoading && merchantStatus && !merchantStatus.isIntegrated;
  const showLoader = isMerchantStatusLoading || (!merchantStatus && !merchantStatusError);

  let content: JSX.Element;

  if (merchantStatusError) {
    content = (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load merchant status</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={handleRetry}>Retry</Button>
      </div>
    );
  } else if (showLoader) {
    content = (
      <Card className="min-h-[40vh] border-muted bg-muted/40">
        <CardHeader>
          <CardTitle>Preparing your dashboard</CardTitle>
          <CardDescription>Hang tight while we check your Stripe integration status.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          This usually takes just a moment.
        </CardContent>
      </Card>
    );
  } else if (showIntegrationPrompt) {
    content = (
      <Card className="border-primary/50 bg-primary/10">
        <CardHeader className="space-y-2">
          <CardTitle>You haven’t connected your Stripe account yet.</CardTitle>
          <CardDescription>
            Connect Stripe to unlock merchant analytics and case management.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
          <Button onClick={handleConnect} disabled={isInitiating} size="lg">
            Connect Stripe
          </Button>
          <Button variant="ghost" onClick={handleRetry} disabled={isInitiating}>
            Refresh status
          </Button>
        </CardContent>
      </Card>
    );
  } else {
    content = <Outlet />;
  }

  return (
    <div className="relative mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-4 py-10 lg:flex-row">
      <Sidebar
        title={<span className="text-sm font-semibold">EnsureBack Merchant</span>}
        items={navItems}
        footer={<span>Need help? support@ensureback.com</span>}
        className="lg:w-64"
      />
      <div className="flex-1 space-y-6 pb-10">{content}</div>
    </div>
  );
};

const MerchantDashboard = () => (
  <Routes>
    <Route element={<MerchantShell />}>
      <Route index element={<MerchantOverview />} />
      <Route path="alerts" element={<Alerts />} />
      <Route path="policies" element={<Policies />} />
      <Route path="integrations" element={<Integrations />} />
      <Route path="receipts" element={<Receipts />} />
      <Route path="billing" element={<Billing />} />
      <Route path="settings" element={<MerchantSettings />} />
      <Route path="*" element={<Navigate to="." replace />} />
    </Route>
  </Routes>
);

export default MerchantDashboard;
