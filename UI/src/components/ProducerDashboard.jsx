import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; // הוספנו ניווט
import PolicyBuilderTab from './PolicyBuilderTab';
import EventInventoryTab from './EventInventoryTab';
import SalesAnalyticsTab from './SalesAnalyticsTab';
import TeamHierarchyTab from './TeamHierarchyTab';
import InboxTab from './InboxTab';

export default function ProducerDashboard() {
  const [activeTab, setActiveTab] = useState('policy');
  const navigate = useNavigate();

  // 1. שולפים את החברה הפעילה מה-LocalStorage
  const activeCompany = localStorage.getItem("activeCompany");

  // 2. מוודאים שאם משתמש הגיע לפה בלי חברה פעילה, הוא חוזר למסך הבחירה
  useEffect(() => {
    if (!activeCompany) {
      navigate('/select-company');
    }
  }, [activeCompany, navigate]);

  const tabs = [
    { id: 'inventory', label: 'Event & Inventory' },
    { id: 'policy', label: 'Policy Builder' },
    { id: 'sales', label: 'Sales & Analytics' },
    { id: 'team', label: 'Team & Hierarchy' },
    { id: 'inbox', label: 'Inbox' },
  ];

  const renderActiveTabContent = () => {
    // 3. מעבירים את companyName בתור prop לכל הטאבים
    switch (activeTab) {
      case 'inventory': return <EventInventoryTab companyName={activeCompany} />;
      case 'policy': return <PolicyBuilderTab companyName={activeCompany} />; 
      case 'sales': return <SalesAnalyticsTab companyName={activeCompany} />;
      case 'team': return <TeamHierarchyTab companyName={activeCompany} />;
      case 'inbox': return <InboxTab companyName={activeCompany} />;
      default: return null;
    }
  };

  return (
    <div className="max-w-[1280px] mx-auto p-6 pt-10 min-h-screen bg-background">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-on-surface">Producer Dashboard</h1>
        {/* נחמד להראות באיזו חברה אנחנו נמצאים עכשיו */}
        {activeCompany && (
          <div className="bg-surface-container px-4 py-2 rounded-lg border border-outline-variant font-bold text-secondary">
            {activeCompany}
          </div>
        )}
      </div>

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