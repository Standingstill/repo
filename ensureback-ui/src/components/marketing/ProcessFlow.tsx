import { motion, useReducedMotion } from 'framer-motion';
import { ArrowRight, CheckCircle2, ClipboardCheck, Inbox, Shuffle } from 'lucide-react';

import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const steps = [
  {
    title: 'Payment received (Stripe)',
    description: 'Stripe charge succeeds and EnsureBack captures the event stream instantly.',
    icon: Inbox
  },
  {
    title: 'EnsureBack evaluates rules',
    description: 'Buyer, merchant, and payment metadata are scored against protection rules.',
    icon: Shuffle
  },
  {
    title: 'Case created (buyer/merchant notified)',
    description: 'Stakeholders receive the context they need in their existing workflows.',
    icon: ArrowRight
  },
  {
    title: 'Action taken (verify, refund, evidence)',
    description: 'Operators choose guided actions or launch automation playbooks.',
    icon: ClipboardCheck
  },
  {
    title: 'Resolved and synced to Stripe',
    description: 'Results write back to Stripe for clean reconciliation and reporting.',
    icon: CheckCircle2
  }
];

export const ProcessFlow = () => {
  const prefersReducedMotion = useReducedMotion();

  return (
    <section id="process-flow" className="bg-secondary/50 px-4 py-24">
      <div className="mx-auto max-w-6xl space-y-10">
        <header className="space-y-4 text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
            How protection flows through Stripe
          </h2>
          <p className="mx-auto max-w-2xl text-base text-muted-foreground">
            The process is transparent for operations teams and remains native to your existing Stripe integration.
          </p>
        </header>
        <div className="grid gap-6 lg:grid-cols-5">
          {steps.map((step, index) => {
            const Icon = step.icon;
            return (
              <motion.div
                key={step.title}
                initial={prefersReducedMotion ? undefined : { opacity: 0, y: 24 }}
                whileInView={prefersReducedMotion ? undefined : { opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.3 }}
                transition={{ delay: index * 0.08, duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
              >
                <Card className="h-full border border-muted">
                  <CardHeader className="space-y-4">
                    <span className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                      <Icon className="h-5 w-5" aria-hidden="true" />
                    </span>
                    <CardTitle className="text-lg leading-tight">{index + 1}. {step.title}</CardTitle>
                    <CardDescription className="text-sm leading-relaxed">{step.description}</CardDescription>
                  </CardHeader>
                </Card>
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
