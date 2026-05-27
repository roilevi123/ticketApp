import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../contexts/AuthContext';

const AVAILABLE_PERMISSIONS = [
  "MANAGE_INVENTORY",
  "CONFIGURE_LAYOUT",
  "CHANGE_POLICIES",
  "RESPOND_TO_INQUIRIES",
  "VIEW_PURCHASE_HISTORY",
  "GENERATE_SALES_REPORTS"
];

const parseJwt = (token) => {
  try { return JSON.parse(atob(token.split('.')[1])); } catch { return null; }
};

export default function TeamHierarchyTab({ companyName }) {
  const [hierarchyTree, setHierarchyTree] = useState("Loading organizational tree...");
  const [formData, setFormData] = useState({ targetUserId: '', role: 'MANAGER', permissions: [] });
  const [fireUsername, setFireUsername] = useState('');
  const [showFireConfirm, setShowFireConfirm] = useState(false);
  const [firing, setFiring] = useState(false);
  const [showFreezeConfirm, setShowFreezeConfirm] = useState(false);
  const [freezing, setFreezing] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [closing, setClosing] = useState(false);
  const [showResignConfirm, setShowResignConfirm] = useState(false);
  const [resigning, setResigning] = useState(false);
  const { role, login } = useAuth();
  const navigate = useNavigate();
  const isFounder = role === 'FOUNDER';
  const isOwner = role === 'OWNER';

  useEffect(() => {
    fetchHierarchy();
  }, [companyName]);

  const fetchHierarchy = async () => {
    try {
      const response = await axiosClient.get(`/company/${encodeURIComponent(companyName)}/hierarchy`);
      setHierarchyTree(response.data.tree || "No tree data available.");
    } catch {
      setHierarchyTree("Network error. Server might be offline.");
    }
  };

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handlePermissionToggle = (permission) => {
    setFormData(prev => {
      const current = prev.permissions;
      return current.includes(permission)
        ? { ...prev, permissions: current.filter(p => p !== permission) }
        : { ...prev, permissions: [...current, permission] };
    });
  };

  const handleAssignRole = async (e) => {
    e.preventDefault();
    const payload = {
      targetUserId: formData.targetUserId,
      companyName,
      role: formData.role,
      permissions: formData.role === 'MANAGER' ? formData.permissions : []
    };
    try {
      await axiosClient.post('/company/assign-role', payload);
      alert(`${formData.role} assigned successfully to user ${formData.targetUserId}! Pending approval.`);
      setFormData({ targetUserId: '', role: 'MANAGER', permissions: [] });
      fetchHierarchy();
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to assign role: ${msg}`);
    }
  };

  const handleFireSubmit = (e) => {
    e.preventDefault();
    setShowFireConfirm(true);
  };

  const handleFireConfirm = async () => {
    setFiring(true);
    try {
      await axiosClient.delete(`/company/member?username=${encodeURIComponent(fireUsername)}&companyName=${encodeURIComponent(companyName)}`);
      setFireUsername('');
      fetchHierarchy();
    } catch (error) {
      const msg = error.response?.data?.error || error.response?.data || error.message || "Network error.";
      alert(`Failed to remove member: ${msg}`);
    } finally {
      setFiring(false);
      setShowFireConfirm(false);
    }
  };

  const handleFreezeCompany = async () => {
    setFreezing(true);
    try {
      await axiosClient.put(`/company/${encodeURIComponent(companyName)}/suspend`);
      alert(`${companyName} has been frozen. All events and purchasing are suspended.`);
      setShowFreezeConfirm(false);
    } catch (error) {
      const msg = error.response?.data?.error || error.message || 'Network error.';
      alert(`Failed to freeze company: ${msg}`);
    } finally {
      setFreezing(false);
      setShowFreezeConfirm(false);
    }
  };

  const handleCloseCompany = async () => {
    setClosing(true);
    try {
      await axiosClient.delete(`/company/close?companyName=${encodeURIComponent(companyName)}`);
      localStorage.removeItem('activeCompany');
      localStorage.removeItem('priorMemberToken');
      navigate('/');
    } catch (error) {
      const msg = error.response?.data?.error || error.message || "Network error.";
      alert(`Failed to close company: ${msg}`);
    } finally {
      setClosing(false);
      setShowCloseConfirm(false);
    }
  };

  const handleResign = async () => {
    setResigning(true);
    try {
      await axiosClient.delete(`/company/owner/relinquish?companyName=${encodeURIComponent(companyName)}`);
      try {
        const res = await axiosClient.post('/auth/exit-company');
        const decoded = parseJwt(res.data.token);
        login(res.data.token, decoded?.role?.toUpperCase() || 'MEMBER', decoded?.sub || 'unknown');
      } catch {
        const prior = localStorage.getItem('priorMemberToken');
        if (prior) {
          const decoded = parseJwt(prior);
          login(prior, decoded?.role?.toUpperCase() || 'MEMBER', decoded?.sub || 'unknown');
        }
      }
      localStorage.removeItem('activeCompany');
      localStorage.removeItem('priorMemberToken');
      navigate('/');
    } catch (error) {
      const msg = error.response?.data?.error || error.message || "Network error.";
      alert(`Failed to resign: ${msg}`);
    } finally {
      setResigning(false);
      setShowResignConfirm(false);
    }
  };

  return (
    <div className="w-full">
      {/* Fire confirm modal */}
      {showFireConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="bg-surface-container border border-outline-variant rounded-xl p-8 max-w-md w-full mx-4 shadow-2xl">
            <div className="flex items-center gap-3 mb-4">
              <span className="material-symbols-outlined text-error" style={{ fontSize: '28px' }}>person_remove</span>
              <h2 className="text-headline-sm font-bold text-on-surface">Remove Member?</h2>
            </div>
            <p className="text-body-md text-on-surface-variant mb-6">
              Remove <span className="font-bold text-secondary">{fireUsername}</span> from{' '}
              <span className="font-bold text-secondary">{companyName}</span>?
              <br /><span className="text-xs mt-1 block">This can only be done if you originally appointed them.</span>
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowFireConfirm(false)}
                className="px-5 py-2 border border-outline-variant text-on-surface-variant hover:bg-surface-container-high transition-colors text-label-md font-medium rounded"
              >
                Cancel
              </button>
              <button
                onClick={handleFireConfirm}
                disabled={firing}
                className="px-5 py-2 bg-error text-on-error text-label-md font-bold hover:opacity-90 transition-opacity disabled:opacity-50 rounded flex items-center gap-2"
              >
                {firing && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                {firing ? 'Removing...' : 'Yes, Remove'}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="mb-8">
        <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Team & Hierarchy</h2>
        <p className="text-on-surface-variant">Manage your organization's structure, assign roles, and configure manager permissions.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left column: Assign + Remove */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* Assign New Role */}
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">person_add</span>
              Assign New Role
            </h3>

            <form onSubmit={handleAssignRole} className="flex flex-col gap-5">
              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Appointed Username</label>
                <input
                  type="text" name="targetUserId" value={formData.targetUserId} onChange={handleInputChange}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                  placeholder="e.g., student2" required
                />
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Assign Role</label>
                <select
                  name="role" value={formData.role} onChange={handleInputChange}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                >
                  <option value="MANAGER">Manager (Custom Permissions)</option>
                  <option value="OWNER">Co-Owner (Full Access)</option>
                </select>
              </div>

              {formData.role === 'MANAGER' && (
                <div className="flex flex-col gap-3 mt-2 bg-background p-4 rounded-lg border border-outline-variant">
                  <label className="text-sm text-secondary uppercase tracking-wider font-bold mb-1">Select Manager Permissions</label>
                  {AVAILABLE_PERMISSIONS.map(permission => (
                    <label key={permission} className="flex items-center gap-3 cursor-pointer group">
                      <div className="relative flex items-center">
                        <input
                          type="checkbox"
                          className="peer sr-only"
                          checked={formData.permissions.includes(permission)}
                          onChange={() => handlePermissionToggle(permission)}
                        />
                        <div className="w-5 h-5 border-2 border-outline-variant rounded bg-surface-container peer-checked:bg-secondary peer-checked:border-secondary transition-all flex items-center justify-center">
                          <span className="material-symbols-outlined text-on-secondary text-[14px] opacity-0 peer-checked:opacity-100">check</span>
                        </div>
                      </div>
                      <span className="text-on-surface group-hover:text-secondary transition-colors text-sm font-mono">{permission}</span>
                    </label>
                  ))}
                </div>
              )}

              <button type="submit" className="mt-2 w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex justify-center items-center gap-2">
                <span className="material-symbols-outlined">how_to_reg</span>
                Assign Role & Send Invite
              </button>
            </form>
          </div>

          {/* Remove Member */}
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-error">person_remove</span>
              Remove Member
            </h3>

            <form onSubmit={handleFireSubmit} className="flex flex-col gap-5">
              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Username to Remove</label>
                <input
                  type="text"
                  value={fireUsername}
                  onChange={(e) => setFireUsername(e.target.value)}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-error focus:ring-1 focus:outline-none"
                  placeholder="e.g., student2" required
                />
              </div>

              <p className="text-xs text-on-surface-variant">
                You can only remove members you personally appointed.
              </p>

              <button type="submit" className="w-full py-3 border border-error text-error font-bold rounded-lg hover:bg-error hover:text-on-error transition-colors flex justify-center items-center gap-2">
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>person_remove</span>
                Remove Member
              </button>
            </form>
          </div>
        </div>

        {/* Right column: Hierarchy Tree */}
        <div className="lg:col-span-7">
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6 h-full flex flex-col">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">account_tree</span>
                Organizational Hierarchy
              </h3>
              <button onClick={fetchHierarchy} className="text-on-surface-variant hover:text-secondary transition-colors" title="Refresh Tree">
                <span className="material-symbols-outlined">refresh</span>
              </button>
            </div>

            <div className="flex-grow bg-background border border-outline-variant/50 rounded-lg p-5 overflow-auto font-mono text-sm text-green-400 whitespace-pre">
              {hierarchyTree}
            </div>

            <p className="text-xs text-on-surface-variant mt-4 text-center">
              The tree displays confirmed members only. Pending invites are not shown.
            </p>
          </div>
        </div>
      </div>

      {/* Freeze + Close — founder only, side by side */}
      {isFounder && (
        <div className="mt-10 grid grid-cols-1 md:grid-cols-2 gap-4">

          {/* Freeze Company */}
          <div className="border border-yellow-500/40 rounded-xl p-6 bg-yellow-500/5">
            {showFreezeConfirm ? (
              <div>
                <h3 className="text-lg font-bold text-yellow-500 mb-2 flex items-center gap-2">
                  <span className="material-symbols-outlined">warning</span>
                  Freeze company?
                </h3>
                <p className="text-body-md text-on-surface-variant mb-4">
                  <span className="font-bold text-secondary">{companyName}</span> will be suspended. Events will be hidden and purchasing halted.
                </p>
                <div className="flex gap-3">
                  <button
                    onClick={() => setShowFreezeConfirm(false)}
                    className="px-4 py-2 border border-outline-variant text-on-surface-variant hover:bg-surface-container-high transition-colors text-label-md font-medium rounded"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleFreezeCompany}
                    disabled={freezing}
                    className="px-4 py-2 bg-yellow-500 text-black text-label-md font-bold hover:opacity-90 transition-opacity disabled:opacity-50 rounded flex items-center gap-2"
                  >
                    {freezing && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                    {freezing ? 'Freezing...' : 'Yes, Freeze'}
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-3 h-full">
                <div>
                  <h3 className="text-lg font-bold text-yellow-500 mb-1">Freeze Company</h3>
                  <p className="text-body-sm text-on-surface-variant">
                    Suspend <span className="font-bold text-secondary">{companyName}</span>. Events are hidden and purchases are halted. Reversible.
                  </p>
                </div>
                <button
                  onClick={() => setShowFreezeConfirm(true)}
                  className="mt-auto flex items-center justify-center gap-2 px-4 py-2 border border-yellow-500 text-yellow-500 hover:bg-yellow-500 hover:text-black transition-colors text-label-md font-bold rounded"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>pause_circle</span>
                  Freeze Company
                </button>
              </div>
            )}
          </div>

          {/* Close Company */}
          <div className="border border-error/40 rounded-xl p-6 bg-error/5">
            {showCloseConfirm ? (
              <div>
                <h3 className="text-lg font-bold text-error mb-2 flex items-center gap-2">
                  <span className="material-symbols-outlined">warning</span>
                  Are you absolutely sure?
                </h3>
                <p className="text-body-md text-on-surface-variant mb-4">
                  This will <strong>permanently delete</strong> <span className="text-secondary font-bold">{companyName}</span> and all its data. This cannot be undone.
                </p>
                <div className="flex gap-3">
                  <button
                    onClick={() => setShowCloseConfirm(false)}
                    className="px-4 py-2 border border-outline-variant text-on-surface-variant hover:bg-surface-container-high transition-colors text-label-md font-medium rounded"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleCloseCompany}
                    disabled={closing}
                    className="px-4 py-2 bg-error text-on-error text-label-md font-bold hover:opacity-90 transition-opacity disabled:opacity-50 rounded flex items-center gap-2"
                  >
                    {closing && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                    {closing ? 'Closing...' : 'Yes, Close Forever'}
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-3 h-full">
                <div>
                  <h3 className="text-lg font-bold text-error mb-1">Close Company</h3>
                  <p className="text-body-sm text-on-surface-variant">
                    Permanently delete <span className="font-bold text-secondary">{companyName}</span> and all its events, roles, and data.
                  </p>
                </div>
                <button
                  onClick={() => setShowCloseConfirm(true)}
                  className="mt-auto flex items-center justify-center gap-2 px-4 py-2 border border-error text-error hover:bg-error hover:text-on-error transition-colors text-label-md font-bold rounded"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>delete_forever</span>
                  Close Company
                </button>
              </div>
            )}
          </div>

        </div>
      )}

      {/* Resign — co-owner only */}
      {isOwner && (
        <div className="mt-10 border border-outline-variant rounded-xl p-6 bg-surface-container/50">
          {showResignConfirm ? (
            <div>
              <h3 className="text-lg font-bold text-error mb-2 flex items-center gap-2">
                <span className="material-symbols-outlined">warning</span>
                Are you absolutely sure?
              </h3>
              <p className="text-body-md text-on-surface-variant mb-4">
                You will permanently lose your <strong>Owner</strong> role in <span className="text-secondary font-bold">{companyName}</span>. This cannot be undone.
              </p>
              <div className="flex gap-3">
                <button
                  onClick={() => setShowResignConfirm(false)}
                  className="px-5 py-2 border border-outline-variant text-on-surface-variant hover:bg-surface-container-high transition-colors text-label-md font-medium rounded"
                >
                  Cancel
                </button>
                <button
                  onClick={handleResign}
                  disabled={resigning}
                  className="px-5 py-2 bg-error text-on-error text-label-md font-bold hover:opacity-90 transition-opacity disabled:opacity-50 rounded flex items-center gap-2"
                >
                  {resigning && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                  {resigning ? 'Resigning...' : 'Yes, Resign from Role'}
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-on-surface mb-1">Resign from Role</h3>
                <p className="text-body-sm text-on-surface-variant">
                  Voluntarily step down as Owner of <span className="font-bold text-secondary">{companyName}</span>. This action is permanent.
                </p>
              </div>
              <button
                onClick={() => setShowResignConfirm(true)}
                className="flex items-center gap-2 px-5 py-2 border border-error text-error hover:bg-error hover:text-on-error transition-colors text-label-md font-bold rounded"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>exit_to_app</span>
                Resign
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
