import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../../api/axiosClient';

// Matches PurchaseOrderDTO: { orderId, buyer (userId), company, event, tickets[] }
const DUMMY_ORDERS = [
  { orderId: 'demo-001', buyer: 'U88102934', company: 'BGU Events', event: 'Rock Night Live', tickets: [{}, {}] },
  { orderId: 'demo-002', buyer: 'U77203845', company: 'BGU Events', event: 'Hamlet', tickets: [{}] },
  { orderId: 'demo-003', buyer: 'U91034712', company: 'BGU Events', event: 'Classical Evening', tickets: [{}, {}, {}] },
];

export default function FinancialsTab() {
  const [orders, setOrders] = useState(DUMMY_ORDERS);

  const fetchOrders = useCallback(async () => {
    try {
      const res = await axiosClient.get('/admin/purchases');
      setOrders(res.data);
    } catch {
      setOrders(DUMMY_ORDERS);
    }
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  return (
    <div className="glass-card rounded-xl overflow-hidden">
      <div className="p-6 border-b border-outline-variant flex justify-between items-center">
        <h2 className="text-headline-sm text-on-surface">Purchase History</h2>
        <div className="flex gap-2">
          <button
            onClick={fetchOrders}
            className="bg-secondary text-on-secondary px-4 py-2 text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-2"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>refresh</span>
            Refresh
          </button>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-surface-container-high text-label-sm text-on-surface-variant uppercase">
            <tr>
              <th className="px-6 py-4">Order ID</th>
              <th className="px-6 py-4">Buyer (User ID)</th>
              <th className="px-6 py-4">Company</th>
              <th className="px-6 py-4">Event</th>
              <th className="px-6 py-4 text-right">Tickets</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant">
            {orders.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-on-surface-variant text-body-md">
                  No orders found.
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.orderId} className="hover:bg-surface-container-low transition-colors">
                  <td className="px-6 py-4 text-label-md text-secondary font-mono">#{order.orderId}</td>
                  <td className="px-6 py-4 text-label-md text-on-surface-variant font-mono truncate max-w-[180px]">
                    {order.buyer}
                  </td>
                  <td className="px-6 py-4 text-body-md">{order.company}</td>
                  <td className="px-6 py-4 text-body-md">{order.event}</td>
                  <td className="px-6 py-4 text-right">
                    <span className="inline-flex items-center gap-1 text-label-md">
                      <span className="material-symbols-outlined text-secondary" style={{ fontSize: '16px' }}>confirmation_number</span>
                      {order.tickets?.length ?? 0}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
