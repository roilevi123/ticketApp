import { useState } from 'react';
import { Link } from 'react-router-dom';
import SystemHealthTab from './admin/SystemHealthTab';
import FinancialsTab from './admin/FinancialsTab';
import UserManagementTab from './admin/UserManagementTab';
import SupportInboxTab from './admin/SupportInboxTab';

const TABS = [
  { id: 'system-health',   label: 'System Health' },
  { id: 'financials',      label: 'Financials' },
  { id: 'user-management', label: 'User Management' },
  { id: 'support-inbox',   label: 'Support Inbox' },
];

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('system-health');

  const renderTab = () => {
    switch (activeTab) {
      case 'system-health':   return <SystemHealthTab />;
      case 'financials':      return <FinancialsTab />;
      case 'user-management': return <UserManagementTab />;
      case 'support-inbox':   return <SupportInboxTab />;
      default: return null;
    }
  };

  return (
    <div className="bg-background min-h-screen">
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex items-center justify-between h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <Link to="/" className="text-headline-md font-bold text-secondary">UNI-TICKETS</Link>
          <div className="flex items-center gap-2 text-on-surface-variant">
            <span className="material-symbols-outlined text-secondary" style={{ fontSize: "20px" }}>admin_panel_settings</span>
            <span className="text-label-md font-medium text-secondary">Admin Control Center</span>
          </div>
          <Link
            to="/"
            className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors text-label-md"
          >
            <span className="material-symbols-outlined" style={{ fontSize: "20px" }}>arrow_back</span>
            <span className="hidden sm:inline">Back to Events</span>
          </Link>
        </div>
      </header>
    <div className="max-w-container-max-width mx-auto px-margin-mobile md:px-margin-desktop pt-10 pb-16">
      <div className="mb-8">
        <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-on-surface tracking-tight mb-2">
          Admin Control Center
        </h1>
        <p className="text-body-lg text-on-surface-variant">
          System-wide overview and management for the academic ticketing infrastructure.
        </p>
      </div>

      <div className="border-b border-outline-variant mb-6 overflow-x-auto">
        <nav className="-mb-px flex whitespace-nowrap">
          {TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`whitespace-nowrap py-4 px-6 border-b-2 text-label-md font-medium transition-all ${
                activeTab === tab.id
                  ? 'border-secondary text-secondary'
                  : 'border-transparent text-on-surface-variant hover:text-on-surface hover:border-outline'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      <div className="mt-4">
        {renderTab()}
      </div>
    </div>
    </div>
  );
}
