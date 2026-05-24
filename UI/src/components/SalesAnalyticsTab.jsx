import { useState, useEffect } from 'react';

export default function SalesAnalyticsTab() {
  const [transactions, setTransactions] = useState([]);
  const [report, setReport] = useState({ totalRevenue: 0, ticketsSold: 0 });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const companyName = encodeURIComponent("BGU Events"); // מקודד את הרווח ב-URL
        const token = localStorage.getItem('token');

        // משיכת דוח מכירות כללי
        const reportRes = await fetch(`http://localhost:8080/api/company/${companyName}/sales-report`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        // משיכת היסטוריית רכישות
        const historyRes = await fetch(`http://localhost:8080/api/company/${companyName}/purchase-history`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });

        if (reportRes.ok && historyRes.ok) {
          const reportData = await reportRes.json();
          const historyData = await historyRes.json();
          
          // אם חזרו נתונים אמיתיים, נשתמש בהם. אחרת נטען נתוני דמה לתצוגה
          if (historyData && historyData.length > 0) {
            setTransactions(historyData);
            setReport(reportData);
          } else {
            loadDummyData();
          }
        } else {
          loadDummyData(); // במקרה של שגיאה (למשל אם ה-endpoint עדיין לא מוכן בשרת)
        }
      } catch (error) {
        console.error("Failed to fetch analytics", error);
        loadDummyData();
      } finally {
        setIsLoading(false);
      }
    };

    fetchAnalytics();
  }, []);

  // נתוני דמה כדי שנוכל לראות איך המסך נראה גם כשה-DB ריק
  const loadDummyData = () => {
    setReport({ totalRevenue: 12450.00, ticketsSold: 342, pendingOrders: 12 });
    setTransactions([
      { id: 'TRX-9821', buyer: 'student1@bgu.ac.il', event: 'STUDENTFEST', date: '2026-05-24 14:30', amount: 40.00, tickets: 2, status: 'COMPLETED' },
      { id: 'TRX-9822', buyer: 'guest99@gmail.com', event: 'STUDENTFEST', date: '2026-05-24 15:10', amount: 20.00, tickets: 1, status: 'COMPLETED' },
      { id: 'TRX-9823', buyer: 'vip_member@bgu.ac.il', event: 'STUDENTFEST', date: '2026-05-24 16:05', amount: 100.00, tickets: 5, status: 'PENDING' },
      { id: 'TRX-9824', buyer: 'musicfan@yahoo.com', event: 'Rock Night Live', date: '2026-05-23 09:15', amount: 89.99, tickets: 1, status: 'COMPLETED' },
    ]);
  };

  if (isLoading) {
    return <div className="text-on-surface p-10 text-center">Loading Analytics...</div>;
  }

  return (
    <div className="w-full flex flex-col gap-8">
      
      {/* אזור 1: ה"תמונה הגדולה" - כרטיסיות מידע (KPIs) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-[#191c1e] border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Total Revenue</span>
          <span className="text-4xl font-bold text-secondary">${report.totalRevenue.toLocaleString()}</span>
          <span className="text-xs text-green-400 mt-2 flex items-center gap-1">
            <span className="material-symbols-outlined text-[16px]">trending_up</span> +14% this week
          </span>
        </div>

        <div className="bg-[#191c1e] border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Tickets Sold</span>
          <span className="text-4xl font-bold text-on-surface">{report.ticketsSold}</span>
          <span className="text-xs text-on-surface-variant mt-2">Across all active events</span>
        </div>

        <div className="bg-[#191c1e] border border-outline-variant rounded-xl p-6 flex flex-col gap-2">
          <span className="text-on-surface-variant text-sm uppercase tracking-wider font-semibold">Pending Orders</span>
          <span className="text-4xl font-bold text-error">{report.pendingOrders || 0}</span>
          <span className="text-xs text-on-surface-variant mt-2">Awaiting payment confirmation</span>
        </div>
      </div>

      {/* אזור 2: טבלת הנתונים - ירידה לפרטים */}
      <div className="bg-[#191c1e] border border-outline-variant rounded-xl overflow-hidden flex flex-col">
        <div className="p-6 border-b border-outline-variant flex justify-between items-center bg-[#1d2022]">
          <h3 className="text-xl font-semibold text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary-fixed">receipt_long</span>
            Recent Transactions
          </h3>
          <button className="px-4 py-2 border border-outline-variant text-on-surface rounded-lg text-sm hover:bg-surface-container-highest transition-colors flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">download</span>
            Export CSV
          </button>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#101415] text-on-surface-variant text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold border-b border-outline-variant">Transaction ID</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Date & Time</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Event</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Buyer Info</th>
                <th className="p-4 font-semibold border-b border-outline-variant text-center">Tickets</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Amount</th>
                <th className="p-4 font-semibold border-b border-outline-variant">Status</th>
              </tr>
            </thead>
            <tbody className="text-on-surface text-sm">
              {transactions.map((trx, index) => (
                <tr key={index} className="hover:bg-[#1d2022] transition-colors border-b border-outline-variant/50 last:border-0">
                  <td className="p-4 font-mono text-primary-fixed">{trx.id}</td>
                  <td className="p-4 text-on-surface-variant">{trx.date}</td>
                  <td className="p-4 font-semibold">{trx.event}</td>
                  <td className="p-4">{trx.buyer}</td>
                  <td className="p-4 text-center">{trx.tickets}</td>
                  <td className="p-4 font-bold text-secondary">${trx.amount.toFixed(2)}</td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${
                      trx.status === 'COMPLETED' ? 'bg-green-900/50 text-green-400 border border-green-800' : 
                      'bg-yellow-900/50 text-yellow-400 border border-yellow-800'
                    }`}>
                      {trx.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}