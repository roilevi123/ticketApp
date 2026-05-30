import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function PolicyBuilderTab({ companyName }) {
  const [policyMode, setPolicyMode] = useState('purchase'); // 'purchase' | 'discount'

  // ==========================================
  // PURCHASE RULES STATE
  // ==========================================
  const [conditionType, setConditionType] = useState('min_age');
  const [conditionValue, setConditionValue] = useState('18');
  const [targetEvent, setTargetEvent] = useState('');

  const [rules, setRules] = useState([
    { id: 1, type: 'min_age', field: 'user.age', operator: 'gte', value: 18, combinator: null },
    { id: 2, type: 'max_tickets', field: 'cart.ticket_count', operator: 'lte', value: 5, combinator: 'AND' }
  ]);

  // ==========================================
  // DISCOUNT POLICY STATE
  // ==========================================
  const [discountTargetType, setDiscountTargetType] = useState('EVENT');
  const [discountTargetId, setDiscountTargetId] = useState('');
  const [discountLogicType, setDiscountLogicType] = useState('simple');
  const [discountPercentage, setDiscountPercentage] = useState('');
  const [discountMinQuantity, setDiscountMinQuantity] = useState('');
  const [discountDeadline, setDiscountDeadline] = useState('');
  const [discountCode, setDiscountCode] = useState('');
  const [discountExistingIds, setDiscountExistingIds] = useState('');

  // Auto-fill Target ID when switching to COMPANY target type
  useEffect(() => {
    if (discountTargetType === 'COMPANY') {
      setDiscountTargetId(companyName);
    } else {
      setDiscountTargetId('');
    }
  }, [discountTargetType, companyName]);

  // ==========================================
  // PURCHASE RULES LOGIC
  // ==========================================
  const getFieldMapping = (type) => {
    switch (type) {
      case 'min_age': return { field: 'user.age', operator: 'gte' };
      case 'max_tickets': return { field: 'cart.ticket_count', operator: 'lte' };
      case 'min_tickets': return { field: 'cart.ticket_count', operator: 'gte' };
      case 'date_range': return { field: 'event.date', operator: 'between' };
      case 'alumni_status': return { field: 'user.is_alumni', operator: 'eq' };
      default: return { field: 'unknown', operator: 'eq' };
    }
  };

  const getDisplayText = (rule) => {
    switch (rule.type) {
      case 'min_age': return <>Min Age <strong className="text-secondary">&gt;= {rule.value}</strong></>;
      case 'max_tickets': return <>Max Tickets <strong className="text-secondary">&lt;= {rule.value}</strong></>;
      case 'min_tickets': return <>Min Tickets <strong className="text-secondary">&gt;= {rule.value}</strong></>;
      case 'alumni_status': return <>Alumni Status <strong className="text-secondary">== {rule.value}</strong></>;
      default: return <>{rule.type} <strong className="text-secondary">{rule.value}</strong></>;
    }
  };

  const handleAddRule = (combinator) => {
    if (!conditionValue) return;
    const mapping = getFieldMapping(conditionType);
    const newRule = {
      id: Date.now(),
      type: conditionType,
      field: mapping.field,
      operator: mapping.operator,
      value: conditionType === 'alumni_status' ? conditionValue === 'true' : Number(conditionValue) || conditionValue,
      combinator: rules.length === 0 ? null : combinator
    };
    setRules([...rules, newRule]);
  };

  const handleDeleteRule = (id) => {
    const newRules = rules.filter(r => r.id !== id);
    if (newRules.length > 0) newRules[0].combinator = null;
    setRules(newRules);
  };

  const generatePurchaseJSON = () => {
    if (rules.length === 0) return {};
    const conditions = rules.map(r => ({
      field: r.field,
      operator: r.operator,
      value: r.value
    }));
    const mainOperator = rules.find(r => r.combinator)?.combinator || 'AND';
    return {
      targetId: targetEvent,
      type: "EVENT",
      ruleset: {
        operator: mainOperator,
        conditions
      }
    };
  };

  const handleSavePurchasePolicy = async () => {
    if (!targetEvent) {
      alert("Please enter the Event Name before saving the policy.");
      return;
    }
    const payload = generatePurchaseJSON();
    if (Object.keys(payload).length === 0) {
      alert("Please define at least one rule before saving.");
      return;
    }
    try {
      const response = await axiosClient.post('/company/policies/purchase/bulk', payload);
      alert(`Policy saved successfully! \nID: ${response.data.policyId}`);
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to save policy: ${msg}`);
    }
  };

  // ==========================================
  // DISCOUNT POLICY LOGIC
  // ==========================================
  const handleSaveDiscountPolicy = async () => {
    if (!discountTargetId) {
      alert("Please provide a Target ID (Event Name or Company).");
      return;
    }

    const basePayload = {
      targetId: discountTargetId,
      type: discountTargetType,
      companyName: companyName
    };

    let endpoint = '';
    let payload = { ...basePayload };

    try {
      switch (discountLogicType) {
        case 'simple':
          endpoint = '/company/policies/discount/simple';
          payload.percentage = Number(discountPercentage);
          break;
        case 'quantity':
          endpoint = '/company/policies/discount/quantity';
          payload.percentage = Number(discountPercentage);
          payload.minQuantity = Number(discountMinQuantity);
          break;
        case 'time-limited':
          endpoint = '/company/policies/discount/time-limited';
          payload.percentage = Number(discountPercentage);
          // Convert local datetime-local to ISO if needed, or leave as string depending on backend
          payload.deadline = new Date(discountDeadline).toISOString();
          break;
        case 'coupon':
          endpoint = '/company/policies/discount/coupon';
          payload.percentage = Number(discountPercentage);
          payload.code = discountCode;
          break;
        case 'combine-sum':
          endpoint = '/company/policies/discount/combine-sum';
          payload.existingPolicyIds = discountExistingIds.split(',').map(id => id.trim()).filter(id => id);
          break;
        case 'combine-max':
          endpoint = '/company/policies/discount/combine-max';
          payload.existingPolicyIds = discountExistingIds.split(',').map(id => id.trim()).filter(id => id);
          break;
        default:
          return;
      }

      const response = await axiosClient.post(endpoint, payload);
      alert(`Discount policy created successfully! \nID: ${response.data.policyId}`);
      
      // Reset form on success
      setDiscountPercentage('');
      setDiscountMinQuantity('');
      setDiscountDeadline('');
      setDiscountCode('');
      setDiscountExistingIds('');
    } catch (error) {
      const errorData = error.response?.data;
      // ממיר אובייקט JSON למחרוזת קריאה, או משתמש בטקסט אם זה רק טקסט
      const msg = typeof errorData === 'object' ? JSON.stringify(errorData, null, 2) : errorData || error.message;
      alert(`Failed to save discount policy:\n${msg}`);
    }
  };

  return (
    <div className="w-full">
      {/* Header and Toggle */}
      <div className="mb-8 flex flex-col justify-between gap-4">
        <div>
          <h2 className="font-display-lg text-display-lg text-on-surface mb-2">Policy Builder</h2>
          <p className="font-body-md text-body-md text-on-surface-variant">Define advanced ticketing rules, access constraints, and dynamic discounts.</p>
        </div>
        
        <div className="flex bg-surface-container-highest p-1 rounded-lg w-fit">
          <button
            onClick={() => setPolicyMode('purchase')}
            className={`px-6 py-2 rounded-md font-label-md transition-colors flex items-center gap-2 ${policyMode === 'purchase' ? 'bg-secondary text-on-secondary shadow' : 'text-on-surface-variant hover:text-on-surface'}`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>rule</span>
            Purchase Rules
          </button>
          <button
            onClick={() => setPolicyMode('discount')}
            className={`px-6 py-2 rounded-md font-label-md transition-colors flex items-center gap-2 ${policyMode === 'discount' ? 'bg-secondary text-on-secondary shadow' : 'text-on-surface-variant hover:text-on-surface'}`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>loyalty</span>
            Discount Policy
          </button>
        </div>
      </div>

      {/* ========================================== */}
      {/* PURCHASE RULES UI */}
      {/* ========================================== */}
      {policyMode === 'purchase' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 animate-fade-in">
          <div className="lg:col-span-7 flex flex-col gap-6">
            <div className="flex justify-end mb-[-1rem] z-10 relative">
              <button className="px-4 py-2 border border-outline-variant text-on-surface rounded-lg font-label-md text-label-md hover:bg-surface-container-highest transition-colors" onClick={() => setRules([])}>
                Clear All
              </button>
            </div>
            
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
                      <option value="min_tickets">Min Tickets Per Purchase</option>
                      <option value="date_range">Valid Date Range</option>
                      <option value="alumni_status">Alumni Status Required</option>
                    </select>
                    <span
                        className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">arrow_drop_down</span>
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

            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6">
              <div className="flex flex-col gap-2">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                  Apply Policy To Event (Event Name)
                </label>
                <input
                  type="text"
                  value={targetEvent}
                  onChange={(e) => setTargetEvent(e.target.value)}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                  placeholder="e.g., STUDENTFEST"
                  required
                />
              </div>
            </div>

            <div className="flex items-center gap-4 justify-center py-2 relative">
              <div className="absolute w-full h-[1px] bg-outline-variant/50 top-1/2 -translate-y-1/2 z-0"></div>
              <button
                onClick={() => handleAddRule('AND')}
                className="relative z-10 px-4 py-2 bg-background border border-outline-variant text-on-surface rounded-full font-label-sm text-label-sm flex items-center gap-2 hover:border-secondary transition-all"
              >
                <span className="material-symbols-outlined text-[16px]">add</span>
                Add AND condition
              </button>
              <button
                onClick={() => handleAddRule('OR')}
                className="relative z-10 px-4 py-2 bg-background border border-outline-variant text-on-surface rounded-full font-label-sm text-label-sm flex items-center gap-2 hover:border-secondary transition-all"
              >
                <span className="material-symbols-outlined text-[16px]">add_circle</span>
                Add OR condition
              </button>
            </div>

            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6">
              <h3 className="font-headline-sm text-headline-sm text-on-surface mb-4">Active Rule Tree</h3>

              {rules.length === 0 ? (
                <p className="text-on-surface-variant text-center py-8">No rules defined yet.</p>
              ) : (
                <div className="flex flex-col gap-3">
                  {rules.map((rule) => (
                    <div key={rule.id}>
                      {rule.combinator && (
                        <div className="ml-6 border-l-2 border-outline-variant pl-4 py-1">
                          <span className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">{rule.combinator}</span>
                        </div>
                      )}
                      <div className="bg-background border border-outline-variant rounded-lg p-4 flex items-center justify-between group hover:border-secondary transition-colors relative">
                        <div className="absolute -left-[1px] top-4 bottom-4 w-1 bg-secondary rounded-r-sm opacity-0 group-hover:opacity-100 transition-opacity"></div>
                        <div className="flex items-center gap-3">
                          <span className="px-2 py-1 bg-primary-container text-on-primary-fixed font-label-sm text-label-sm rounded uppercase">Condition</span>
                          <span className="font-body-md text-body-md text-on-surface">{getDisplayText(rule)}</span>
                        </div>
                        <button onClick={() => handleDeleteRule(rule.id)} className="text-on-surface-variant hover:text-error transition-colors">
                          <span className="material-symbols-outlined">delete</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="lg:col-span-5 flex flex-col gap-6">
            <div className="bg-surface-container-low border border-outline-variant rounded-xl p-0 flex flex-col h-full overflow-hidden">
              <div className="px-6 py-4 border-b border-outline-variant bg-surface-container flex justify-between items-center">
                <h3 className="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined">data_object</span>
                  Generated Policy Logic
                </h3>
              </div>

              <div className="p-6 bg-background flex-grow font-mono text-sm text-secondary overflow-auto min-h-[300px]">
                <pre><code>{JSON.stringify(generatePurchaseJSON(), null, 2)}</code></pre>
              </div>

              <div className="p-6 border-t border-outline-variant bg-surface-container-low">
                <button
                  onClick={handleSavePurchasePolicy}
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
      )}

      {/* ========================================== */}
      {/* DISCOUNT POLICY UI */}
      {/* ========================================== */}
      {policyMode === 'discount' && (
        <div className="max-w-4xl mx-auto flex flex-col gap-6 animate-fade-in">
          
          <div className="bg-surface-container-low border border-outline-variant rounded-xl p-6">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">sell</span>
              Create Discount Policy
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div className="flex flex-col gap-2">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Target Level</label>
                <div className="relative">
                  <select
                    className="w-full bg-surface-container-highest border border-outline-variant text-on-surface rounded-lg py-3 px-4 appearance-none focus:border-secondary focus:ring-1 focus:outline-none"
                    value={discountTargetType}
                    onChange={(e) => setDiscountTargetType(e.target.value)}
                  >
                    <option value="EVENT">Specific Event</option>
                    <option value="COMPANY">Entire Company</option>
                  </select>
                  <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">arrow_drop_down</span>
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                  Target ID (Event / Company Name)
                </label>
                <input
                  type="text"
                  value={discountTargetId}
                  onChange={(e) => setDiscountTargetId(e.target.value)}
                  disabled={discountTargetType === 'COMPANY'}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none disabled:opacity-50"
                  placeholder={discountTargetType === 'EVENT' ? "e.g., TECH_CONF_2026" : companyName}
                />
              </div>
            </div>

            <div className="h-px bg-outline-variant/50 w-full mb-6"></div>

            <div className="flex flex-col gap-2 mb-6">
              <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Discount Logic Type</label>
              <div className="relative">
                <select
                  className="w-full bg-surface-container-highest border border-outline-variant text-on-surface rounded-lg py-3 px-4 appearance-none focus:border-secondary focus:ring-1 focus:outline-none"
                  value={discountLogicType}
                  onChange={(e) => setDiscountLogicType(e.target.value)}
                >
                  <option value="simple">Simple / Unconditional Discount</option>
                  <option value="quantity">Quantity Based Discount</option>
                  <option value="time-limited">Time-Limited Discount (Early Bird)</option>
                  <option value="coupon">Coupon Code</option>
                  <option value="combine-sum">Combine Policies (Sum)</option>
                  <option value="combine-max">Combine Policies (Max)</option>
                </select>
                <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">arrow_drop_down</span>
              </div>
            </div>

            {/* Dynamic Form Fields Based on Selected Discount Type */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-surface-container-highest/30 p-4 rounded-lg border border-outline-variant">
              
              {/* Percentage (Visible for non-composite types) */}
              {!discountLogicType.startsWith('combine') && (
                <div className="flex flex-col gap-2">
                  <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Discount %</label>
                  <input
                    type="number"
                    value={discountPercentage}
                    onChange={(e) => setDiscountPercentage(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., 15"
                    min="0"
                    max="100"
                  />
                </div>
              )}

              {/* Quantity Specific */}
              {discountLogicType === 'quantity' && (
                <div className="flex flex-col gap-2">
                  <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Minimum Quantity</label>
                  <input
                    type="number"
                    value={discountMinQuantity}
                    onChange={(e) => setDiscountMinQuantity(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., 5 tickets"
                    min="1"
                  />
                </div>
              )}

              {/* Time Limited Specific */}
              {discountLogicType === 'time-limited' && (
                <div className="flex flex-col gap-2">
                  <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Deadline</label>
                  <input
                    type="datetime-local"
                    value={discountDeadline}
                    onChange={(e) => setDiscountDeadline(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                  />
                </div>
              )}

              {/* Coupon Specific */}
              {discountLogicType === 'coupon' && (
                <div className="flex flex-col gap-2">
                  <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Coupon Code</label>
                  <input
                    type="text"
                    value={discountCode}
                    onChange={(e) => setDiscountCode(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., SUMMER2026"
                  />
                </div>
              )}

              {/* Composite Specific (Combine Max/Sum) */}
              {discountLogicType.startsWith('combine') && (
                <div className="flex flex-col gap-2 md:col-span-2">
                  <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Existing Policy IDs (Comma separated)</label>
                  <input
                    type="text"
                    value={discountExistingIds}
                    onChange={(e) => setDiscountExistingIds(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., uuid-1234, uuid-5678"
                  />
                </div>
              )}
            </div>
            
            <div className="mt-8 flex justify-end">
              <button
                onClick={handleSaveDiscountPolicy}
                className="w-full md:w-auto px-8 py-3 bg-secondary text-on-secondary font-bold rounded-lg font-headline-sm text-headline-sm hover:brightness-110 transition-all flex justify-center items-center gap-2"
              >
                <span className="material-symbols-outlined font-bold">add_circle</span>
                Create Policy
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}