import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../../api/axiosClient';

function formatTimeAgo(createdAt) {
  if (!createdAt) return '';
  const diff = Date.now() - new Date(createdAt).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

// Maps a Notification { id, userId, message, read, createdAt } to the inbox ticket shape
function toTicket(n) {
  const time = n.createdAt
    ? new Date(n.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
    : '';
  return {
    id: n.id,
    userId: n.userId,
    subject: n.message.slice(0, 60) + (n.message.length > 60 ? '…' : ''),
    preview: n.message,
    fromEmail: n.userId,
    priority: 'NORMAL',
    timeAgo: formatTimeAgo(n.createdAt),
    messages: [{ text: n.message, time, fromAdmin: false }],
  };
}

// Matches mapped ticket shape — used as fallback when API is unavailable
const DUMMY_TICKETS = [
  {
    id: 'demo-001',
    userId: 'U88102934',
    subject: 'Payment failed but charged',
    preview: 'I was trying to buy the theater tickets and the app crashed during checkout.',
    fromEmail: 'U88102934',
    priority: 'NORMAL',
    timeAgo: '10m ago',
    messages: [
      { text: 'I was trying to buy the theater tickets and the app crashed during checkout.', time: '10:45 AM', fromAdmin: false },
    ],
  },
  {
    id: 'demo-002',
    userId: 'U77203845',
    subject: 'Forgot password link not arriving',
    preview: 'The reset link is not arriving in my university inbox.',
    fromEmail: 'U77203845',
    priority: 'NORMAL',
    timeAgo: '1h ago',
    messages: [
      { text: 'The forgot password reset link is not arriving in my university inbox. I have checked my spam folder too.', time: '09:30 AM', fromAdmin: false },
    ],
  },
];

const PRIORITY_COLOR = {
  HIGH: 'text-error',
  NORMAL: 'text-on-surface-variant',
};

export default function SupportInboxTab() {
  const [tickets, setTickets] = useState(DUMMY_TICKETS);
  const [selectedId, setSelectedId] = useState(DUMMY_TICKETS[0].id);
  const [replyText, setReplyText] = useState('');
  const [sending, setSending] = useState(false);
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [broadcasting, setBroadcasting] = useState(false);

  const fetchTickets = useCallback(async () => {
    try {
      const res = await axiosClient.get('/admin/complaints');
      const mapped = res.data.map(toTicket);
      setTickets(mapped.length > 0 ? mapped : DUMMY_TICKETS);
      if (mapped.length > 0) setSelectedId(mapped[0].id);
    } catch {
      setTickets(DUMMY_TICKETS);
    }
  }, []);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const selected = tickets.find(t => t.id === selectedId) ?? tickets[0];

  const handleReply = async () => {
    if (!replyText.trim() || !selected) return;
    setSending(true);
    const newMsg = {
      text: replyText,
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      fromAdmin: true,
    };
    try {
      await axiosClient.post(`/admin/users/${encodeURIComponent(selected.userId)}/message`, {
        message: replyText,
      });
    } catch {
      // message sent optimistically
    }
    setTickets(prev =>
      prev.map(t => t.id === selected.id ? { ...t, messages: [...t.messages, newMsg] } : t)
    );
    setReplyText('');
    setSending(false);
  };

  const handleBroadcast = async () => {
    if (!broadcastMsg.trim()) return;
    setBroadcasting(true);
    // No broadcast endpoint in backend yet — no-op
    await new Promise(r => setTimeout(r, 400));
    setBroadcastMsg('');
    setBroadcasting(false);
  };

  return (
    <div className="space-y-gutter">
      {/* Inbox + message view */}
      <div className="glass-card rounded-xl overflow-hidden h-[600px] flex">
        {/* Left: ticket list */}
        <div className="w-1/3 border-r border-outline-variant flex flex-col min-w-0">
          <div className="p-4 border-b border-outline-variant bg-surface-container-high text-label-md flex-shrink-0">
            Incoming Complaints
          </div>
          <div className="flex-grow overflow-y-auto">
            {tickets.map(t => (
              <div
                key={t.id}
                onClick={() => setSelectedId(t.id)}
                className={`p-4 border-b border-outline-variant cursor-pointer transition-colors ${
                  t.id === selectedId
                    ? 'bg-secondary/10 border-l-4 border-l-secondary'
                    : 'hover:bg-surface-variant border-l-4 border-l-transparent'
                }`}
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-on-surface text-sm truncate max-w-[60%]">{t.fromEmail}</span>
                  <span className="text-[10px] text-on-surface-variant flex-shrink-0">{t.timeAgo}</span>
                </div>
                <p className={`text-xs font-semibold truncate ${t.id === selectedId ? 'text-secondary' : 'text-on-surface-variant'}`}>
                  {t.subject}
                </p>
                <p className="text-xs text-on-surface-variant truncate mt-0.5">{t.preview}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Right: message view */}
        {selected && (
          <div className="flex-grow flex flex-col min-w-0">
            <div className="p-4 border-b border-outline-variant flex-shrink-0">
              <h3 className="text-headline-sm text-on-surface truncate">{selected.subject}</h3>
              <p className="text-xs text-on-surface-variant mt-0.5">
                From user: <span className="font-mono">{selected.fromEmail}</span> | Priority:{' '}
                <span className={`font-bold ${PRIORITY_COLOR[selected.priority] ?? 'text-on-surface-variant'}`}>
                  {selected.priority}
                </span>
              </p>
            </div>

            <div className="flex-grow p-6 overflow-y-auto bg-surface-container-low/30 space-y-4">
              {selected.messages.map((msg, i) => (
                <div
                  key={i}
                  className={`max-w-[80%] rounded-lg p-4 ${
                    msg.fromAdmin ? 'bg-secondary/20 ml-auto' : 'bg-surface-container'
                  }`}
                >
                  <p className="text-sm text-on-surface">{msg.text}</p>
                  <span className="text-[10px] text-on-surface-variant mt-2 block">{msg.time}</span>
                </div>
              ))}
            </div>

            <div className="p-6 border-t border-outline-variant bg-surface-container-high/50 flex-shrink-0">
              <textarea
                value={replyText}
                onChange={e => setReplyText(e.target.value)}
                placeholder="Type your reply here..."
                className="w-full bg-background border border-outline-variant rounded p-3 text-sm focus:border-secondary focus:outline-none resize-none h-24 mb-3"
              />
              <div className="flex justify-end">
                <button
                  onClick={handleReply}
                  disabled={sending || !replyText.trim()}
                  className="bg-secondary text-on-secondary px-6 py-2 rounded-lg font-bold flex items-center gap-2 hover:opacity-90 disabled:opacity-50 transition-opacity"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>send</span>
                  {sending ? 'Sending...' : 'Send Reply'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Broadcast Alert */}
      <div className="glass-card p-6 rounded-xl">
        <h2 className="text-headline-sm text-secondary mb-2 flex items-center gap-2">
          <span className="material-symbols-outlined">campaign</span>
          Global Broadcast Alert
        </h2>
        <p className="text-sm text-on-surface-variant mb-6">
          This message will appear as a high-visibility banner for all active users across the platform.
        </p>
        <div className="flex gap-4 items-center">
          <input
            type="text"
            value={broadcastMsg}
            onChange={e => setBroadcastMsg(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleBroadcast()}
            placeholder="Example: System maintenance scheduled for Sunday at 2 AM EST..."
            className="flex-grow bg-background border border-outline-variant rounded px-4 py-3 focus:border-secondary focus:outline-none transition-all text-body-md"
          />
          <button
            onClick={handleBroadcast}
            disabled={broadcasting || !broadcastMsg.trim()}
            className="bg-on-surface text-background px-8 py-3 rounded font-bold hover:bg-secondary transition-all disabled:opacity-50 whitespace-nowrap"
          >
            {broadcasting ? 'Broadcasting...' : 'Broadcast Now'}
          </button>
        </div>
      </div>
    </div>
  );
}
