import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { isAxiosError } from 'axios';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/hooks/useAuth';

const Login = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const {
    isAuthenticated,
    isInitiating,
    isCompleting,
    initiateError,
    completeError,
    initiateConnect,
    completeConnect,
  } = useAuth();
  const [email, setEmail] = useState('demo@ensureback.test');
  const [callbackHandled, setCallbackHandled] = useState(false);

  const callbackParams = useMemo(
    () => ({
      code: searchParams.get('code') ?? undefined,
      state: searchParams.get('state') ?? undefined,
      error: searchParams.get('error') ?? undefined,
      errorDescription: searchParams.get('error_description') ?? undefined,
    }),
    [searchParams]
  );

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      const response = await initiateConnect({ email });
      window.location.href = response.authorizationUrl;
    } catch (error) {
      console.error('Login failed', error);
    }
  };

  useEffect(() => {
    if (!callbackParams.state || callbackHandled) {
      return;
    }

    setCallbackHandled(true);

    const executeCallback = async () => {
      try {
        await completeConnect({
          state: callbackParams.state!,
          code: callbackParams.code,
          error: callbackParams.error,
          errorDescription: callbackParams.errorDescription,
        });
        setSearchParams({}, { replace: true });
        navigate('/dashboard');
      } catch (error) {
        console.error('Stripe Connect callback failed', error);
      }
    };

    void executeCallback();
  }, [callbackHandled, callbackParams, completeConnect, navigate, setSearchParams]);

  const renderError = () => {
    const error = completeError ?? initiateError;
    if (!error) {
      return null;
    }

    if (isAxiosError(error)) {
      const message = error.response?.data?.message ?? 'Unable to authenticate with Stripe Connect';
      return <p className="rounded-md bg-red-100 px-3 py-2 text-sm font-medium text-red-700">{message}</p>;
    }

    return (
      <p className="rounded-md bg-red-100 px-3 py-2 text-sm font-medium text-red-700">
        Something went wrong. Please try again.
      </p>
    );
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-muted/60 px-4 py-16">
      <Card className="w-full max-w-md border-primary/20 shadow-lg">
        <CardHeader className="space-y-2 text-center">
          <CardTitle>EnsureBack Portal</CardTitle>
          <CardDescription>Sign in with your EnsureBack merchant credentials.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium">
                Email
              </label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
                disabled={isInitiating || isCompleting}
              />
            </div>

            {renderError()}

            <Button type="submit" className="w-full" disabled={isInitiating || isCompleting}>
              {isCompleting
                ? 'Finalizing Stripe login…'
                : isInitiating
                ? 'Redirecting to Stripe…'
                : 'Sign in with Stripe Connect'}
            </Button>
          </form>
          {isCompleting && (
            <p className="mt-4 text-center text-sm text-muted-foreground">
              Returning from Stripe. Please wait while we verify your account…
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;
