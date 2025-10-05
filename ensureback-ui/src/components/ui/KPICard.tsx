import type { ReactNode } from 'react';
import { motion, useReducedMotion } from 'framer-motion';

import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface KPICardProps {
  label: string;
  value: string;
  helper?: string;
  icon?: ReactNode;
  trend?: ReactNode;
  delay?: number;
}

export const KPICard = ({ label, value, helper, icon, trend, delay = 0 }: KPICardProps) => {
  const prefersReducedMotion = useReducedMotion();

  return (
    <motion.div
      initial={prefersReducedMotion ? undefined : { opacity: 0, y: 24 }}
      whileInView={prefersReducedMotion ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1], delay }}
    >
      <Card className="h-full border border-muted">
        <CardHeader className="space-y-5">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">{label}</p>
              <CardTitle className="mt-3 text-3xl font-semibold">{value}</CardTitle>
            </div>
            {icon && <span className="text-primary">{icon}</span>}
          </div>
          {(helper || trend) && (
            <CardDescription className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              {trend}
              {helper && <span>{helper}</span>}
            </CardDescription>
          )}
        </CardHeader>
      </Card>
    </motion.div>
  );
};
