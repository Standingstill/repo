import type { ReactNode } from 'react';

import { cn } from '@/lib/utils';

interface StatPillProps {
  children: ReactNode;
  tone?: 'default' | 'info' | 'success' | 'danger';
  className?: string;
}

const tones: Record<NonNullable<StatPillProps['tone']>, string> = {
  default: 'bg-muted text-muted-foreground',
  info: 'bg-info/10 text-info',
  success: 'bg-success/10 text-success',
  danger: 'bg-danger/10 text-danger'
};

export const StatPill = ({ children, tone = 'default', className }: StatPillProps) => (
  <span className={cn('inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold', tones[tone], className)}>
    {children}
  </span>
);
