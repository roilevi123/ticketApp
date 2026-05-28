import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

function parseComplaint(notification, index) {
  let title = 'Complaint';
  let senderId = null;
  let displaySubject = '';
  let body = '';

  try {
    const parsed = JSON.parse(notification.message);
    title = parsed.title || 'Complaint';
    senderId = parsed.senderId || null;
    const raw = parsed.message || notification.message;
    const match = raw.match(/^\[([^\]]+)\]\s*([\s\S]*)/);
    if (match) { displaySubject = match[1].trim(); body = match[2].trim(); }
    else { body = raw; }
  } catch {
    body = notification.message || '';
  }

  const time = notification.createdAt
    ? new Date(notification.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
    : new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  return {
    id: notification.id || `COMPLAINT-${Date.now()}-${index}`,
    senderId,
    subject: title,
    displaySubject,
    body,
    date: notification.createdAt
      ? new Date(notification.createdAt).toLocaleDateString()
      : new Date().toLocaleDateString(),
    // thread = array of chat bubbles; starts with the complaint itself
    thread: [{ text: body, fromProducer: false, time }],
  };
}

export default function InboxTab({ companyName }) {
  const [complaints, setComplaints] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const selected = complaints.find(c => c.id === selectedId) ?? null;

  useEffect(() => { fetchComplaints(); }, []);

  const fetchComplaints = async () => {
    if (!companyName) { setIsLoading(false); return; }
    setIsLoading(true);
    try {
      const res = await axiosClient.get(`/company/${encodeURIComponent(companyName)}/complaints`);
      const parsed = (res.data ?? []).map(parseComplaint);
      setComplaints(parsed);
      setSelectedId(null);
    } catch (err) {
      console.error('Failed to fetch complaints', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelect = (c) => {
    setSelectedId(c.id);
    setReplyText('');
  };

  const handleSendReply = async () => {
    if (!replyText.trim() || !selected?.senderId) return;
    setIsSending(true);
    const newMsg = {
      text: replyText,
      fromProducer: true,
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
    try {
      await axiosClient.post('/company/reply-message', {
        companyName,
        buyerId: selected.senderId,
        message: replyText,
      });
      // Append the reply bubble to the thread immediately
      setComplaints(prev =>
        prev.map(c =>
          c.id === selected.id ? { ...c, thread: [...c.thread, newMsg] } : c
        )
      );
      setReplyText('');
    } catch (err) {
      const msg = err.response?.data || err.message || 'Network error.';
      alert(`Failed to send reply: ${msg}`);
    } finally {
      setIsSending(false);
    }
  };

  if (isLoading) {
    return (
      <div className="text-on-surface p-10 text-center flex items-center justify-center gap-3">
        <span className="material-symbols-outlined animate-spin text-secondary">sync</span>
        Loading Complaints…
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col h-[75vh]">
      {/* ── Header ── */}
      <div className="mb-6 flex justify-between items-end">
        <div>
          <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Producer Inbox</h2>
          <p className="text-on-surface-variant">Complaints submitted by buyers about your events.</p>
        </div>
        <div className="flex gap-4">
          <button
            onClick={fetchComplaints}
            className="bg-background hover:bg-surface-container transition-colors px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-2 text-on-surface"
          >
            <span className="material-symbols-outlined text-[18px]">refresh</span> Refresh
          </button>
          <div className="bg-surface-container px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-3">
            <span className="material-symbols-outlined text-secondary">report</span>
            <span className="font-bold text-on-surface">{complaints.length} Complaint{complaints.length !== 1 ? 's' : ''}</span>
          </div>
        </div>
      </div>

      {/* ── Two-panel layout ── */}
      <div className="flex flex-grow overflow-hidden bg-surface-container border border-outline-variant rounded-xl">

        {/* Left: complaint list */}
        <div className="w-1/3 border-r border-outline-variant flex flex-col overflow-y-auto">
          {complaints.length === 0 ? (
            <div className="p-8 text-center text-on-surface-variant opacity-70 flex flex-col items-center gap-2">
              <span className="material-symbols-outlined text-4xl">report_off</span>
              <p>No complaints received</p>
            </div>
          ) : (
            complaints.map(c => (
              <div
                key={c.id}
                onClick={() => handleSelect(c)}
                className={`p-4 border-b border-outline-variant cursor-pointer transition-colors border-l-4 ${
                  selectedId === c.id
                    ? 'bg-surface-container-high border-l-secondary'
                    : 'hover:bg-background border-l-transparent'
                }`}
              >
                <div className="flex justify-between items-start mb-1">
                  <span className="font-bold text-sm text-on-surface truncate pr-2">{c.subject}</span>
                  <span className="text-xs text-on-surface-variant whitespace-nowrap">{c.date}</span>
                </div>
                {c.displaySubject && (
                  <div className="text-xs text-secondary font-semibold truncate mb-0.5">{c.displaySubject}</div>
                )}
                <div className="text-xs text-on-surface-variant truncate">{c.body}</div>
                {c.thread.length > 1 && (
                  <div className="mt-1 flex items-center gap-1">
                    <span className="material-symbols-outlined text-[12px] text-green-400">reply</span>
                    <span className="text-[11px] text-green-400">{c.thread.length - 1} repl{c.thread.length - 1 === 1 ? 'y' : 'ies'}</span>
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Right: thread + reply */}
        <div className="w-2/3 flex flex-col bg-background">
          {selected ? (
            <>
              {/* Header */}
              <div className="p-6 border-b border-outline-variant bg-surface-container flex-shrink-0">
                <h3 className="text-xl font-bold text-on-surface mb-1">{selected.subject}</h3>
                {selected.displaySubject && (
                  <p className="text-sm text-secondary font-semibold mb-2">{selected.displaySubject}</p>
                )}
                <div className="flex justify-between items-center text-sm">
                  <div className="flex items-center gap-2 text-on-surface-variant">
                    <span className="material-symbols-outlined text-[18px]">person</span>
                    <span className="font-mono text-xs">{selected.senderId ?? 'Unknown sender'}</span>
                  </div>
                  <span className="text-on-surface-variant">{selected.date}</span>
                </div>
              </div>

              {/* Chat thread */}
              <div className="flex-grow p-6 overflow-y-auto space-y-4 bg-surface-container-low/20">
                {selected.thread.map((msg, i) => (
                  <div
                    key={i}
                    className={`max-w-[80%] rounded-lg p-4 ${
                      msg.fromProducer
                        ? 'bg-secondary/20 ml-auto'
                        : 'bg-surface-container'
                    }`}
                  >
                    <p className="text-sm text-on-surface whitespace-pre-wrap">{msg.text}</p>
                    <span className="text-[10px] text-on-surface-variant mt-2 block">{msg.time}</span>
                  </div>
                ))}
              </div>

              {/* Reply box */}
              <div className="p-6 border-t border-outline-variant bg-surface-container flex-shrink-0">
                {!selected.senderId && (
                  <p className="text-xs text-error mb-2">Cannot reply — sender ID not available.</p>
                )}
                <textarea
                  value={replyText}
                  onChange={e => setReplyText(e.target.value)}
                  placeholder="Type your reply here..."
                  disabled={!selected.senderId}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg p-4 min-h-[100px] focus:border-secondary focus:ring-1 focus:outline-none resize-none mb-4 disabled:opacity-50"
                />
                <div className="flex justify-end">
                  <button
                    onClick={handleSendReply}
                    disabled={isSending || !replyText.trim() || !selected.senderId}
                    className="px-6 py-2 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined">{isSending ? 'hourglass_empty' : 'send'}</span>
                    {isSending ? 'Sending...' : 'Send Reply'}
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-grow flex flex-col items-center justify-center text-on-surface-variant opacity-50">
              <span className="material-symbols-outlined text-6xl mb-4">forum</span>
              <p>Select a complaint from the list to read and reply</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
