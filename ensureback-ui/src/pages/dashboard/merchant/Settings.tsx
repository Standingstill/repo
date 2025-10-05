import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Copy, Eye, EyeOff } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { Textarea } from '@/components/ui/textarea';

import { fetchMerchantProfile } from './data';

const Settings = () => {
  const profileQuery = useQuery({ queryKey: ['merchant', 'profile'], queryFn: fetchMerchantProfile });
  const profile = profileQuery.data;
  const [showSecret, setShowSecret] = useState(false);
  const [notificationEmails, setNotificationEmails] = useState(
    profile?.supportEmail ? [profile.supportEmail, profile.financeEmail ?? ''].filter(Boolean).join('\n') : ''
  );

  const publicKey = profile?.apiKey ?? 'pk_live_1234567890';
  const secretKey = `sk_live_${publicKey.slice(-12)}`;

  const handleCopy = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
    } catch (error) {
      console.error('Clipboard copy failed', error);
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Workspace"
        title="Settings"
        description="Manage company identity, notification recipients, and read-only API credentials."
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Company profile</CardTitle>
            <CardDescription>Information shown on buyer-facing communications.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2">
              <label className="text-sm font-medium text-foreground" htmlFor="companyName">
                Company name
              </label>
              <Input id="companyName" defaultValue={profile?.companyName ?? ''} placeholder="Company LLC" />
            </div>
            <div className="grid gap-2">
              <label className="text-sm font-medium text-foreground" htmlFor="primaryEmail">
                Primary contact email
              </label>
              <Input id="primaryEmail" type="email" defaultValue={profile?.email ?? ''} placeholder="ops@company.com" />
            </div>
          </CardContent>
        </Card>

        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Notification emails</CardTitle>
            <CardDescription>Ensure alerts and case updates reach the right teams.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Textarea
              rows={4}
              value={notificationEmails}
              onChange={(event) => setNotificationEmails(event.target.value)}
              placeholder={'support@company.com\nfinance@company.com'}
            />
            <p className="text-xs text-muted-foreground">One email per line. Changes sync when saved in the EnsureBack console.</p>
          </CardContent>
        </Card>
      </div>

      <Card className="border border-muted">
        <CardHeader>
          <CardTitle>API keys</CardTitle>
          <CardDescription>Keys are read-only. Regenerate and rotate from the EnsureBack console.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-2">
            <label className="text-sm font-medium text-foreground" htmlFor="public-key">
              Public key
            </label>
            <div className="flex items-center gap-2 rounded-2xl border border-muted bg-muted/40 px-3 py-2 text-sm font-medium text-foreground">
              <span className="truncate" id="public-key">
                {publicKey}
              </span>
              <Button variant="ghost" size="icon" aria-label="Copy public key" onClick={() => handleCopy(publicKey)}>
                <Copy className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
          <div className="grid gap-2">
            <label className="text-sm font-medium text-foreground" htmlFor="secret-key">
              Secret key
            </label>
            <div className="flex items-center gap-2 rounded-2xl border border-muted bg-muted/40 px-3 py-2 text-sm font-medium text-foreground">
              <span className="truncate" id="secret-key">
                {showSecret ? secretKey : '•••• •••• •••• ••••'}
              </span>
              <Button variant="ghost" size="icon" aria-label="Toggle secret key visibility" onClick={() => setShowSecret((prev) => !prev)}>
                {showSecret ? <EyeOff className="h-4 w-4" aria-hidden="true" /> : <Eye className="h-4 w-4" aria-hidden="true" />}
              </Button>
              <Button variant="ghost" size="icon" aria-label="Copy secret key" onClick={() => handleCopy(secretKey)}>
                <Copy className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Settings;
