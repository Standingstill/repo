import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Drawer } from '@/components/ui/Drawer';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

import { fetchOrders, formatCurrency } from './data';

const Receipts = () => {
  const ordersQuery = useQuery({ queryKey: ['merchant', 'orders'], queryFn: fetchOrders });
  const orders = ordersQuery.data ?? [];
  const [search, setSearch] = useState('');
  const [activeOrderId, setActiveOrderId] = useState<string | null>(null);

  const filteredOrders = useMemo(() => {
    const lower = search.toLowerCase();
    return orders.filter(
      (order) =>
        order.customerEmail.toLowerCase().includes(lower) || order.id.toLowerCase().includes(lower)
    );
  }, [orders, search]);

  const activeOrder = filteredOrders.find((order) => order.id === activeOrderId) ?? null;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Proof of communication"
        title="Digital Receipts"
        description="Search receipts by buyer or order to access timelines, emails, and downloadable PDFs."
      />

      <Card className="border border-muted">
        <CardHeader className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle>Receipts</CardTitle>
            <CardDescription>Stripe events keep receipts up to date with buyer notifications.</CardDescription>
          </div>
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              type="search"
              placeholder="Search by email or order id"
              className="pl-9"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Search digital receipts"
            />
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="text-xs uppercase tracking-[0.3em] text-muted-foreground">
                <TableHead>Order</TableHead>
                <TableHead>Buyer</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Updated</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredOrders.map((order) => (
                <TableRow key={order.id} className="text-sm">
                  <TableCell className="font-medium">{order.id}</TableCell>
                  <TableCell>{order.customerEmail}</TableCell>
                  <TableCell className="capitalize">{order.status}</TableCell>
                  <TableCell className="text-right text-xs text-muted-foreground">
                    {new Date(order.updatedAt).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(order.amount, order.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button size="sm" variant="outline" onClick={() => setActiveOrderId(order.id)}>
                      View
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Drawer
        open={Boolean(activeOrder)}
        onClose={() => setActiveOrderId(null)}
        title="Receipt details"
        description={activeOrder ? activeOrder.id : ''}
      >
        {activeOrder && (
          <div className="space-y-4 text-sm">
            <div className="grid gap-1">
              <span className="text-xs text-muted-foreground">Buyer</span>
              <span className="font-semibold text-foreground">{activeOrder.customerEmail}</span>
            </div>
            <div className="grid gap-1">
              <span className="text-xs text-muted-foreground">Amount</span>
              <span className="font-semibold text-foreground">
                {formatCurrency(activeOrder.amount, activeOrder.currency)}
              </span>
            </div>
            <div className="grid gap-2">
              <span className="text-xs text-muted-foreground">Timeline</span>
              <ul className="space-y-2 text-muted-foreground">
                <li>Payment received on Stripe</li>
                <li>EnsureBack policy executed</li>
                <li>Buyer email sent with protection summary</li>
              </ul>
            </div>
            <div className="flex gap-2">
              <Button size="sm">Download PDF</Button>
              <Button size="sm" variant="outline">
                Share link
              </Button>
            </div>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default Receipts;
