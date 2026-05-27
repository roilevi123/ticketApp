import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function SalesAnalyticsTab({ companyName }) {
  const [transactions, setTransactions] = useState([]);
  const [report, setReport] = useState({ totalRevenue: 0, totalTicketsSold: 0, pendingOrders: 0 });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchAnalytics = async () => {
      if (!companyName) return;
      setIsLoading(true);

      try {
        const encodedName = encodeURIComponent(companyName);
        const response = await axiosClient.get(`/company/${encodedName}/sales-report`);
        const reportData = response.data;

        if (reportData) {
          setReport({
            totalRevenue: reportData.totalRevenue || 0,
            totalTicketsSold: reportData.totalTicketsSold || 0,
            pendingOrders: 0 
          });

          if (Array.isArray(reportData.orders)) {
            setTransactions(reportData.orders);
          } else {
            setTransactions([]);
          }
        }
      } catch (error) {
        console.error("Failed to fetch analytics", error);
        setTransactions([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAnalytics();
  }, [companyName]);

  if (isLoading) {
    return <div className="text-on-surface p-10 text-center">Loading Analytics...</div>;
  }

  const safeRevenue = Number(report.totalRevenue) || 0;
  const safeTickets = Number(report.totalTicketsSold) || 0;
  const safePending = Number(report.pendingOrders) || 0;

  return (
    <div className="w-full flex flex-col gap-8">
      
      {/* --- אזור 1: קוביות המידע --- */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-surface-container border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Total Revenue</span>
          <span className="text-4xl font-bold text-secondary">${safeRevenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>

        <div className="bg-surface-container border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Tickets Sold</span>
          <span className="text-4xl font-bold text-on-surface">{safeTickets}</span>
        </div>

        <div className="bg-surface-container border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Pending Orders</span>
          <span className="text-4xl font-bold text-error">{safePending}</span>
        </div>
      </div>

      {/* --- אזור 2: טבלת העסקאות --- */}
      <div className="bg-surface-container border border-outline-variant rounded-xl overflow-hidden flex flex-col">
        <div className="p-6 border-b border-outline-variant flex justify-between items-center bg-surface-container-high">
          <h3 className="text-xl font-semibold text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary">receipt_long</span>
            Recent Transactions
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-background text-on-surface-variant text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold border-b border-outline-variant">Order ID</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Date</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Event</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Buyer ID</th>
                <th className="p-4 font-semibold border-b border-outline-variant text-center">Tickets</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Total Amount</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Status</th>
              </tr>
            </thead>
            <tbody className="text-on-surface text-sm">
              
              {/* אם אין עסקאות בכלל, נציג הודעה יפה */}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan="7" className="p-8 text-center text-on-surface-variant opacity-70">
                    <span className="material-symbols-outlined text-4xl mb-2 block">receipt_long</span>
                    No transactions found for this company yet.
                  </td>
                </tr>
              )}

              {/* מעבר על כל העסקאות (ההזמנות) */}
              {transactions.map((order, index) => {
                const ticketsArray = Array.isArray(order.tickets) ? order.tickets : [];
                
                // חישוב סכום ההזמנה - חיבור של מחירי כל הכרטיסים
                const totalAmount = ticketsArray.reduce((sum, ticket) => sum + (Number(ticket.price) || 0), 0);
                
                // משיכת התאריך מהכרטיס הראשון בהזמנה
                let formattedDate = 'N/A';
                if (ticketsArray.length > 0 && ticketsArray[0].date) {
                  formattedDate = new Date(ticketsArray[0].date).toLocaleString('en-GB', {
                    day: '2-digit', month: '2-digit', year: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                  });
                }

                return (
                  <tr key={order.orderId || index} className="hover:bg-surface-container-high transition-colors border-b border-outline-variant/50 last:border-0">
                    {/* מציג את תחילת ה-UUID כדי שלא יתפוס את כל המסך */}
                    <td className="p-4 font-mono text-secondary" title={order.orderId}>
                      {order.orderId ? order.orderId.substring(0, 8) + '...' : 'N/A'}
                    </td>
                    <td className="p-4 text-on-surface-variant">{formattedDate}</td>
                    <td className="p-4 font-semibold">{order.event || 'N/A'}</td>
                    
                    {/* מציג את תחילת מזהה הקונה */}
                    <td className="p-4" title={order.buyer}>
                      {order.buyer ? order.buyer.substring(0, 8) + '...' : 'N/A'}
                    </td>
                    
                    <td className="p-4 text-center font-bold text-on-surface">{ticketsArray.length}</td>
                    <td className="p-4 font-bold text-secondary">${totalAmount.toFixed(2)}</td>
                    <td className="p-4">
                      {/* ה-DB שלך מחזיר הזמנות שכבר נקנו (isPurchased: true), אז נסמן כהושלם */}
                      <span className="px-2 py-1 rounded text-xs font-bold bg-green-900/50 text-green-400 border border-green-800">
                        COMPLETED
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}