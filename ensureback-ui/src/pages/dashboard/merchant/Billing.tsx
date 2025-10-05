import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PageHeader } from '@/components/ui/page-header';

const Billing = () => (
  <div className="space-y-8">
    <PageHeader
      eyebrow="Plan details"
      title="Billing"
      description="Review your EnsureBack usage and Stripe-aligned pricing. Changes are handled in the EnsureBack console."
    />

    <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
      <Card className="border border-muted">
        <CardHeader>
          <CardTitle>Usage-based pricing</CardTitle>
          <CardDescription>Aligned with Stripe charge volume for protected payments.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-3xl font-semibold text-foreground">0.65% + $0.20</p>
          <p className="text-sm text-muted-foreground">Per protected Stripe payment. Volume discounts kick in above $1M / month.</p>
          <ul className="list-disc space-y-2 pl-5 text-sm text-muted-foreground">
            <li>Includes alerting, case automation, and evidence packaging</li>
            <li>Unlimited teammates and workspaces</li>
            <li>Guaranteed access to support SLAs</li>
          </ul>
        </CardContent>
      </Card>

      <div className="space-y-6">
        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>This month</CardTitle>
            <CardDescription>01 Oct – 31 Oct</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm text-muted-foreground">
            <div className="flex items-center justify-between">
              <span>Protected payments</span>
              <span className="font-semibold text-foreground">$486,900</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Alerts processed</span>
              <span className="font-semibold text-foreground">68</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Estimated fee</span>
              <span className="font-semibold text-foreground">$3,177.94</span>
            </div>
          </CardContent>
        </Card>
        <Card className="border border-muted">
          <CardHeader>
            <CardTitle>Statements</CardTitle>
            <CardDescription>Downloadable invoices for finance reconciliation.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-3 text-sm">
            <a className="rounded-2xl border border-muted px-3 py-2 hover:bg-muted" href="#">
              September 2025
            </a>
            <a className="rounded-2xl border border-muted px-3 py-2 hover:bg-muted" href="#">
              August 2025
            </a>
            <a className="rounded-2xl border border-muted px-3 py-2 hover:bg-muted" href="#">
              July 2025
            </a>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
);

export default Billing;
