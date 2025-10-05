import { Radar, Sparkles, ShieldCheck } from 'lucide-react';
import { motion, useReducedMotion } from 'framer-motion';

import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const items = [
  {
    title: 'Pre-dispute alerts & monitoring',
    description: 'Listen to Stripe events, flag risky payments, notify the right team.',
    icon: Radar
  },
  {
    title: 'Automated actions',
    description: 'Trigger emails, partial refunds, evidence packages, or policy checks with one click.',
    icon: Sparkles
  },
  {
    title: 'Fewer chargebacks',
    description: 'Operational playbooks that shorten time-to-resolution.',
    icon: ShieldCheck
  }
];

export const ValueProps = () => {
  const prefersReducedMotion = useReducedMotion();

  return (
    <section id="value-props" className="px-4 pb-24">
      <div className="mx-auto max-w-6xl">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item, index) => {
            const Icon = item.icon;
            return (
              <motion.div
                key={item.title}
                initial={prefersReducedMotion ? undefined : { opacity: 0, y: 24 }}
                whileInView={prefersReducedMotion ? undefined : { opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.4 }}
                transition={{ delay: index * 0.1, duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
              >
                <Card className="h-full border border-muted">
                  <CardHeader className="space-y-4">
                    <span className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                      <Icon className="h-5 w-5" aria-hidden="true" />
                    </span>
                    <CardTitle className="text-xl">{item.title}</CardTitle>
                    <CardDescription className="text-base leading-relaxed">{item.description}</CardDescription>
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
