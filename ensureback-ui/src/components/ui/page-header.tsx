import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

import { cn } from '@/lib/utils';

interface PageHeaderProps {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
  className?: string;
}

export const PageHeader = ({ eyebrow, title, description, actions, className }: PageHeaderProps) => (
  <motion.header
    className={cn('flex flex-col gap-6 rounded-3xl bg-white/70 p-6 shadow-sm shadow-black/5 backdrop-blur lg:flex-row lg:items-center lg:justify-between', className)}
    initial={{ opacity: 0, y: 12 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.35 }}
  >
    <div className="space-y-3">
      {eyebrow && <span className="text-xs uppercase tracking-[0.3em] text-muted-foreground">{eyebrow}</span>}
      <h1 className="text-3xl font-semibold text-foreground lg:text-4xl">{title}</h1>
      {description && <p className="max-w-2xl text-sm text-muted-foreground lg:text-base">{description}</p>}
    </div>
    {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
  </motion.header>
);
