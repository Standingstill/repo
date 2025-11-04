import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';

type ToastVariant = 'default' | 'destructive' | 'success' | 'info';

export interface Toast {
  id: string;
  title?: string;
  description?: string;
  variant?: ToastVariant;
  durationMs?: number;
}

interface ToastContextShape {
  notify: (t: Omit<Toast, 'id'>) => void;
  success: (title: string, description?: string) => void;
  error: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
}

const ToastContext = createContext<ToastContextShape | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const idRef = useRef(0);

  const remove = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const notify = useCallback((t: Omit<Toast, 'id'>) => {
    const id = `${Date.now()}_${idRef.current++}`;
    const toast: Toast = { id, durationMs: 2500, variant: 'default', ...t };
    setToasts((prev) => [...prev, toast]);
    const timeout = setTimeout(() => remove(id), toast.durationMs);
    // Avoid Node types; rely on browser env
    // @ts-ignore
    timeout && undefined;
  }, [remove]);

  const success = useCallback((title: string, description?: string) => notify({ title, description, variant: 'success' }), [notify]);
  const error = useCallback((title: string, description?: string) => notify({ title, description, variant: 'destructive' }), [notify]);
  const info = useCallback((title: string, description?: string) => notify({ title, description, variant: 'info' }), [notify]);

  const value = useMemo<ToastContextShape>(() => ({ notify, success, error, info }), [notify, success, error, info]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toaster toasts={toasts} onClose={remove} />
    </ToastContext.Provider>
  );
};

export const useToast = (): ToastContextShape => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
};

export const Toaster: React.FC<{ toasts: Toast[]; onClose: (id: string) => void }> = ({ toasts, onClose }) => {
  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={[
            'pointer-events-auto rounded-md border p-3 shadow-md bg-background',
            t.variant === 'destructive' ? 'border-red-400/50' : '',
            t.variant === 'success' ? 'border-emerald-400/50' : '',
            t.variant === 'info' ? 'border-blue-400/50' : '',
          ].join(' ')}
        >
          {t.title && <p className="text-sm font-medium">{t.title}</p>}
          {t.description && <p className="mt-0.5 text-xs text-muted-foreground">{t.description}</p>}
          <button onClick={() => onClose(t.id)} className="mt-2 text-xs text-muted-foreground hover:text-foreground">Dismiss</button>
        </div>
      ))}
    </div>
  );
};

