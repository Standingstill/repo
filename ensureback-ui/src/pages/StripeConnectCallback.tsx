import { useEffect, useMemo, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { useLocation, useNavigate } from 'react-router-dom';

import axiosClient from '@/api/axiosClient';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const StripeConnectCallback = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { checkIntegrationStatus, setSessionFromToken, integrationError } = useAuth();
  const [statusMessage, setStatusMessage] = useState('Finalizing your Stripe connection...');
  const [showRetry, setShowRetry] = useState(false);
  const hasFinalizedRef = useRef(false);
  const latestIntegrationErrorRef = useRef<string | null>(integrationError);

  const searchParams = useMemo(() => {
    const params = new URLSearchParams(location.search ?? window.location.search ?? '');
    return Object.fromEntries(params.entries());
  }, [location.search]);

  useEffect(() => {
    latestIntegrationErrorRef.current = integrationError;
  }, [integrationError]);

  useEffect(() => {
    if (!searchParams.state) {
      setStatusMessage('Missing Stripe session state. Please restart the connection.');
      setShowRetry(true);
      return;
    }

    if (hasFinalizedRef.current) {
      return;
    }

    hasFinalizedRef.current = true;

    // Perform a full redirect to backend callback so it can set HttpOnly cookie
    const params = new URLSearchParams(searchParams as any).toString();
    const url = `/api/stripe/callback?${params}`;
    window.location.replace(url);
  }, [checkIntegrationStatus, navigate, searchParams, setSessionFromToken]);

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-muted/60 px-4 py-16">
      <Card className="w-full max-w-md border-primary/20 shadow-lg">
        <CardHeader className="space-y-2 text-center">
          <CardTitle>Connecting to EnsureBack</CardTitle>
          <CardDescription>Your Stripe account is being verified and your dashboard is being prepared.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4 text-sm text-center text-muted-foreground">
          <p>{statusMessage}</p>
          {showRetry && (
            <Button variant="outline" onClick={() => navigate('/')}>Try again</Button>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default StripeConnectCallback;
