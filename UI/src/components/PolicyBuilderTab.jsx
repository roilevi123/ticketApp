import { useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function PolicyBuilderTab() {
  // ניהול המצב של הטופס הנוכחי
  const [conditionType, setConditionType] = useState('min_age');
  const [conditionValue, setConditionValue] = useState('18');
  const [targetEvent, setTargetEvent] = useState(''); 

  // ניהול המצב של רשימת החוקים (עץ ה-Composite שלנו)
  const [rules, setRules] = useState([
    { id: 1, type: 'min_age', field: 'user.age', operator: 'gte', value: 18, combinator: null },
    { id: 2, type: 'max_tickets', field: 'cart.ticket_count', operator: 'lte', value: 5, combinator: 'AND' }
  ]);

  // פונקציית עזר להמרת סוג התנאי לשדות שהשרת מבין ב-JSON
  const getFieldMapping = (type) => {
    switch(type) {
      case 'min_age': return { field: 'user.age', operator: 'gte' };
      case 'max_tickets': return { field: 'cart.ticket_count', operator: 'lte' };
      case 'date_range': return { field: 'event.date', operator: 'between' };
      case 'alumni_status': return { field: 'user.is_alumni', operator: 'eq' };
      default: return { field: 'unknown', operator: 'eq' };
    }
  };

  // פונקציית עזר להצגת טקסט קריא בעץ החוקים
  const getDisplayText = (rule) => {
    switch(rule.type) {
      case 'min_age': return <>Min Age <strong className="text-secondary">&gt;= {rule.value}</strong></>;
      case 'max_tickets': return <>Max Tickets <strong className="text-secondary">&lt;= {rule.value}</strong></>;
      case 'alumni_status': return <>Alumni Status <strong className="text-secondary">== {rule.value}</strong></>;
      default: return <>{rule.type} <strong className="text-secondary">{rule.value}</strong></>;
    }
  };

  // הוספת חוק חדש לעץ
  const handleAddRule = (combinator) => {
    if (!conditionValue) return;
    const mapping = getFieldMapping(conditionType);
    
    const newRule = {
      id: Date.now(), // ID ייחודי לכל שורה
      type: conditionType,
      field: mapping.field,
      operator: mapping.operator,
      value: conditionType === 'alumni_status' ? conditionValue === 'true' : Number(conditionValue) || conditionValue,
      combinator: rules.length === 0 ? null : combinator
    };
    
    setRules([...rules, newRule]);
  };

  // מחיקת חוק מהעץ
  const handleDeleteRule = (id) => {
    const newRules = rules.filter(r => r.id !== id);
    // אם מחקנו את החוק הראשון, החוק הבא הופך לראשון ולכן ה-Combinator שלו מתאפס
    if (newRules.length > 0) newRules[0].combinator = null;
    setRules(newRules);
  };

 const generateJSON = () => {
    if (rules.length === 0) return {};

    const conditions = rules.map(r => ({
      field: r.field,
      operator: r.operator,
      value: r.value
    }));

    const mainOperator = rules.find(r => r.combinator)?.combinator || 'AND';

    return {
        targetId: targetEvent, // <-- עכשיו זה דינאמי!
        type: "EVENT",           
        ruleset: {
            operator: mainOperator,
            conditions: conditions
        }
    };
  };

  // הפונקציה שתשלח את העץ ל-Java
  const handleSavePolicy = async () => {
    if (!targetEvent) {
      alert("Please enter the Event Name before saving the policy.");
      return;
    }
    const payload = generateJSON();
    
    // מוודאים שיש חוקים לפני ששולחים
    if (Object.keys(payload).length === 0) {
      alert("Please define at least one rule before saving.");
      return;
    }

    try {
      const response = await axiosClient.post('/company/policies/purchase/bulk', payload);
      alert(`Policy saved successfully! \nID: ${response.data.policyId}`);
      // אופציונלי: לנקות את העץ אחרי שמירה מוצלחת
      // setRules([]);
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to save policy: ${msg}`);
    }
  };

  return (
    <div className="w-full">
      {/* כותרת הדף */}
      <div className="mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-display-lg text-display-lg text-on-surface mb-2">Policy Builder</h2>
          <p className="font-body-md text-body-md text-on-surface-variant">Define advanced ticketing rules and access constraints for your events.</p>
        </div>
        <div className="flex gap-3">
          <button className="px-4 py-2 border border-outline-variant text-on-surface rounded-lg font-label-md text-label-md hover:bg-surface-container-highest transition-colors" onClick={() => setRules([])}>
            Clear All
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* אזור בניית החוקים */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">rule_settings</span>
              Define New Condition
            </h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-2">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Condition Type</label>
                <div className="relative">
                  <select 
                    className="w-full bg-surface-container-highest border border-outline-variant text-on-surface rounded-lg py-3 px-4 appearance-none focus:border-secondary focus:ring-1 focus:outline-none"
                    value={conditionType}
                    onChange={(e) => setConditionType(e.target.value)}
                  >
                    <option value="min_age">Minimum Age</option>
                    <option value="max_tickets">Max Tickets Per User</option>
                    <option value="date_range">Valid Date Range</option>
                    <option value="alumni_status">Alumni Status Required</option>
                  </select>
                  <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">arrow_drop_down</span>
                </div>
              </div>
              
              <div className="flex flex-col gap-2">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Value Constraint</label>
                <input 
                  className="w-full bg-surface-container-highest border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                  type="text" 
                  value={conditionValue}
                  onChange={(e) => setConditionValue(e.target.value)}
                  placeholder="Enter value..."
                />
              </div>
            </div>
          </div>

          {/* בחירת אירוע היעד */}
          <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6 mb-6">
            <div className="flex flex-col gap-2">
              <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                Apply Policy To Event (Event Name)
              </label>
              <input 
                type="text"
                value={targetEvent}
                onChange={(e) => setTargetEvent(e.target.value)}
                className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                placeholder="e.g., STUDENTFEST"
                required
              />
            </div>
          </div>

          {/* כפתורי הוספה */}
          <div className="flex items-center gap-4 justify-center py-2 relative">
            <div className="absolute w-full h-[1px] bg-outline-variant/50 top-1/2 -translate-y-1/2 z-0"></div>
            <button 
              onClick={() => handleAddRule('AND')}
              className="relative z-10 px-4 py-2 bg-[#101415] border border-outline-variant text-on-surface rounded-full font-label-sm text-label-sm flex items-center gap-2 hover:border-secondary transition-all"
            >
              <span className="material-symbols-outlined text-[16px]">add</span>
              Add AND condition
            </button>
            <button 
              onClick={() => handleAddRule('OR')}
              className="relative z-10 px-4 py-2 bg-[#101415] border border-outline-variant text-on-surface rounded-full font-label-sm text-label-sm flex items-center gap-2 hover:border-secondary transition-all"
            >
              <span className="material-symbols-outlined text-[16px]">add_circle</span>
              Add OR condition
            </button>
          </div>

          {/* העץ הוויזואלי */}
          <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-4">Active Rule Tree</h3>
            
            {rules.length === 0 ? (
              <p className="text-on-surface-variant text-center py-8">No rules defined yet.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {rules.map((rule, index) => (
                  <div key={rule.id}>
                    {rule.combinator && (
                      <div className="ml-6 border-l-2 border-outline-variant pl-4 py-1">
                        <span className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">{rule.combinator}</span>
                      </div>
                    )}
                    <div className="bg-[#101415] border border-outline-variant rounded-lg p-4 flex items-center justify-between group hover:border-secondary transition-colors relative">
                      <div className="absolute -left-[1px] top-4 bottom-4 w-1 bg-secondary rounded-r-sm opacity-0 group-hover:opacity-100 transition-opacity"></div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-primary-container text-on-primary-fixed font-label-sm text-label-sm rounded uppercase">Condition</span>
                        <span className="font-body-md text-body-md text-on-surface">
                          {getDisplayText(rule)}
                        </span>
                      </div>
                      <button 
                        onClick={() => handleDeleteRule(rule.id)}
                        className="text-on-surface-variant hover:text-error transition-colors"
                      >
                        <span className="material-symbols-outlined">delete</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* אזור התצוגה המקדימה והשמירה */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          <div className="bg-surface-container-low border border-outline-variant rounded-xl p-0 flex flex-col h-full overflow-hidden">
            <div className="px-6 py-4 border-b border-outline-variant bg-[#1d2022] flex justify-between items-center">
              <h3 className="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined">data_object</span>
                Generated Policy Logic
              </h3>
            </div>
            
            <div className="p-6 bg-[#0B0F10] flex-grow font-mono text-sm text-primary-fixed overflow-auto min-h-[300px]">
              <pre><code>{JSON.stringify(generateJSON(), null, 2)}</code></pre>
            </div>
            
            <div className="p-6 border-t border-outline-variant bg-surface-container-low">
              <button 
                onClick={handleSavePolicy}
                className="w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg font-headline-sm text-headline-sm hover:brightness-110 transition-all flex justify-center items-center gap-2"
              >
                <span className="material-symbols-outlined font-bold">save</span>
                Save Policy Configuration
              </button>
              <p className="font-label-sm text-label-sm text-on-surface-variant text-center mt-3">This will immediately apply to selected events.</p>
            </div>
          </div>
        </div>
        
      </div>
    </div>
  );
}