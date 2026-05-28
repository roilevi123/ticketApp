import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../../api/axiosClient';

// Matches Suspension: { userID, startTime, endTime, isPermanent }
const DUMMY_SUSPENDED = [
  { userID: 'U88234A', startTime: '2024-10-01T10:00:00', endTime: '2024-11-12T10:00:00', isPermanent: false },
  { userID: 'U11092B', startTime: '2024-09-15T08:30:00', endTime: null, isPermanent: true },
];

const DURATIONS = ['24 Hours', '7 Days', '30 Days', 'Indefinite'];

const DURATION_TO_DAYS = {
  '24 Hours': 1,
  '7 Days': 7,
  '30 Days': 30,
  'Indefinite': 0,
};

function formatEndDate(suspension) {
  if (suspension.isPermanent || !suspension.endTime) return 'Permanent';
  try {
    return new Date(suspension.endTime).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
    });
  } catch {
    return suspension.endTime;
  }
}

export default function UserManagementTab() {
  const [suspendedUsers, setSuspendedUsers] = useState(DUMMY_SUSPENDED);
  const [suspendUserId, setSuspendUserId] = useState('');
  const [suspendDuration, setSuspendDuration] = useState(DURATIONS[0]);
  const [suspending, setSuspending] = useState(false);
  const [deleteUserId, setDeleteUserId] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [actionMsg, setActionMsg] = useState(null);

  // Close Company state
  const [closeCompanyName, setCloseCompanyName] = useState('');
  const [confirmingClose, setConfirmingClose] = useState(false);
  const [closing, setClosing] = useState(false);

  const fetchSuspended = useCallback(async () => {
    try {
      const res = await axiosClient.get('/admin/suspensions');
      setSuspendedUsers(res.data);
    } catch {
      setSuspendedUsers(DUMMY_SUSPENDED);
    }
  }, []);

  useEffect(() => {
    fetchSuspended();
  }, [fetchSuspended]);

  const showMsg = (msg, type = 'success') => {
    setActionMsg({ msg, type });
    setTimeout(() => setActionMsg(null), 3000);
  };

  const handleCloseCompany = async (e) => {
    e.preventDefault();
    if (!closeCompanyName.trim() || !confirmingClose) return;
    setClosing(true);
    try {
      await axiosClient.delete(`/admin/companies/${encodeURIComponent(closeCompanyName)}`);
      showMsg(`Company "${closeCompanyName}" has been closed.`);
      setCloseCompanyName('');
      setConfirmingClose(false);
    } catch (err) {
      const msg = err.response?.data?.error ?? err.message;
      showMsg(`Close failed: ${msg}`, 'error');
    } finally {
      setClosing(false);
    }
  };

  const handleSuspend = async (e) => {
    e.preventDefault();
    if (!suspendUserId.trim()) return;
    setSuspending(true);
    try {
      await axiosClient.put(`/admin/users/${encodeURIComponent(suspendUserId)}/suspend`, {
        durationInDays: DURATION_TO_DAYS[suspendDuration],
      });
      showMsg(`User ${suspendUserId} suspended for ${suspendDuration}.`);
      setSuspendUserId('');
      fetchSuspended();
    } catch (err) {
      const msg = err.response?.data?.error ?? err.message;
      showMsg(`Suspension failed: ${msg}`, 'error');
    } finally {
      setSuspending(false);
    }
  };

  const handleUnsuspend = async (userId) => {
    try {
      await axiosClient.put(`/admin/users/${encodeURIComponent(userId)}/suspend/cancel`);
      setSuspendedUsers(prev => prev.filter(u => u.userID !== userId));
      showMsg('User unsuspended.');
    } catch (err) {
      const msg = err.response?.data?.error ?? err.message;
      showMsg(`Unsuspend failed: ${msg}`, 'error');
    }
  };

  const handleDeleteAccount = async (e) => {
    e.preventDefault();
    if (!deleteUserId.trim() || !confirmingDelete) return;
    try {
      await axiosClient.delete(`/admin/users/${encodeURIComponent(deleteUserId)}`);
      showMsg(`User ${deleteUserId} deleted.`);
    } catch (err) {
      const msg = err.response?.data?.error ?? err.message;
      showMsg(`Delete failed: ${msg}`, 'error');
    }
    setDeleteUserId('');
    setConfirmingDelete(false);
  };

  return (
    <div className="space-y-gutter">
      {actionMsg && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-lg shadow-xl border text-label-md font-bold ${
          actionMsg.type === 'success'
            ? 'bg-surface-container border-secondary text-secondary'
            : 'bg-surface-container border-error text-error'
        }`}>
          <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
            {actionMsg.type === 'success' ? 'check_circle' : 'error'}
          </span>
          {actionMsg.msg}
        </div>
      )}

      {/* Close Company */}
      <div className="glass-card p-6 rounded-xl">
        <h2 className="text-headline-sm text-on-surface mb-6 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary" style={{ fontSize: '20px' }}>domain_disabled</span>
          Force Close Company
        </h2>
        <form onSubmit={handleCloseCompany} className="space-y-4">
          <div>
            <label className="text-label-sm text-on-surface-variant block mb-2">Company Name</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-2.5 text-on-surface-variant" style={{ fontSize: '18px' }}>
                domain
              </span>
              <input
                type="text"
                value={closeCompanyName}
                onChange={e => { setCloseCompanyName(e.target.value); setConfirmingClose(false); }}
                placeholder="Enter company name"
                className="w-full bg-background border border-outline-variant rounded px-10 py-2.5 focus:border-secondary focus:outline-none transition-all text-body-md"
              />
            </div>
          </div>
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={confirmingClose}
              onChange={e => setConfirmingClose(e.target.checked)}
              className="accent-secondary"
            />
            <span className="text-label-sm text-on-surface-variant">
              I understand this permanently closes the company and cancels all its events
            </span>
          </label>
          <button
            type="submit"
            disabled={closing || !closeCompanyName.trim() || !confirmingClose}
            className="w-full bg-on-surface text-background font-bold py-3 rounded hover:bg-secondary transition-all disabled:opacity-50"
          >
            {closing ? 'Closing…' : 'Force Close Company'}
          </button>
        </form>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-gutter">
        {/* Suspend User Form */}
        <div className="glass-card p-6 rounded-xl">
          <h2 className="text-headline-sm text-on-surface mb-6">Suspend User Account</h2>
          <form onSubmit={handleSuspend} className="space-y-4">
            <div>
              <label className="text-label-sm text-on-surface-variant block mb-2">User ID</label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-2.5 text-on-surface-variant" style={{ fontSize: '18px' }}>
                  person_search
                </span>
                <input
                  type="text"
                  value={suspendUserId}
                  onChange={e => setSuspendUserId(e.target.value)}
                  placeholder="Enter user ID"
                  className="w-full bg-background border border-outline-variant rounded px-10 py-2.5 focus:border-secondary focus:outline-none transition-all text-body-md"
                />
              </div>
            </div>
            <div>
              <label className="text-label-sm text-on-surface-variant block mb-2">Duration</label>
              <select
                value={suspendDuration}
                onChange={e => setSuspendDuration(e.target.value)}
                className="w-full bg-background border border-outline-variant rounded px-4 py-2.5 focus:border-secondary focus:outline-none transition-all text-on-surface text-body-md"
              >
                {DURATIONS.map(d => <option key={d}>{d}</option>)}
              </select>
            </div>
            <button
              type="submit"
              disabled={suspending || !suspendUserId.trim()}
              className="w-full bg-on-surface text-background font-bold py-3 rounded hover:bg-secondary transition-all disabled:opacity-50"
            >
              {suspending ? 'Submitting...' : 'Submit Suspension'}
            </button>
          </form>
        </div>

        {/* Danger Zone */}
        <div className="border-2 border-error-container p-6 rounded-xl bg-error-container/5">
          <h2 className="text-headline-sm text-error mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined">warning</span>
            Danger Zone
          </h2>
          <p className="text-body-md text-on-surface-variant mb-6">
            Actions here are irreversible. Use extreme caution when deleting accounts or clearing institutional data.
          </p>
          <form onSubmit={handleDeleteAccount} className="space-y-3">
            <div>
              <label className="text-label-sm text-on-surface-variant block mb-2">User ID to Delete</label>
              <input
                type="text"
                value={deleteUserId}
                onChange={e => { setDeleteUserId(e.target.value); setConfirmingDelete(false); }}
                placeholder="Enter user ID"
                className="w-full bg-background border border-error-container/50 rounded px-4 py-2.5 focus:border-error focus:outline-none transition-all text-body-md"
              />
            </div>
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={confirmingDelete}
                onChange={e => setConfirmingDelete(e.target.checked)}
                className="accent-error"
              />
              <span className="text-label-sm text-on-surface-variant">I understand this is irreversible</span>
            </label>
            <div className="flex justify-between items-center p-4 bg-error-container/10 rounded border border-error-container/30">
              <div>
                <p className="text-label-md text-on-surface">Permanently Delete Account</p>
                <p className="text-xs text-on-surface-variant">Removes all history and active tickets.</p>
              </div>
              <button
                type="submit"
                disabled={!deleteUserId.trim() || !confirmingDelete}
                className="bg-error text-on-error px-6 py-2 font-bold rounded-lg hover:opacity-90 disabled:opacity-40 transition-opacity"
              >
                Delete Account
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Suspended Users Table */}
      <div className="glass-card rounded-xl overflow-hidden">
        <div className="p-6 border-b border-outline-variant flex justify-between items-center">
          <h2 className="text-headline-sm text-on-surface">Suspended Users</h2>
          <button
            onClick={fetchSuspended}
            className="text-label-sm text-on-surface-variant hover:text-secondary transition-colors flex items-center gap-1"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>refresh</span>
            Refresh
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-surface-container-high text-label-sm text-on-surface-variant uppercase">
              <tr>
                <th className="px-6 py-4">User ID</th>
                <th className="px-6 py-4">Suspended Since</th>
                <th className="px-6 py-4">Expires</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {suspendedUsers.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-on-surface-variant text-body-md">
                    No suspended users.
                  </td>
                </tr>
              ) : (
                suspendedUsers.map((u) => (
                  <tr key={u.userID} className="hover:bg-surface-container-low transition-colors">
                    <td className="px-6 py-4 text-label-md font-mono">{u.userID}</td>
                    <td className="px-6 py-4 text-sm text-on-surface-variant">
                      {u.startTime ? new Date(u.startTime).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <span className={u.isPermanent ? 'text-error font-semibold' : ''}>
                        {formatEndDate(u)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleUnsuspend(u.userID)}
                        className="text-secondary text-label-sm hover:underline"
                      >
                        Unsuspend
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
