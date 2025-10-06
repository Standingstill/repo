import { PropsWithChildren, useEffect } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '@/hooks/useAuth';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

const SETUP_MESSAGE = "Let's set up your Stripe integration.";

const LoadingState = ({ message }: { message: string }) => (
  <div className="flex min-h-[60vh] items-center justify-center px-4 text-muted-foreground">{message}</div>
);

export const MerchantGate = () => {
  const navigate = useNavigate();
  const { role, hasCheckedIntegration, isCheckingIntegration, isIntegrated, integrationError, checkIntegrationStatus } = useAuth();
  const errorMessage =
    integrationError ?? 'We ran into an issue while checking your integration status. Retry the request or contact support if the problem continues.';

  useEffect(() => {
    if (role !== 'MERCHANT' || integrationError) {
      return;
    }

    if (!hasCheckedIntegration || isCheckingIntegration) {
      return;
    }

    if (isIntegrated === true) {
      navigate('/merchant/dashboard', { replace: true });
    } else if (isIntegrated === false) {
      navigate('/integration-wizard', {
        replace: true,
        state: { showSetupBanner: true, setupMessage: SETUP_MESSAGE },
      });
    }
  }, [hasCheckedIntegration, integrationError, isCheckingIntegration, isIntegrated, navigate, role]);

  if (integrationError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load account status</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => checkIntegrationStatus({ force: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  return <LoadingState message="Checking your Stripe integration..." />;
};

export const RequireMerchantIntegration = ({ children }: PropsWithChildren) => {
  const { hasCheckedIntegration, isCheckingIntegration, isIntegrated, integrationError, checkIntegrationStatus } = useAuth();
  const errorMessage =
    integrationError ?? "We couldn't confirm your integration status. Try refreshing the page or reconnecting your Stripe account.";

  if (integrationError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load dashboard</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => checkIntegrationStatus({ force: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  if (!hasCheckedIntegration || isCheckingIntegration || isIntegrated === null) {
    return <LoadingState message="Loading dashboard..." />;
  }

  if (!isIntegrated) {
    return <Navigate to="/integration-wizard" replace state={{ showSetupBanner: true, setupMessage: SETUP_MESSAGE }} />;
  }

  return <>{children}</>;
};

export const IntegrationWizardGuard = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const { hasCheckedIntegration, isCheckingIntegration, isIntegrated, integrationError, checkIntegrationStatus } = useAuth();
  const errorMessage = integrationError ?? 'Please retry in a few moments or contact support if the issue persists.';

  if (integrationError) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
        <Alert variant="destructive" className="max-w-md text-left">
          <AlertTitle>Unable to load integration wizard</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
        <Button onClick={() => checkIntegrationStatus({ force: true }).catch(() => undefined)}>Retry</Button>
      </div>
    );
  }

  if (!hasCheckedIntegration || isCheckingIntegration || isIntegrated === null) {
    return <LoadingState message="Preparing integration wizard..." />;
  }

  if (isIntegrated) {
    return <Navigate to="/merchant/dashboard" replace state={location.state} />;
  }

  return <>{children}</>;
};
