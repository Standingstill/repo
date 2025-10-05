import { useId } from 'react';
import { motion, useReducedMotion } from 'framer-motion';

import { Button } from '@/components/ui/button';

const heroVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: (index: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: index * 0.1, duration: 0.6, ease: [0.16, 1, 0.3, 1] }
  })
};

interface HeroProps {
  onBookDemo: () => void;
  onGetStarted: () => void;
  isLoading?: boolean;
}

export const Hero = ({ onBookDemo, onGetStarted, isLoading = false }: HeroProps) => {
  const prefersReducedMotion = useReducedMotion();
  const stripeId = useId();

  return (
    <section className="relative isolate overflow-hidden px-4 pt-20 pb-24 sm:pt-24 lg:pt-28">
      <div className="mx-auto max-w-6xl">
        <div className="grid gap-16 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1fr)] lg:items-center">
          <div className="space-y-8">
            <motion.div initial="hidden" animate="visible" custom={0} variants={heroVariants}>
              <span className="inline-flex items-center rounded-full bg-primary/10 px-4 py-1 text-xs font-medium text-primary">
                Built for Stripe teams
              </span>
            </motion.div>
            <motion.h1
              className="text-4xl font-semibold tracking-tight text-foreground sm:text-5xl lg:text-6xl"
              initial={prefersReducedMotion ? undefined : 'hidden'}
              animate={prefersReducedMotion ? undefined : 'visible'}
              custom={1}
              variants={heroVariants}
            >
              Buyer protection for Stripe payments.
            </motion.h1>
            <motion.p
              className="max-w-2xl text-lg text-muted-foreground sm:text-xl"
              initial={prefersReducedMotion ? undefined : 'hidden'}
              animate={prefersReducedMotion ? undefined : 'visible'}
              custom={2}
              variants={heroVariants}
            >
              Reduce chargebacks and friendly fraud. Automate protection workflows on top of your existing Stripe integration.
            </motion.p>
            <motion.div
              className="flex flex-col gap-3 sm:flex-row"
              initial={prefersReducedMotion ? undefined : 'hidden'}
              animate={prefersReducedMotion ? undefined : 'visible'}
              custom={3}
              variants={heroVariants}
            >
              <Button size="lg" aria-label="Book a demo of EnsureBack" onClick={onBookDemo} disabled={isLoading}>
                Book a demo
              </Button>
              <Button size="lg" variant="outline" aria-label="Get started with EnsureBack" onClick={onGetStarted} disabled={isLoading}>
                Get started
              </Button>
            </motion.div>
            <motion.div
              className="flex items-center gap-3 text-sm text-muted-foreground"
              initial={prefersReducedMotion ? undefined : 'hidden'}
              animate={prefersReducedMotion ? undefined : 'visible'}
              custom={4}
              variants={heroVariants}
            >
              <svg
                aria-labelledby={stripeId}
                className="h-6 w-16 text-foreground"
                role="img"
                viewBox="0 0 72 24"
              >
                <title id={stripeId}>Stripe</title>
                <path
                  d="M8.649 7.405c1.32 0 2.217.209 3.093.523V4.192c-.978-.37-1.956-.563-3.079-.563-3.119 0-5.309 1.623-5.309 4.846 0 3.046 1.965 4.722 4.876 4.722.997 0 1.716-.163 2.382-.407V11.28c-.69.209-1.306.33-2.13.33-1.182 0-1.906-.6-1.906-1.827 0-1.156.773-1.726 2.073-1.726zM24.321 4H21v6.545c0 2.486 1.195 3.44 2.987 3.44 1.266 0 1.978-.226 2.591-.5V11.48c-.484.163-.9.267-1.54.267-.823 0-1.224-.371-1.224-1.29V4zm-5.245.243-1.89.614-.12.03V13.7h3.321V4.243zm13.42 2.515c-.027-1.665-1.14-2.758-3.003-2.758-1.885 0-3.1 1.093-3.1 2.758 0 1.61 1.214 2.58 3.001 2.58h.109c-.037 1.333-.15 1.741-.706 2.127-.523.363-1.419.488-2.088.488-.748 0-1.503-.124-2.223-.335v2.713c.8.175 1.528.264 2.486.264 1.455 0 2.696-.3 3.672-.925 1.21-.774 1.544-2.014 1.544-4.11zM42.87 1.022l-3.32 1.071-.12.03V13.7h3.32V1.022zM51.537 4h-3.32v6.545c0 2.486 1.194 3.44 2.987 3.44 1.266 0 1.978-.226 2.59-.5V11.48c-.484.163-.9.267-1.54.267-.824 0-1.224-.371-1.224-1.29V4zm10.65 6.96c0-2.92-4.01-3.086-4.01-4.4 0-.51.457-.9 1.221-.9.554 0 1.03.162 1.55.435l.479-2.85C60.688 3.036 59.658 2.7 58.38 2.7c-2.587 0-4.273 1.46-4.273 3.547 0 2.882 4.047 3.137 4.047 4.423 0 .613-.523.962-1.308.962-.611 0-1.31-.187-1.983-.55l-.51 2.904c.883.411 1.867.61 3.098.61 2.748 0 4.736-1.446 4.736-3.636z"
                  fill="currentColor"
                />
              </svg>
              <span className="text-sm font-medium text-muted-foreground">Built for Stripe</span>
            </motion.div>
          </div>
          <motion.div
            className="relative rounded-2xl border bg-white/80 p-6 shadow-sm"
            initial={prefersReducedMotion ? undefined : { opacity: 0, y: 30 }}
            animate={prefersReducedMotion ? undefined : { opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1], delay: 0.2 }}
          >
            <div className="grid gap-4 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-muted-foreground">20-minute setup</span>
                <span className="rounded-full bg-success/10 px-3 py-1 text-xs font-medium text-success">
                  zero code changes to backend UI
                </span>
              </div>
              <div className="rounded-2xl bg-secondary p-6">
                <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">Protection snapshot</p>
                <p className="mt-4 text-3xl font-semibold text-foreground">98.3% of disputes resolved</p>
                <p className="mt-2 text-sm text-muted-foreground">
                  Reconcile Stripe chargebacks without rebuilding your workflows.
                </p>
              </div>
              <dl className="grid gap-3 sm:grid-cols-2">
                <div>
                  <dt className="text-xs text-muted-foreground">Live chargebacks</dt>
                  <dd className="text-lg font-semibold text-foreground">4</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Automation playbooks</dt>
                  <dd className="text-lg font-semibold text-foreground">12</dd>
                </div>
              </dl>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
};
