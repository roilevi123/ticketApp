import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function PolicyBuilderTab({ companyName }) {
  const [policyMode, setPolicyMode] = useState('purchase'); // 'purchase' | 'discount'

  // ==========================================
  // PURCHASE RULES STATE (LOCK - DO NOT TOUCH)
  // ==========================================
  const [targetEvent, setTargetEvent] = useState('');
  const [activeSubTab, setActiveSubTab] = useState('age'); // 'age' | 'quantity' | 'and' | 'or'

  const [existingPolicies, setExistingPolicies] = useState([]);
  const [selectedPolicyIds, setSelectedPolicyIds] = useState([]);

  const [minAge, setMinAge] = useState('18');
  const [minTickets, setMinTickets] = useState('1');
  const [maxTickets, setMaxTickets] = useState('5');

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

  // רשימת ההנחות הקיימות שנשלפות מהשרת וההנחות שנבחרו לשילוב (Sum/Max)
  const [existingDiscounts, setExistingDiscounts] = useState([]);
  const [selectedDiscountIds, setSelectedDiscountIds] = useState([]);

  // קריאת ה-GET הרשמית לשרת לפי שם האירוע המדויק (Purchase Rules)
  const fetchExistingPolicies = async () => {
    if (!targetEvent) return;
    try {
      const response = await axiosClient.get(`/company/policies/purchase/by-event?eventId=${targetEvent}`);

      let policiesArray = [];
      if (Array.isArray(response.data)) {
        policiesArray = response.data;
      } else if (response.data?.data && Array.isArray(response.data.data)) {
        policiesArray = response.data.data;
      } else if (response.data && typeof response.data === 'object' && response.data.id) {
        policiesArray = [response.data];
      }

      const formattedPolicies = policiesArray
          .map(p => ({
            id: p.policyId || p.id,
            description: p.description || p.type || (p.operator ? `Composite (${p.operator}) Rule` : 'Active Policy Logic')
          }))
          .filter(p => Boolean(p.id));

      setExistingPolicies(formattedPolicies);
    } catch (error) {
      console.error("Fetch failed, keeping local sandbox state:", error);
    }
  };

  // שליפת הנחות קיימות מהשרת לפי היעד הנוכחי של ההנחה
  const fetchExistingDiscounts = async () => {
    if (!discountTargetId || !companyName) {
      setExistingDiscounts([]);
      return;
    }

    try {
      const response = await axiosClient.get(
          `/company/policies/discount/by-event?eventId=${encodeURIComponent(discountTargetId)}&companyName=${encodeURIComponent(companyName)}`
      );

      let discountsArray = [];
      if (Array.isArray(response.data)) {
        discountsArray = response.data;
      } else if (response.data?.data && Array.isArray(response.data.data)) {
        discountsArray = response.data.data;
      } else if (response.data && typeof response.data === 'object' && (response.data.id || response.data.policyId)) {
        discountsArray = [response.data];
      }

      const formattedDiscounts = discountsArray
          .map(d => ({
            id: d.policyId || d.id,
            targetId: d.targetId,
            targetType: d.targetType || d.type,
            description: d.description || d.type || 'Active Discount Logic'
          }))
          .filter(d => Boolean(d.id));

      setExistingDiscounts(formattedDiscounts);
    } catch (error) {
      console.error("Fetch discounts failed:", error);
      setExistingDiscounts([]);
    }
  };

  // שינוי אירוע משפיע על מדיניות רכישה בלבד
  useEffect(() => {
    if (targetEvent) {
      fetchExistingPolicies();
    } else {
      setExistingPolicies([]);
    }
    setSelectedPolicyIds([]);
  }, [targetEvent]);

  // סנכרון יעד ההנחה לפי סוג היעד
  useEffect(() => {
    if (discountTargetType === 'COMPANY') {
      setDiscountTargetId(companyName || '');
    } else {
      setDiscountTargetId(targetEvent || '');
    }
  }, [discountTargetType, companyName, targetEvent]);

  // שינוי יעד הנחה משפיע על רשימת ההנחות בלבד
  useEffect(() => {
    if (discountTargetId) {
      fetchExistingDiscounts();
    } else {
      setExistingDiscounts([]);
    }
    setSelectedDiscountIds([]);
  }, [discountTargetId, companyName]);

  // ==========================================
  // PURCHASE RULES HANDLERS (LOCK - DO NOT TOUCH)
  // ==========================================
  const handleCreateAgePolicy = async () => {
    if (!targetEvent) return alert("Please specify an Event Name");
    try {
      const response = await axiosClient.post('/company/policies/purchase/age-limit', {
        targetId: targetEvent,
        type: 'EVENT',
        minAge: Number(minAge)
      });
      const serverGeneratedId = response.data?.id || response.data?.policyId || `age-${Date.now()}`;
      const newPolicyItem = { id: serverGeneratedId, description: `Minimum age required: ${minAge}` };
      setExistingPolicies(prev => [...prev, newPolicyItem]);
      alert(`Age Limit Policy Created successfully!\nID: ${serverGeneratedId}`);
    } catch (e) {
      alert(`Error: ${e.response?.data?.message || e.response?.data || e.message}`);
    }
  };

  const handleCreateQuantityPolicy = async () => {
    if (!targetEvent) return alert("Please specify an Event Name");
    try {
      const response = await axiosClient.post('/company/policies/purchase/quantity-limit', {
        targetId: targetEvent,
        type: 'EVENT',
        min: Number(minTickets),
        max: Number(maxTickets)
      });
      const serverGeneratedId = response.data?.id || response.data?.policyId || `qty-${Date.now()}`;
      const newPolicyItem = { id: serverGeneratedId, description: `Quantity limit: between ${minTickets} and ${maxTickets}` };
      setExistingPolicies(prev => [...prev, newPolicyItem]);
      alert(`Quantity Limit Policy Created successfully!\nID: ${serverGeneratedId}`);
    } catch (e) {
      alert(`Error: ${e.response?.data?.message || e.response?.data || e.message}`);
    }
  };

  const handleCreateCompositePolicy = async (operator) => {
    if (!targetEvent) return alert("Please specify an Event Name");
    if (selectedPolicyIds.length < 2) return alert("Please select at least 2 policies to combine");
    const endpoint = operator === 'AND' ? '/company/policies/purchase/combine-and' : '/company/policies/purchase/combine-or';
    try {
      const response = await axiosClient.post(endpoint, {
        targetId: targetEvent,
        type: 'EVENT',
        componentIds: selectedPolicyIds
      });
      const compositeId = response.data?.id || response.data?.policyId || `composite-${Date.now()}`;
      const combinedItem = { id: compositeId, description: `Combined Composite (${operator}) Rule` };
      setExistingPolicies(prev => [...prev, combinedItem]);
      setSelectedPolicyIds([]);
      alert(`${operator} Composite Policy Applied successfully!`);
    } catch (e) {
      alert(`Error: ${e.response?.data?.message || e.response?.data || e.message}`);
    }
  };

  const togglePolicySelection = (id) => {
    if (selectedPolicyIds.includes(id)) {
      setSelectedPolicyIds(selectedPolicyIds.filter(i => i !== id));
    } else {
      setSelectedPolicyIds([...selectedPolicyIds, id]);
    }
  };

  // ==========================================
  // DISCOUNT POLICY HANDLERS (ALIGNED WITH SERVER)
  // ==========================================
  const toggleDiscountSelection = (id) => {
    if (selectedDiscountIds.includes(id)) {
      setSelectedDiscountIds(selectedDiscountIds.filter(i => i !== id));
    } else {
      setSelectedDiscountIds([...selectedDiscountIds, id]);
    }
  };

  const handleSaveDiscountPolicy = async () => {
    if (!discountTargetId) return alert("Please provide a Target ID.");

    const basePayload = {
      targetId: discountTargetId,
      type: discountTargetType,
      companyName
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
          payload.deadline = new Date(discountDeadline).toISOString();
          break;
        case 'coupon':
          endpoint = '/company/policies/discount/coupon';
          payload.percentage = Number(discountPercentage);
          payload.code = discountCode;
          break;
        case 'combine-sum':
          if (selectedDiscountIds.length < 2) return alert("Please select at least 2 discount policies to combine");
          endpoint = '/company/policies/discount/combine-sum';
          payload.existingPolicyIds = selectedDiscountIds;
          break;
        case 'combine-max':
          if (selectedDiscountIds.length < 2) return alert("Please select at least 2 discount policies to combine");
          endpoint = '/company/policies/discount/combine-max';
          payload.existingPolicyIds = selectedDiscountIds;
          break;
        default: return;
      }

      const response = await axiosClient.post(endpoint, payload);

      // חילוץ ה-ID בהתאם ל-Controller המחזיר: Map.of("message", "...", "policyId", "...")
      const serverGeneratedId = response.data?.policyId || response.data?.id || `disc-${Date.now()}`;

      alert(`Discount policy created successfully! ID: ${serverGeneratedId}`);

      // איפוס שדות ורענון רשימת ההנחות מהשרת
      setDiscountPercentage('');
      setDiscountMinQuantity('');
      setDiscountDeadline('');
      setDiscountCode('');
      setSelectedDiscountIds([]);
      await fetchExistingDiscounts();
    } catch (error) {
      alert(`Failed: ${error.response?.data?.message || error.response?.data || error.message}`);
    }
  };

  return (
      <div className="w-full p-4">
        {/* Main Tabs Header */}
        <div className="mb-8 flex flex-col justify-between gap-4">
          <div>
            <h2 className="font-bold text-2xl text-white mb-2">Policy Builder</h2>
            <p className="text-gray-400 text-sm">Configure direct purchase restrictions and dynamic discounts.</p>
          </div>

          <div className="flex bg-gray-800 p-1 rounded-lg w-fit">
            <button
                onClick={() => setPolicyMode('purchase')}
                className={`px-6 py-2 rounded-md transition-colors text-sm font-semibold ${policyMode === 'purchase' ? 'bg-yellow-500 text-black shadow' : 'text-gray-400 hover:text-white'}`}
            >
              Purchase Rules
            </button>
            <button
                onClick={() => setPolicyMode('discount')}
                className={`px-6 py-2 rounded-md transition-colors text-sm font-semibold ${policyMode === 'discount' ? 'bg-yellow-500 text-black shadow' : 'text-gray-400 hover:text-white'}`}
            >
              Discount Policy
            </button>
          </div>
        </div>

        {/* PURCHASE RULES MODE (LOCK - DO NOT TOUCH) */}
        {policyMode === 'purchase' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 text-white">
              {/* Form Side */}
              <div className="lg:col-span-7 flex flex-col gap-6">
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                  <label className="text-xs text-gray-400 uppercase tracking-wider block mb-2 font-semibold">
                    1. Target Event Name (Required for all actions)
                  </label>
                  <input
                      type="text"
                      value={targetEvent}
                      onChange={(e) => setTargetEvent(e.target.value)}
                      className="w-full bg-black border border-gray-800 text-white rounded-lg py-3 px-4 focus:border-yellow-500 focus:outline-none"
                      placeholder="e.g., TECH_CONFERENCE"
                  />
                </div>

                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                  <label className="text-xs text-gray-400 uppercase tracking-wider block mb-4 font-semibold">
                    2. Select Policy Type to Configure
                  </label>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2 mb-6">
                    {['age', 'quantity', 'and', 'or'].map((tab) => (
                        <button
                            key={tab}
                            onClick={() => setActiveSubTab(tab)}
                            className={`py-2 px-3 text-center rounded-lg text-xs font-semibold border uppercase ${activeSubTab === tab ? 'bg-blue-600 text-white border-blue-600' : 'bg-black text-gray-300 border-gray-800'}`}
                        >
                          {tab === 'age' ? 'Age Limit' : tab === 'quantity' ? 'Ticket Range' : `Combine (${tab.toUpperCase()})`}
                        </button>
                    ))}
                  </div>

                  <div className="bg-black border border-gray-800 p-4 rounded-xl">
                    {activeSubTab === 'age' && (
                        <div className="flex flex-col gap-4">
                          <h4 className="text-md font-medium text-gray-200">New Age Limit Constraint</h4>
                          <div>
                            <label className="text-xs text-gray-400 block mb-1">Minimum Age Required</label>
                            <input type="number" value={minAge} onChange={(e) => setMinAge(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none" />
                          </div>
                          <button onClick={handleCreateAgePolicy} className="mt-2 py-2 px-4 bg-yellow-500 text-black font-bold rounded-lg hover:bg-yellow-400 transition-colors">
                            Create Age Limit Policy
                          </button>
                        </div>
                    )}

                    {activeSubTab === 'quantity' && (
                        <div className="flex flex-col gap-4">
                          <h4 className="text-md font-medium text-gray-200">New Ticket Quantity Range Constraint</h4>
                          <div className="grid grid-cols-2 gap-4">
                            <div>
                              <label className="text-xs text-gray-400 block mb-1">Min Tickets</label>
                              <input type="number" value={minTickets} onChange={(e) => setMinTickets(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none" />
                            </div>
                            <div>
                              <label className="text-xs text-gray-400 block mb-1">Max Tickets</label>
                              <input type="number" value={maxTickets} onChange={(e) => setMaxTickets(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none" />
                            </div>
                          </div>
                          <button onClick={handleCreateQuantityPolicy} className="mt-2 py-2 px-4 bg-yellow-500 text-black font-bold rounded-lg hover:bg-yellow-400 transition-colors">
                            Create Quantity Range Policy
                          </button>
                        </div>
                    )}

                    {(activeSubTab === 'and' || activeSubTab === 'or') && (
                        <div className="flex flex-col gap-4">
                          <h4 className="text-md font-medium text-gray-200">Create {activeSubTab.toUpperCase()} Composite Policy</h4>
                          <p className="text-xs text-gray-400">Select multiple items from the right panel checkbox list below to bundle them.</p>
                          <div className="p-3 bg-gray-900 rounded-lg border border-gray-800 text-yellow-500 font-mono text-xs">
                            Selected elements count: {selectedPolicyIds.length}
                          </div>
                          <button onClick={() => handleCreateCompositePolicy(activeSubTab.toUpperCase())} className="mt-2 py-2 px-4 bg-yellow-500 text-black font-bold rounded-lg hover:bg-yellow-400 transition-colors">
                            Build {activeSubTab.toUpperCase()} Composite
                          </button>
                        </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Side Panel */}
              <div className="lg:col-span-5 flex flex-col gap-6">
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 h-full flex flex-col">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-sm font-medium flex items-center gap-2">Server Active Policies</h3>
                    <button onClick={fetchExistingPolicies} className="text-xs text-blue-400 hover:underline">Refresh From Server</button>
                  </div>

                  {!targetEvent ? (
                      <p className="text-gray-500 text-center my-auto py-8 text-sm">Enter an Event Name to view its current policies.</p>
                  ) : existingPolicies.length === 0 ? (
                      <p className="text-gray-500 text-center my-auto py-8 text-sm">No active policies found on the server for this event.</p>
                  ) : (
                      <div className="flex flex-col gap-3 overflow-y-auto max-h-[450px]">
                        {existingPolicies.map((p) => {
                          const isSelected = selectedPolicyIds.includes(p.id);
                          const isCompositeMode = activeSubTab === 'and' || activeSubTab === 'or';
                          return (
                              <div
                                  key={p.id}
                                  onClick={() => isCompositeMode && togglePolicySelection(p.id)}
                                  className={`p-4 rounded-xl border transition-all ${isCompositeMode ? 'cursor-pointer hover:border-blue-500' : 'border-gray-800'} ${isSelected ? 'bg-blue-900/40 border-blue-600' : 'bg-black'}`}
                              >
                                <div className="flex items-start justify-between gap-2">
                                  <div className="overflow-hidden">
                                    <span className="px-2 py-0.5 text-[10px] bg-gray-800 text-gray-400 rounded font-mono block w-fit mb-1 truncate max-w-full">
                                      ID: {p.id.substring(0, 8)}...
                                    </span>
                                    <p className="text-sm text-gray-200">{p.description}</p>
                                  </div>
                                  {isCompositeMode && (
                                      <input type="checkbox" checked={isSelected} readOnly className="w-4 h-4 rounded text-blue-600 focus:ring-0 mt-1" />
                                  )}
                                </div>
                              </div>
                          );
                        })}
                      </div>
                  )}
                </div>
              </div>
            </div>
        )}

        {/* DISCOUNT POLICY MODE (UPDATED SECTIONS) */}
        {policyMode === 'discount' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 text-white">

              {/* Left Side: Form Configuration */}
              <div className="lg:col-span-7 flex flex-col gap-6">
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
                  <h3 className="text-lg font-medium mb-6">Create Discount Policy</h3>

                  {/* Target level selection */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    <div className="flex flex-col gap-2">
                      <label className="text-xs uppercase font-semibold text-gray-400">Target Level</label>
                      <select className="w-full bg-black border border-gray-800 text-white rounded-lg py-3 px-4 focus:outline-none focus:border-yellow-500" value={discountTargetType} onChange={(e) => setDiscountTargetType(e.target.value)}>
                        <option value="EVENT">Specific Event</option>
                        <option value="COMPANY">Entire Company</option>
                      </select>
                    </div>
                    <div className="flex flex-col gap-2">
                      <label className="text-xs uppercase font-semibold text-gray-400">Target ID / Event Name</label>
                      <input type="text" value={discountTargetId} onChange={(e) => setDiscountTargetId(e.target.value)} disabled={discountTargetType === 'COMPANY'} className="w-full bg-black border border-gray-800 text-white rounded-lg py-3 px-4 disabled:opacity-50 focus:outline-none focus:border-yellow-500" placeholder="e.g., TECH_CONFERENCE" />
                    </div>
                  </div>

                  {/* Logic Type */}
                  <div className="flex flex-col gap-2 mb-6">
                    <label className="text-xs uppercase font-semibold text-gray-400">Discount Logic Type</label>
                    <select className="w-full bg-black border border-gray-800 text-white rounded-lg py-3 px-4 focus:outline-none focus:border-yellow-500" value={discountLogicType} onChange={(e) => setDiscountLogicType(e.target.value)}>
                      <option value="simple">Simple / Unconditional Discount</option>
                      <option value="quantity">Quantity Based Discount</option>
                      <option value="time-limited">Time-Limited Discount (Early Bird)</option>
                      <option value="coupon">Coupon Code</option>
                      <option value="combine-sum">Combine Existing (SUM)</option>
                      <option value="combine-max">Combine Existing (MAX)</option>
                    </select>
                  </div>

                  {/* Dynamic inputs based on selected logic type */}
                  <div className="bg-black border border-gray-800 p-4 rounded-xl mb-6">
                    {['simple', 'quantity', 'time-limited', 'coupon'].includes(discountLogicType) && (
                        <div className="mb-4">
                          <label className="text-xs text-gray-400 block mb-1">Discount Percentage (%)</label>
                          <input type="number" step="0.01" value={discountPercentage} onChange={(e) => setDiscountPercentage(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none focus:border-yellow-500" placeholder="e.g., 15" />
                        </div>
                    )}

                    {discountLogicType === 'quantity' && (
                        <div>
                          <label className="text-xs text-gray-400 block mb-1">Minimum Tickets Required</label>
                          <input type="number" value={discountMinQuantity} onChange={(e) => setDiscountMinQuantity(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none focus:border-yellow-500" placeholder="e.g., 3" />
                        </div>
                    )}

                    {discountLogicType === 'time-limited' && (
                        <div>
                          <label className="text-xs text-gray-400 block mb-1">Deadline Date & Time</label>
                          <input type="datetime-local" value={discountDeadline} onChange={(e) => setDiscountDeadline(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none focus:border-yellow-500" />
                        </div>
                    )}

                    {discountLogicType === 'coupon' && (
                        <div>
                          <label className="text-xs text-gray-400 block mb-1">Coupon Code</label>
                          <input type="text" value={discountCode} onChange={(e) => setDiscountCode(e.target.value)} className="w-full bg-gray-900 border border-gray-800 rounded-lg p-2 text-white focus:outline-none focus:border-yellow-500" placeholder="e.g., EARLY50" />
                        </div>
                    )}

                    {['combine-sum', 'combine-max'].includes(discountLogicType) && (
                        <div className="text-xs text-gray-400 p-2 bg-gray-900 border border-gray-800 rounded-lg">
                          <p className="font-semibold text-yellow-500 mb-1">Composition Sandbox Mode:</p>
                          Select multiple discount items from the <span className="font-bold text-white">Active Discounts</span> section on the right side panel to chain them together.
                          <div className="mt-2 text-white font-mono">Selected: {selectedDiscountIds.length} policies</div>
                        </div>
                    )}
                  </div>

                  <div className="flex justify-end">
                    <button onClick={handleSaveDiscountPolicy} className="w-full md:w-auto px-8 py-3 bg-yellow-500 text-black font-bold rounded-lg hover:bg-yellow-400 transition-all">
                      Build & Push Discount Policy
                    </button>
                  </div>
                </div>
              </div>

              {/* Right Side: Active Server Discounts & Checkbox Chaining */}
              <div className="lg:col-span-5 flex flex-col gap-6">
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 h-full flex flex-col">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-sm font-medium flex items-center gap-2">Server Active Discounts</h3>
                    <button onClick={fetchExistingDiscounts} className="text-xs text-blue-400 hover:underline">Refresh Discounts</button>
                  </div>

                  {!discountTargetId ? (
                      <p className="text-gray-500 text-center my-auto py-8 text-sm">Enter a Target ID or Event Name to pull its active discount schemas.</p>
                  ) : existingDiscounts.length === 0 ? (
                      <p className="text-gray-500 text-center my-auto py-8 text-sm">No active discount policies configured for this target scope.</p>
                  ) : (
                      <div className="flex flex-col gap-3 overflow-y-auto max-h-[450px]">
                        {existingDiscounts.map((d) => {
                          const isSelected = selectedDiscountIds.includes(d.id);
                          const isCombineMode = ['combine-sum', 'combine-max'].includes(discountLogicType);
                          return (
                              <div
                                  key={d.id}
                                  onClick={() => isCombineMode && toggleDiscountSelection(d.id)}
                                  className={`p-4 rounded-xl border transition-all ${isCombineMode ? 'cursor-pointer hover:border-blue-500' : 'border-gray-800'} ${isSelected ? 'bg-blue-900/40 border-blue-600' : 'bg-black'}`}
                              >
                                <div className="flex items-start justify-between gap-2">
                                  <div className="overflow-hidden">
                                    <span className="px-2 py-0.5 text-[10px] bg-gray-800 text-gray-400 rounded font-mono block w-fit mb-1 truncate max-w-full">
                                      ID: {d.id ? d.id.substring(0, 8) : 'Unknown'}... ({d.targetType})
                                    </span>
                                    <p className="text-sm text-gray-200">{d.description}</p>
                                  </div>
                                  {isCombineMode && (
                                      <input type="checkbox" checked={isSelected} readOnly className="w-4 h-4 rounded text-blue-600 focus:ring-0 mt-1" />
                                  )}
                                </div>
                              </div>
                          );
                        })}
                      </div>
                  )}
                </div>
              </div>
            </div>
        )}
      </div>
  );
}
