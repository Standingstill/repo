import * as React from 'react';

import { cn } from '@/lib/utils';

const Navbar = React.forwardRef<HTMLElement, React.HTMLAttributes<HTMLElement>>(({ className, ...props }, ref) => (
  <nav ref={ref} className={cn('sticky top-0 z-40 w-full border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/60', className)} {...props} />
));
Navbar.displayName = 'Navbar';

const NavbarContent = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('mx-auto flex h-16 max-w-6xl items-center justify-between px-4', className)} {...props} />
));
NavbarContent.displayName = 'NavbarContent';

const NavbarSection = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('flex items-center gap-4', className)} {...props} />
));
NavbarSection.displayName = 'NavbarSection';

export { Navbar, NavbarContent, NavbarSection };
