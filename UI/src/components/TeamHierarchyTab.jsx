import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

const AVAILABLE_PERMISSIONS = [
    "MANAGE_INVENTORY",
    "CONFIGURE_LAYOUT",
    "CHANGE_POLICIES",
    "RESPOND_TO_INQUIRIES",
    "VIEW_PURCHASE_HISTORY",
    "GENERATE_SALES_REPORTS"
];

export default function TeamHierarchyTab() {
  const [hierarchyTree, setHierarchyTree] = useState("Loading organizational tree...");
  const [formData, setFormData] = useState({
    targetUserId: '',
    role: 'MANAGER',
    permissions: []
  });

  const companyName = "BGU Events"; 

  useEffect(() => {
    fetchHierarchy();
  }, []);

  const fetchHierarchy = async () => {
    try {
      const response = await axiosClient.get(`/company/${encodeURIComponent(companyName)}/hierarchy`);
      setHierarchyTree(response.data.tree || "No tree data available.");
    } catch (error) {
      setHierarchyTree("Network error. Server might be offline.");
    }
  };

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handlePermissionToggle = (permission) => {
    setFormData(prev => {
      const currentPermissions = prev.permissions;
      if (currentPermissions.includes(permission)) {
        return { ...prev, permissions: currentPermissions.filter(p => p !== permission) };
      } else {
        return { ...prev, permissions: [...currentPermissions, permission] };
      }
    });
  };

  const handleAssignRole = async (e) => {
    e.preventDefault();

    const payload = {
      targetUserId: formData.targetUserId,
      companyName: companyName,
      role: formData.role,
      permissions: formData.role === 'MANAGER' ? formData.permissions : []
    };

    try {
      await axiosClient.post('/company/assign-role', payload);
      alert(`${formData.role} assigned successfully to user ${formData.targetUserId}!`);
      setFormData({ targetUserId: '', role: 'MANAGER', permissions: [] });
      fetchHierarchy();
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to assign role: ${msg}`);
    }
  };

  return (
    <div className="w-full">
      <div className="mb-8">
        <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Team & Hierarchy</h2>
        <p className="text-on-surface-variant">Manage your organization's structure, assign roles, and configure manager permissions.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* אזור 1: טופס מינוי תפקיד */}
        <div className="lg:col-span-5">
          <div className="bg-[#191c1e] border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">person_add</span>
              Assign New Role
            </h3>

            <form onSubmit={handleAssignRole} className="flex flex-col gap-5">
              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Target User ID (Email / Username)</label>
                <input 
                  type="text" name="targetUserId" value={formData.targetUserId} onChange={handleInputChange}
                  className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                  placeholder="e.g., student2@bgu.ac.il" required
                />
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Assign Role</label>
                <select 
                  name="role" value={formData.role} onChange={handleInputChange}
                  className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                >
                  <option value="MANAGER">Manager (Custom Permissions)</option>
                  <option value="OWNER">Co-Owner (Full Access)</option>
                </select>
              </div>

              {/* הרשאות יופיעו רק אם נבחר מנהל */}
              {formData.role === 'MANAGER' && (
                <div className="flex flex-col gap-3 mt-2 bg-[#101415] p-4 rounded-lg border border-outline-variant">
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
                        <div className="w-5 h-5 border-2 border-outline-variant rounded bg-[#191c1e] peer-checked:bg-secondary peer-checked:border-secondary transition-all flex items-center justify-center">
                          <span className="material-symbols-outlined text-on-secondary text-[14px] opacity-0 peer-checked:opacity-100">check</span>
                        </div>
                      </div>
                      <span className="text-on-surface group-hover:text-secondary transition-colors text-sm font-mono">{permission}</span>
                    </label>
                  ))}
                </div>
              )}

              <button type="submit" className="mt-4 w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex justify-center items-center gap-2">
                <span className="material-symbols-outlined">how_to_reg</span>
                Assign Role & Send Invite
              </button>
            </form>
          </div>
        </div>

        {/* אזור 2: העץ הארגוני */}
        <div className="lg:col-span-7">
          <div className="bg-[#1d2022] border border-outline-variant rounded-xl p-6 h-full flex flex-col">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary-fixed">account_tree</span>
                Organizational Hierarchy
              </h3>
              <button onClick={fetchHierarchy} className="text-on-surface-variant hover:text-secondary transition-colors" title="Refresh Tree">
                <span className="material-symbols-outlined">refresh</span>
              </button>
            </div>
            
            <div className="flex-grow bg-[#0B0F10] border border-outline-variant/50 rounded-lg p-5 overflow-auto font-mono text-sm text-green-400 whitespace-pre">
              {hierarchyTree}
            </div>
            
            <p className="text-xs text-on-surface-variant mt-4 text-center">
              The tree displays the chain of command. Owners can manage their sub-managers.
            </p>
          </div>
        </div>
        
      </div>
    </div>
  );
}