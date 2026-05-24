import { useState } from 'react';
import PolicyBuilderTab from './PolicyBuilderTab';
import EventInventoryTab from './EventInventoryTab';
import SalesAnalyticsTab from './SalesAnalyticsTab';
import TeamHierarchyTab from './TeamHierarchyTab';
import InboxTab from './InboxTab';


export default function ProducerDashboard() {
  const [activeTab, setActiveTab] = useState('policy'); 

  const tabs = [
    { id: 'inventory', label: 'Event & Inventory' },
    { id: 'policy', label: 'Policy Builder' },
    { id: 'sales', label: 'Sales & Analytics' },
    { id: 'team', label: 'Team & Hierarchy' },
    { id: 'inbox', label: 'Inbox' },
  ];

  const renderActiveTabContent = () => {
    switch (activeTab) {
      case 'inventory': return <EventInventoryTab />;
      case 'policy': return <PolicyBuilderTab />; 
      case 'sales': return <SalesAnalyticsTab />;
      case 'team': return <TeamHierarchyTab />;
      case 'inbox': return <InboxTab />;
      default: return null;
    }
  };

  return (
    <div className="max-w-[1280px] mx-auto p-6 pt-10 min-h-screen bg-background">
      <h1 className="text-3xl font-bold text-on-surface mb-8">Producer Dashboard</h1>

     
      <div className="border-b border-outline-variant mb-6">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`
                whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm
                ${activeTab === tab.id
                  ? 'border-secondary text-secondary'
                  : 'border-transparent text-on-surface-variant hover:text-on-surface hover:border-outline'}
              `}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

     
      <div className="mt-4">
        {renderActiveTabContent()}
      </div>
    </div>
  );
}