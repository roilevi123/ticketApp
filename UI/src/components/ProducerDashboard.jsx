import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import PolicyBuilderTab from './PolicyBuilderTab';
import EventInventoryTab from './EventInventoryTab';
import SalesAnalyticsTab from './SalesAnalyticsTab';
import TeamHierarchyTab from './TeamHierarchyTab';
import InboxTab from './InboxTab';

const parseJwt = (token) => {
  try { return JSON.parse(atob(token.split('.')[1])); } catch { return null; }
};

export default function ProducerDashboard() {
  const [activeTab, setActiveTab] = useState('inventory');
  const navigate = useNavigate();
  const { role, token } = useAuth();
  const activeCompany = localStorage.getItem('activeCompany');

  useEffect(() => {
    if (!activeCompany) {
      navigate('/select-company');
    }
  }, [activeCompany, navigate]);

  const isManager = role === 'MANAGER';
  const decoded = token ? parseJwt(token) : null;
  const managerPerms = decoded?.permissions || [];

  // Returns true if the current user can access this tab
  // Non-managers always can; managers need at least one of the listed permissions
  const canSee = (perms) => !isManager || perms.some(p => managerPerms.includes(p));

  const tabs = [
    ...(canSee(['MANAGE_INVENTORY', 'CONFIGURE_LAYOUT'])
      ? [{ id: 'inventory', label: 'Event & Inventory', icon: 'edit_calendar' }] : []),
    ...(canSee(['CHANGE_POLICIES'])
      ? [{ id: 'policy', label: 'Policy Builder', icon: 'rule_settings' }] : []),
    ...(canSee(['GENERATE_SALES_REPORTS', 'VIEW_PURCHASE_HISTORY'])
      ? [{ id: 'sales', label: 'Sales & Analytics', icon: 'analytics' }] : []),
    ...(!isManager
      ? [{ id: 'team', label: 'Team & Hierarchy', icon: 'account_tree' }] : []),
    ...(canSee(['RESPOND_TO_INQUIRIES'])
      ? [{ id: 'inbox', label: 'Inbox', icon: 'mail' }] : []),
  ];

  const visibleTabIds = tabs.map(t => t.id);
  const effectiveTab = visibleTabIds.includes(activeTab) ? activeTab : (visibleTabIds[0] ?? '');

  const renderActiveTab = () => {
    switch (effectiveTab) {
      case 'inventory': return <EventInventoryTab companyName={activeCompany} />;
      case 'policy':    return <PolicyBuilderTab companyName={activeCompany} />;
      case 'sales':     return <SalesAnalyticsTab companyName={activeCompany} />;
      case 'team':      return <TeamHierarchyTab companyName={activeCompany} />;
      case 'inbox':     return <InboxTab companyName={activeCompany} />;
      default:          return (
        <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
          <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '64px' }}>lock</span>
          <p className="text-headline-sm text-on-surface">No Access</p>
          <p className="text-body-md text-on-surface-variant max-w-sm">
            You have not been granted any permissions for this company. Contact the owner who appointed you.
          </p>
        </div>
      );
    }
  };

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      {/* Header */}
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
            <span className="text-label-md font-medium">Home</span>
          </button>

          <div className="flex items-center gap-3">
            <span className="text-headline-md font-bold text-secondary hidden sm:inline">Producer Dashboard</span>
            {activeCompany && (
              <span className="bg-surface-container px-3 py-1 border border-outline-variant text-label-md font-bold text-secondary rounded">
                {activeCompany}
              </span>
            )}
          </div>

          <div className="w-16" />
        </div>
      </header>

      {/* Tab navigation */}
      <div className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="max-w-container-max-width mx-auto px-margin-mobile md:px-margin-desktop">
          <nav className="-mb-px flex overflow-x-auto">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 whitespace-nowrap py-4 px-3 border-b-2 text-label-md font-medium transition-colors ${
                  effectiveTab === tab.id
                    ? 'border-secondary text-secondary'
                    : 'border-transparent text-on-surface-variant hover:text-on-surface hover:border-outline-variant'
                }`}
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{tab.icon}</span>
                <span className="hidden sm:inline">{tab.label}</span>
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Tab content */}
      <main className="flex-grow pt-8 pb-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto w-full">
        {renderActiveTab()}
      </main>
    </div>
  );
}
