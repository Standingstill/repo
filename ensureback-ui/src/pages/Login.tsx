import { FormEvent, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/hooks/useAuth';

const Login = () => {
  const navigate = useNavigate();
  const { isAuthenticated, isLoggingIn, loginError, login } = useAuth();
  const [email, setEmail] = useState('demo@ensureback.test');
  const [password, setPassword] = useState('demo-password-hash');

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      await login({ email, password });
      navigate('/dashboard');
    } catch (error) {
      console.error('Login failed', error);
    }
  };

  const renderError = () => {
    if (!loginError) {
      return null;
    }

    if (isAxiosError(loginError)) {
      const message = loginError.response?.data?.message ?? 'Invalid credentials';
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
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="text-sm font-medium">
                Password
              </label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                autoComplete="current-password"
              />
            </div>

            {renderError()}

            <Button type="submit" className="w-full" disabled={isLoggingIn}>
              {isLoggingIn ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;
