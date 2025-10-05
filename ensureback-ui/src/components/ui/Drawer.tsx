import type { ReactNode } from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { motion } from 'framer-motion';
import { X } from 'lucide-react';

import { Button } from '@/components/ui/button';

interface DrawerProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
}

export const Drawer = ({ open, onClose, title, description, children }: DrawerProps) => {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={(value) => {
      if (!value) onClose();
    }}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-background/60 backdrop-blur-sm" />
        <motion.div
          initial={{ x: '100%' }}
          animate={{ x: open ? 0 : '100%' }}
          transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
          className="fixed inset-y-0 right-0 z-50 flex h-full w-full max-w-md flex-col border-l border-muted bg-background shadow-xl"
        >
          <header className="flex items-start justify-between gap-6 border-b border-muted px-6 py-5">
            <div className="space-y-1">
              <h2 className="text-lg font-semibold text-foreground">{title}</h2>
              {description && <p className="text-sm text-muted-foreground">{description}</p>}
            </div>
            <Button variant="ghost" size="icon" aria-label="Close drawer" onClick={onClose}>
              <X className="h-5 w-5" aria-hidden="true" />
            </Button>
          </header>
          <div className="flex-1 overflow-y-auto px-6 py-6">{children}</div>
        </motion.div>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
};
