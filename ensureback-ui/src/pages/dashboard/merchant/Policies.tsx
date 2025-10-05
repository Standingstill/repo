import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import axiosClient from '@/api/axiosClient';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { Textarea } from '@/components/ui/textarea';

import { fetchPolicies, type MerchantPolicy } from './data';

const Policies = () => {
  const policiesQuery = useQuery({ queryKey: ['merchant', 'policies'], queryFn: fetchPolicies });
  const [drafts, setDrafts] = useState<MerchantPolicy[]>([]);

  useEffect(() => {
    if (policiesQuery.data && !drafts.length) {
      setDrafts(policiesQuery.data);
    }
  }, [policiesQuery.data, drafts.length]);

  const handleFieldChange = (policyId: string, field: keyof MerchantPolicy, value: number | string | string[]) => {
    setDrafts((current) =>
      current.map((policy) => (policy.id === policyId ? { ...policy, [field]: value } : policy))
    );
  };

  const handleSave = (policy: MerchantPolicy) => {
    const payload = {
      ...policy,
      evidenceChecklist: Array.isArray(policy.evidenceChecklist)
        ? policy.evidenceChecklist
        : (policy.evidenceChecklist as unknown as string).split('\n').filter(Boolean)
    };

    void axiosClient.post(`/merchant/policies/${policy.id}`, payload).catch((error) => {
      console.error('Failed to update policy', error);
    });
  };

  const policies = drafts.length ? drafts : policiesQuery.data ?? [];

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Buyer protection"
        title="Policies"
        description="Review the rule thresholds EnsureBack applies before Stripe disputes reach your team."
      />

      <div className="grid gap-6 lg:grid-cols-2">
        {policies.map((policy) => (
          <Card key={policy.id} className="border border-muted">
            <CardHeader>
              <CardTitle>{policy.name}</CardTitle>
              <CardDescription>Adjust SLA timing and automated actions. Field names mirror existing APIs.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-2">
                <label className="text-sm font-medium text-foreground" htmlFor={`${policy.id}-slaHours`}>
                  slaHours
                </label>
                <Input
                  id={`${policy.id}-slaHours`}
                  type="number"
                  min={0}
                  value={policy.slaHours}
                  onChange={(event) => handleFieldChange(policy.id, 'slaHours', Number(event.target.value))}
                />
              </div>
              <div className="grid gap-2">
                <label className="text-sm font-medium text-foreground" htmlFor={`${policy.id}-autoRefundThreshold`}>
                  autoRefundThreshold
                </label>
                <Input
                  id={`${policy.id}-autoRefundThreshold`}
                  type="number"
                  min={0}
                  step={100}
                  value={policy.autoRefundThreshold}
                  onChange={(event) => handleFieldChange(policy.id, 'autoRefundThreshold', Number(event.target.value))}
                />
              </div>
              <div className="grid gap-2">
                <label className="text-sm font-medium text-foreground" htmlFor={`${policy.id}-evidenceChecklist`}>
                  evidenceChecklist
                </label>
                <Textarea
                  id={`${policy.id}-evidenceChecklist`}
                  rows={4}
                  value={policy.evidenceChecklist.join('\n')}
                  onChange={(event) =>
                    handleFieldChange(policy.id, 'evidenceChecklist', event.target.value.split('\n').filter(Boolean))
                  }
                />
                <p className="text-xs text-muted-foreground">One checklist item per line.</p>
              </div>
              <div className="flex justify-end">
                <Button size="sm" type="button" onClick={() => handleSave(policy)}>
                  Save changes
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default Policies;
