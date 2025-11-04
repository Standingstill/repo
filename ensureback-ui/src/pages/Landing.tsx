import { useCallback } from 'react';

import { Hero } from '@/components/marketing/Hero';
import { FAQ } from '@/components/marketing/FAQ';
import { ProcessFlow } from '@/components/marketing/ProcessFlow';
import { ValueProps } from '@/components/marketing/ValueProps';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/useAuth';

const Landing = () => {
  const { initiateConnect, isInitiating } = useAuth();

  const handleGetStarted = useCallback(() => {
    void initiateConnect('/integration-wizard').catch((error) => {
      console.error('Unable to start Stripe Connect onboarding', error);
    });
  }, [initiateConnect]);

  const handleBookDemo = useCallback(() => {
    window.location.href = 'mailto:sales@ensureback.com?subject=EnsureBack%20demo%20request';
  }, []);

  return (
    <div className="flex min-h-screen flex-col">
      <main className="flex-1">
        <Hero onBookDemo={handleBookDemo} onGetStarted={handleGetStarted} isLoading={isInitiating} />
        <ValueProps />
        <section className="px-4 pb-24">
          <div className="mx-auto max-w-6xl rounded-2xl border border-muted bg-card px-8 py-10 text-center">
            <p className="text-base font-medium text-muted-foreground">20-minute setup &bull; zero code changes to backend UI</p>
          </div>
        </section>
        <ProcessFlow />
        <FAQ />
      </main>

      <footer className="border-t bg-background px-4 py-10 text-sm text-muted-foreground">
        <div className="mx-auto flex max-w-6xl flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
          <p>&copy; {new Date().getFullYear()} EnsureBack. All rights reserved.</p>
          <nav className="flex flex-wrap items-center gap-4">
            <a href="/developer" className="transition-colors hover:text-foreground">
              Docs
            </a>
            <a href="/security" className="transition-colors hover:text-foreground">
              Security
            </a>
            <a href="/status" className="transition-colors hover:text-foreground">
              Status
            </a>
            <a href="/privacy" className="transition-colors hover:text-foreground">
              Privacy
            </a>
            <a href="/terms" className="transition-colors hover:text-foreground">
              Terms
            </a>
            <Button asChild variant="link" size="sm" className="px-0 text-muted-foreground hover:text-foreground">
              <a href="mailto:contact@ensureback.com">Contact</a>
            </Button>
          </nav>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
