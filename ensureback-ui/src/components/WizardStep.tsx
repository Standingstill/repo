import type { ReactNode } from 'react';
import { CheckCircle2, Circle } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

type StepStatus = 'pending' | 'active' | 'complete';

interface WizardStepProps {
  step: number;
  title: string;
  description: string;
  status: StepStatus;
  children: ReactNode;
}

const statusCopy: Record<StepStatus, string> = {
  pending: 'Pending',
  active: 'In progress',
  complete: 'Complete',
};

const WizardStep = ({ step, title, description, status, children }: WizardStepProps) => {
  const isComplete = status === 'complete';
  const isActive = status === 'active';

  return (
    <Card
      className={cn(
        'border bg-card transition-shadow duration-200',
        isComplete && 'border-primary/60 shadow-lg shadow-primary/20',
        isActive && !isComplete && 'border-primary/40'
      )}
    >
      <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-1 items-start gap-4">
          <div
            className={cn(
              'flex h-10 w-10 items-center justify-center rounded-full border text-primary',
              isComplete ? 'border-primary bg-primary/10' : isActive ? 'border-primary/40 bg-primary/5' : 'border-muted bg-muted/40'
            )}
          >
            {isComplete ? <CheckCircle2 className="h-5 w-5" /> : <Circle className="h-5 w-5" />}
          </div>
          <div className="space-y-2">
            <Badge variant="muted" className="w-max uppercase tracking-[0.3em]">Step {step}</Badge>
            <CardTitle className="text-xl font-semibold">{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>
        </div>
        <Badge variant={isComplete ? 'success' : 'outline'} className="w-max">
          {statusCopy[status]}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4 text-sm text-muted-foreground">{children}</CardContent>
    </Card>
  );
};

export default WizardStep;
