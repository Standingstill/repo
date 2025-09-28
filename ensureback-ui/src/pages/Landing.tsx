import { ArrowRight, ShieldCheck, Wallet } from 'lucide-react';
import { Link } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const howItWorks = [
  {
    title: 'Customer pays via Stripe',
    description: 'Embed EnsureBack into your Stripe Checkout or Payment Links to keep funds protected from chargebacks.',
    icon: <Wallet className="h-6 w-6 text-primary" />
  },
  {
    title: 'Funds held in escrow',
    description: 'EnsureBack keeps payouts in managed escrow accounts until delivery, reducing risk for both sides.',
    icon: <ShieldCheck className="h-6 w-6 text-primary" />
  },
  {
    title: 'Disputes resolved fairly',
    description: 'Collaborative tooling allows buyers and sellers to negotiate, upload evidence, and settle in-app.',
    icon: <ArrowRight className="h-6 w-6 text-primary" />
  }
];

const Landing = () => {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <main className="flex-1">
        <section className="mx-auto flex max-w-6xl flex-col items-start gap-6 px-4 pb-20 pt-24 text-left md:flex-row md:items-center md:justify-between">
          <div className="max-w-2xl space-y-6">
            <span className="inline-flex items-center rounded-full border border-primary/30 bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-wider text-primary">
              Buyer protection for Stripe-first merchants
            </span>
            <h1 className="text-4xl font-semibold tracking-tight md:text-5xl">
              Get Rid of PayPal; Offer Buyer Protection through Stripe
            </h1>
            <p className="text-lg text-muted-foreground">
              EnsureBack gives your marketplace PayPal-grade protection with Stripe-native experiences, real-time dispute tooling,
              and instant onboarding.
            </p>
            <div className="flex flex-col gap-3 sm:flex-row">
              <Button asChild size="lg">
                <Link to="/login" className="flex items-center gap-2">
                  Get Started Free
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
              <Button asChild variant="outline" size="lg">
                <a href="#pricing">See Pricing</a>
              </Button>
            </div>
          </div>
          <div className="hidden max-w-sm rounded-3xl border bg-card p-6 shadow-xl md:block">
            <div className="space-y-4">
              <h2 className="text-xl font-semibold">Stripe Connect for Escrow</h2>
              <p className="text-sm text-muted-foreground">
                Provide a seamless onboarding flow. Invite merchants, verify identity, and start protecting payments instantly.
              </p>
              <Button asChild variant="secondary" size="lg">
                <a
                  href="https://connect.stripe.com/oauth/authorize"
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-2"
                >
                  Launch Connect Onboarding
                  <ArrowRight className="h-4 w-4" />
                </a>
              </Button>
            </div>
          </div>
        </section>

        <section id="how-it-works" className="bg-muted/40 py-16">
          <div className="mx-auto max-w-6xl px-4">
            <div className="mb-10 max-w-2xl space-y-3">
              <h2 className="text-3xl font-semibold tracking-tight">How it works</h2>
              <p className="text-muted-foreground">
                EnsureBack combines Stripe payments, escrow management, and collaborative dispute tooling into a single experience.
              </p>
            </div>
            <div className="grid gap-6 md:grid-cols-3">
              {howItWorks.map((step) => (
                <Card key={step.title}>
                  <CardHeader className="space-y-3">
                    <div className="rounded-full bg-primary/10 p-3 text-primary">{step.icon}</div>
                    <CardTitle className="text-xl">{step.title}</CardTitle>
                    <CardDescription>{step.description}</CardDescription>
                  </CardHeader>
                </Card>
              ))}
            </div>
          </div>
        </section>

        <section id="pricing" className="py-16">
          <div className="mx-auto max-w-5xl px-4 text-center">
            <h2 className="text-3xl font-semibold">Simple, predictable pricing</h2>
            <p className="mt-4 text-muted-foreground">
              Match PayPal costs while staying fully in the Stripe ecosystem.
            </p>
            <Card className="mx-auto mt-8 max-w-xl border-primary/40">
              <CardHeader>
                <CardTitle className="text-4xl font-semibold">0.59% + $0.19</CardTitle>
                <CardDescription>per transaction processed through EnsureBack</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2 text-sm text-muted-foreground">
                <p>Buyer dispute coverage and escrow included.</p>
                <p>No monthly fees. Volume discounts start at $1M GMV.</p>
              </CardContent>
              <div className="flex items-center justify-center gap-3 pb-6">
                <Button asChild size="lg">
                  <Link to="/login">Get Started</Link>
                </Button>
                <Button asChild variant="outline" size="lg">
                  <a href="mailto:sales@ensureback.com">Talk to sales</a>
                </Button>
              </div>
            </Card>
          </div>
        </section>

        <section className="bg-primary py-16 text-primary-foreground">
          <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-6 px-4 text-center md:flex-row md:text-left">
            <div className="space-y-3">
              <h2 className="text-3xl font-semibold">Stripe Connect onboarding for merchants</h2>
              <p className="text-primary-foreground/80">
                Let sellers onboard instantly with prebuilt Stripe Connect flows. EnsureBack syncs account capabilities and payout
                schedules automatically.
              </p>
            </div>
            <Button asChild size="lg" variant="secondary" className="text-base">
              <a
                href="https://connect.stripe.com/oauth/authorize"
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-2"
              >
                Connect with Stripe
                <ArrowRight className="h-4 w-4" />
              </a>
            </Button>
          </div>
        </section>
      </main>

      <footer className="border-t bg-muted/40 py-8 text-sm text-muted-foreground">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 md:flex-row">
          <p>© {new Date().getFullYear()} EnsureBack. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <Link to="/developer" className="transition-colors hover:text-foreground">
              Docs
            </Link>
            <a href="/terms" className="transition-colors hover:text-foreground">
              Terms
            </a>
            <a href="/privacy" className="transition-colors hover:text-foreground">
              Privacy
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
