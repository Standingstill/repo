import { PropsWithChildren, useEffect } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '@/hooks/useAuth';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

const SETUP_MESSAGE = "Let's set up your Stripe integration.";

export const MerchantGate = () => {
  const navigate = useNavigate();
  const { merchantStatus, isMerchantStatusLoading, merchantStatusError, refreshMerchantStatus } = useAuth();
  const errorMessage = merchantStatusError?.message ?? 'We ran into an issue while checking your integration status. Retry the request or contact support if the problem continues.';

  useEffect(() => {
    if (isMerchantStatusLoading || !merchantStatus) {
      return;
    }

    if (merchantStatus.isIntegrated) {
      navigate('/merchant/dashboard', { replace: true });
    } else {
      navigate('/integration-wizard', {
        replace: true,
        state: { showSetupBanner: true, setupMessage: SETUP_MESSAGE },
      });
    }
  }, [isMerchantStatusLoading, merchantStatus, navigate]);

  if (merchantStatusError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load account status</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => refreshMerchantStatus({ bypassManual: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  return (
    <div className="flex min-h-[60vh] items-center justify-center px-4 text-muted-foreground">
      Checking your Stripe integration...
    </div>
  );
};

export const RequireMerchantIntegration = ({ children }: PropsWithChildren) => {
  const { merchantStatus, isMerchantStatusLoading, merchantStatusError, refreshMerchantStatus } = useAuth();
  const errorMessage = merchantStatusError?.message ?? "We couldn't confirm your integration status. Try refreshing the page or reconnecting your Stripe account.";

  if (merchantStatusError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load dashboard</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => refreshMerchantStatus({ bypassManual: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  if (isMerchantStatusLoading || !merchantStatus) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-muted-foreground">
        Loading dashboard...
      </div>
    );
  }

  if (!merchantStatus.isIntegrated) {
    return <Navigate to="/integration-wizard" replace state={{ showSetupBanner: true, setupMessage: SETUP_MESSAGE }} />;
  }

  return <>{children}</>;
};

export const IntegrationWizardGuard = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const { merchantStatus, isMerchantStatusLoading, merchantStatusError, refreshMerchantStatus } = useAuth();
  const errorMessage = merchantStatusError?.message ?? 'Please retry in a few moments or contact support if the issue persists.';

  if (merchantStatusError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load integration wizard</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => refreshMerchantStatus({ bypassManual: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  if (isMerchantStatusLoading || !merchantStatus) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-muted-foreground">
        Preparing integration wizard...
      </div>
    );
  }

  if (merchantStatus.isIntegrated) {
    return <Navigate to="/merchant/dashboard" replace state={location.state} />;
  }

  return <>{children}</>;
};
