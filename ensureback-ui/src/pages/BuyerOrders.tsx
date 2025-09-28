import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Check, MessageCircle, Package, X } from 'lucide-react';

import axiosClient from '@/api/axiosClient';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/utils';

interface BuyerOrder {
  orderId: string;
  productName: string;
  status: 'processing' | 'shipped' | 'delivered' | 'disputed';
  total: number;
  currency: string;
  updatedAt: string;
  disputeStatus?: 'open' | 'resolved' | 'partial_refund';
  partialRefundOffer?: number;
}

const fetchBuyerOrders = async (): Promise<BuyerOrder[]> => {
  try {
    const response = await axiosClient.get<BuyerOrder[]>('/buyer/orders');
    return response.data;
  } catch (error) {
    const now = Date.now();
    return [
      {
        orderId: 'ORDER-12345',
        productName: 'Wireless Earbuds',
        status: 'processing',
        total: 12500,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 15).toISOString()
      },
      {
        orderId: 'ORDER-67890',
        productName: 'Mechanical Keyboard',
        status: 'delivered',
        total: 18900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 18).toISOString(),
        disputeStatus: 'open',
        partialRefundOffer: 5000
      },
      {
        orderId: 'ORDER-24680',
        productName: '4K Monitor',
        status: 'disputed',
        total: 32900,
        currency: 'usd',
        updatedAt: new Date(now - 1000 * 60 * 60 * 42).toISOString(),
        disputeStatus: 'partial_refund',
        partialRefundOffer: 8000
      }
    ];
  }
};

const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: currency.toUpperCase() }).format(amount / 100);

const BuyerOrders = () => {
  const { data, isLoading } = useQuery({ queryKey: ['buyer', 'orders'], queryFn: fetchBuyerOrders });
  const [disputeOrder, setDisputeOrder] = useState<BuyerOrder | null>(null);
  const [disputeMessage, setDisputeMessage] = useState('');
  const [evidence, setEvidence] = useState<File | null>(null);

  const resetModal = () => {
    setDisputeOrder(null);
    setDisputeMessage('');
    setEvidence(null);
  };

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-4 py-12">
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">Buyer Portal</h1>
        <p className="text-muted-foreground">
          View order progress, start disputes, and negotiate partial refunds without leaving EnsureBack.
        </p>
      </header>

      <Card>
        <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Orders</CardTitle>
            <CardDescription>All of your EnsureBack protected orders and their current status.</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Order</TableHead>
                <TableHead>Product</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Updated</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={6} className="py-12 text-center text-muted-foreground">
                    Loading buyer orders…
                  </TableCell>
                </TableRow>
              )}
              {data?.map((order) => (
                <TableRow key={order.orderId}>
                  <TableCell className="font-medium">{order.orderId}</TableCell>
                  <TableCell>{order.productName}</TableCell>
                  <TableCell>
                    <span
                      className={cn(
                        'inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium capitalize',
                        order.status === 'processing' && 'bg-amber-100 text-amber-700',
                        order.status === 'delivered' && 'bg-emerald-100 text-emerald-700',
                        order.status === 'shipped' && 'bg-blue-100 text-blue-700',
                        order.status === 'disputed' && 'bg-red-100 text-red-700'
                      )}
                    >
                      {order.status}
                    </span>
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {new Date(order.updatedAt).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(order.total, order.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setDisputeOrder(order)}
                        className="flex items-center gap-1"
                      >
                        <MessageCircle className="h-4 w-4" />
                        {order.disputeStatus ? 'Update dispute' : 'Open dispute'}
                      </Button>
                      {order.partialRefundOffer && (
                        <Button variant="ghost" size="sm" className="flex items-center gap-1">
                          <Package className="h-4 w-4" /> Offer {formatCurrency(order.partialRefundOffer, order.currency)}
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Dialog open={Boolean(disputeOrder)} onOpenChange={(open) => {
        if (!open) {
          resetModal();
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Dispute order {disputeOrder?.orderId}</DialogTitle>
            <DialogDescription>
              Share what went wrong and add evidence so the seller can respond quickly.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid gap-2">
              <label htmlFor="message" className="text-sm font-medium">
                Message to seller
              </label>
              <Textarea
                id="message"
                value={disputeMessage}
                onChange={(event) => setDisputeMessage(event.target.value)}
                placeholder="Describe the issue you're experiencing"
              />
            </div>
            <div className="grid gap-2">
              <label htmlFor="evidence" className="text-sm font-medium">
                Upload evidence
              </label>
              <Input id="evidence" type="file" onChange={(event) => setEvidence(event.target.files?.[0] ?? null)} />
              {evidence && <p className="text-xs text-muted-foreground">Attached: {evidence.name}</p>}
            </div>
            {disputeOrder?.partialRefundOffer && (
              <div className="rounded-lg border bg-muted/40 p-4 text-sm">
                <p className="font-medium">Partial refund offer available</p>
                <p className="text-muted-foreground">
                  The seller proposed {formatCurrency(disputeOrder.partialRefundOffer, disputeOrder.currency)} to resolve this
                  dispute.
                </p>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button variant="default" size="sm" className="flex items-center gap-1">
                    <Check className="h-4 w-4" /> Accept offer
                  </Button>
                  <Button variant="outline" size="sm" className="flex items-center gap-1">
                    <X className="h-4 w-4" /> Decline
                  </Button>
                </div>
              </div>
            )}
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="ghost" onClick={resetModal}>
              Cancel
            </Button>
            <Button onClick={resetModal}>Submit dispute</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default BuyerOrders;
