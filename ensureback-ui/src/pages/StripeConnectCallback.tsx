import { useEffect, useMemo, useState } from 'react';
import { isAxiosError } from 'axios';
import { useLocation, useNavigate } from 'react-router-dom';

import axiosClient from '@/api/axiosClient';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const StripeConnectCallback = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { refreshMerchantStatus, setSessionFromToken } = useAuth();
  const [statusMessage, setStatusMessage] = useState('Finalizing your Stripe connection...');
  const [showRetry, setShowRetry] = useState(false);

  const searchParams = useMemo(() => {
    const params = new URLSearchParams(location.search ?? window.location.search ?? '');
    return Object.fromEntries(params.entries());
  }, [location.search]);

  useEffect(() => {
    if (!searchParams.state) {
      setStatusMessage('Missing Stripe session state. Please restart the connection.');
      setShowRetry(true);
      return;
    }

    const finalize = async () => {
      setShowRetry(false);
      try {
        const response = await axiosClient.get('/stripe/callback', {
          params: searchParams,
          headers: {
            'X-Requested-With': 'XMLHttpRequest',
            Accept: 'application/json',
          },
        });

        const rawMessage = response.data?.message;
        if (typeof rawMessage === 'string' && rawMessage.trim().length > 0) {
          setStatusMessage(rawMessage);
        } else {
          setStatusMessage('Verifying your account details...');
        }

        const redirectHint: unknown = response.data?.redirectUrl;
        let cleanedRedirect: string | null = null;

        if (typeof redirectHint === 'string' && redirectHint.trim().length > 0) {
          const redirectUrl = new URL(redirectHint, window.location.origin);
          if (redirectUrl.origin !== window.location.origin) {
            window.location.replace(redirectUrl.href);
            return;
          }

          const tokenParam = redirectUrl.searchParams.get('token');
          if (tokenParam) {
            setSessionFromToken(tokenParam);
            redirectUrl.searchParams.delete('token');
          }

          const path = redirectUrl.pathname || '/';
          const search = redirectUrl.search;
          const hash = redirectUrl.hash;
          cleanedRedirect = `${path}${search}${hash}`;
        }

        try {
          const status = await refreshMerchantStatus();
          if (status?.isIntegrated) {
            setStatusMessage('Sending you to your merchant dashboard...');
            navigate('/merchant/dashboard', { replace: true });
            return;
          }

          if (status && !status.isIntegrated) {
            setStatusMessage("Let's set up your Stripe integration.");
            navigate('/integration-wizard', {
              replace: true,
              state: { showSetupBanner: true, setupMessage: "Let's set up your Stripe integration." },
            });
            return;
          }
        } catch (statusError) {
          console.error('Unable to refresh merchant status after Stripe callback', statusError);
        }

        if (cleanedRedirect) {
          setStatusMessage('Finishing up...');
          navigate(cleanedRedirect, { replace: true });
          return;
        }

        navigate('/merchant/dashboard', { replace: true });
      } catch (error) {
        if (isAxiosError(error)) {
          const raw = error.response?.data;
          const fallback = error.message ?? 'Unable to finalize Stripe Connect login. Please try again.';
          if (typeof raw === 'string') {
            setStatusMessage(raw || fallback);
          } else if (raw && typeof raw === 'object') {
            const detail = (raw as Record<string, unknown>).detail;
            if (typeof detail === 'string' && detail.trim().length > 0) {
              setStatusMessage(detail);
            } else {
              const maybeMessage = (raw as Record<string, unknown>).message;
              if (typeof maybeMessage === 'string' && maybeMessage.trim().length > 0) {
                setStatusMessage(maybeMessage);
              } else {
                setStatusMessage(fallback);
              }
            }
          } else {
            setStatusMessage(fallback);
          }
        } else {
          setStatusMessage('Unable to finalize Stripe Connect login. Please try again.');
        }
        setShowRetry(true);
      }
    };

    void finalize();
  }, [navigate, refreshMerchantStatus, searchParams, setSessionFromToken]);

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
