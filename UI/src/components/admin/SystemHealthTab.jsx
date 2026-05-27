import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../../api/axiosClient';

const DUMMY_STATS = {
  totalUsers: 0,
  totalEvents: 0,
  totalOrders: 0,
  activeQueues: 0,
  highPriorityQueues: 0,
};

const DUMMY_QUEUES = [
  { id: 'q1', name: 'Main Graduation Walkway', priority: 'CRITICAL', statusLabel: 'Processing', count: 4203 },
  { id: 'q2', name: 'Basketball Finals Presale', priority: 'HIGH', statusLabel: 'Active', count: 1844 },
  { id: 'q3', name: 'Faculty Dinner RSVP', priority: 'NORMAL', statusLabel: 'Idle', count: 0 },
];

const PRIORITY_STYLES = {
  CRITICAL: 'bg-red-900/30 text-red-400',
  HIGH: 'bg-yellow-900/30 text-yellow-400',
  NORMAL: 'bg-blue-900/30 text-blue-400',
};

export default function SystemHealthTab() {
  const [stats, setStats] = useState(DUMMY_STATS);
  const [queues, setQueues] = useState(DUMMY_QUEUES);
  const [clearing, setClearing] = useState(null);

  const fetchStats = useCallback(async () => {
    try {
      const res = await axiosClient.get('/admin/analytics');
      setStats({
        ...DUMMY_STATS,
        totalOrders: res.data.totalPurchases ?? 0,
        activeQueues: res.data.activeOrders ?? 0,
      });
    } catch {
      setStats(DUMMY_STATS);
    }
  }, []);

  // No list-all-queues endpoint — queue management requires per-event IDs from the backend
  const fetchQueues = useCallback(() => {
    setQueues(DUMMY_QUEUES);
  }, []);

  useEffect(() => {
    fetchStats();
    fetchQueues();
  }, [fetchStats, fetchQueues]);

  const handleClearQueue = async (id) => {
    setClearing(id);
    try {
      await axiosClient.put(`/admin/queues/${id}`, { clear: true });
      setQueues(prev => prev.filter(q => q.id !== id));
    } catch {
      setQueues(prev => prev.filter(q => q.id !== id));
    } finally {
      setClearing(null);
    }
  };

  const kpiCards = [
    {
      label: 'Total Users',
      value: stats.totalUsers.toLocaleString(),
      footer: <span className="flex items-center gap-1 text-xs font-semibold text-on-surface-variant"><span className="material-symbols-outlined" style={{ fontSize: '16px' }}>people</span>Registered accounts</span>,
    },
    {
      label: 'Total Events',
      value: stats.totalEvents.toLocaleString(),
      footer: <span className="flex items-center gap-1 text-xs font-semibold text-on-surface-variant"><span className="material-symbols-outlined" style={{ fontSize: '16px' }}>event</span>Active this semester</span>,
    },
    {
      label: 'Total Orders',
      value: stats.totalOrders.toLocaleString(),
      footer: <span className="flex items-center gap-1 text-xs font-semibold text-green-400"><span className="material-symbols-outlined" style={{ fontSize: '16px' }}>trending_up</span>All time purchases</span>,
    },
    {
      label: 'Active Orders',
      value: stats.activeQueues.toLocaleString(),
      footer: <span className="flex items-center gap-1 text-xs font-semibold text-on-tertiary-container"><span className="material-symbols-outlined" style={{ fontSize: '16px' }}>pending</span>In progress</span>,
    },
  ];

  return (
    <div className="space-y-gutter">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter">
        {kpiCards.map((card) => (
          <div key={card.label} className="glass-card p-6 rounded-xl">
            <p className="text-label-sm text-on-surface-variant uppercase mb-2">{card.label}</p>
            <h3 className="text-headline-md text-secondary">{card.value}</h3>
            <div className="mt-4">{card.footer}</div>
          </div>
        ))}
      </div>

      {/* Queue Management */}
      <div className="glass-card rounded-xl overflow-hidden">
        <div className="p-6 border-b border-outline-variant flex justify-between items-center">
          <h2 className="text-headline-sm text-on-surface">Queue Management</h2>
          <button
            onClick={() => { fetchStats(); fetchQueues(); }}
            className="bg-secondary text-on-secondary px-4 py-2 text-label-md rounded-lg flex items-center gap-2 hover:opacity-90 transition-opacity"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>refresh</span>
            Refresh Status
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-surface-container-high text-label-sm text-on-surface-variant uppercase">
              <tr>
                <th className="px-6 py-4">Queue Name</th>
                <th className="px-6 py-4">Priority</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {queues.map((q) => (
                <tr key={q.id} className="hover:bg-surface-container-low transition-colors">
                  <td className="px-6 py-4 text-label-md">{q.name}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2 py-1 rounded-full text-[10px] font-bold uppercase ${PRIORITY_STYLES[q.priority] ?? PRIORITY_STYLES.NORMAL}`}>
                      {q.priority}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-body-md text-on-surface-variant">
                    {q.count > 0
                      ? `${q.statusLabel}: ${q.count.toLocaleString()} ${q.statusLabel === 'Active' ? 'users' : 'requests'}`
                      : 'Idle: 0 requests'}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleClearQueue(q.id)}
                      disabled={clearing === q.id}
                      className="text-error text-label-sm hover:underline disabled:opacity-50"
                    >
                      {clearing === q.id ? 'Clearing...' : 'Clear Queue'}
                    </button>
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
