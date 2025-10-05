import type { ReactNode } from 'react';
import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Menu } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export interface SidebarItem {
  label: string;
  to: string;
  icon?: ReactNode;
  end?: boolean;
}

interface SidebarProps {
  title: ReactNode;
  items: SidebarItem[];
  footer?: ReactNode;
  className?: string;
}

export const Sidebar = ({ title, items, footer, className }: SidebarProps) => {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);

  const activeClass = 'bg-primary/10 text-primary';

  return (
    <aside className={cn('lg:w-60 lg:flex lg:flex-col', className)}>
      <div className="flex items-center justify-between rounded-2xl border border-muted bg-card px-4 py-3 shadow-sm lg:hidden">
        <div className="text-sm font-semibold text-foreground">{title}</div>
        <Button
          type="button"
          size="sm"
          variant="outline"
          aria-expanded={isOpen}
          aria-controls="sidebar-mobile"
          onClick={() => setIsOpen((prev) => !prev)}
        >
          <Menu className="h-4 w-4" aria-hidden="true" />
        </Button>
      </div>
      <motion.nav
        id="sidebar-mobile"
        aria-label="Main navigation"
        initial={false}
        animate={isOpen ? { height: 'auto', opacity: 1 } : { height: 0, opacity: 0 }}
        className="overflow-hidden lg:hidden"
      >
        <ul className="mt-2 space-y-1 rounded-2xl border border-muted bg-card p-2 shadow-sm">
          {items.map((item) => {
            const isActive = item.end ? location.pathname === item.to : location.pathname.startsWith(item.to);
            return (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive: linkActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-2xl px-3 py-2 text-sm font-medium text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                      (linkActive || isActive) && activeClass
                    )
                  }
                  onClick={() => setIsOpen(false)}
                >
                  {item.icon && <span className="text-muted-foreground">{item.icon}</span>}
                  <span>{item.label}</span>
                </NavLink>
              </li>
            );
          })}
        </ul>
      </motion.nav>

      <div className="hidden h-full flex-col gap-6 rounded-2xl border border-muted bg-card p-6 shadow-sm lg:flex">
        <div className="text-sm font-semibold text-foreground">{title}</div>
        <ul className="space-y-1">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-2xl px-3 py-2 text-sm font-medium text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                    isActive && activeClass
                  )
                }
              >
                {item.icon && <span className="text-muted-foreground">{item.icon}</span>}
                <span>{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
        {footer && <div className="mt-auto text-xs text-muted-foreground">{footer}</div>}
      </div>
    </aside>
  );
};
