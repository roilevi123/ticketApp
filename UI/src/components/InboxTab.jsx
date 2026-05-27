import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function InboxTab({ companyName }) {
  const [messages, setMessages] = useState([]);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchMessages();
  }, []);

  const parseNotification = (msgString, index) => {
    let title = 'User Inquiry';
    let content = msgString;
    let buyerId = 'unknown';

    try {
      const parsed = JSON.parse(msgString);
      title = parsed.title || 'User Inquiry';
      content = parsed.message || msgString;
      const fromMatch = title.match(/from\s+(.+)/i);
      if (fromMatch) buyerId = fromMatch[1].trim();
    } catch {
      const splitIndex = msgString.indexOf(':');
      if (splitIndex !== -1) {
        const senderPart = msgString.substring(0, splitIndex);
        content = msgString.substring(splitIndex + 1).trim();
        buyerId = senderPart.replace(/complaint from /i, '').trim();
        title = 'User Inquiry';
      }
    }

    return {
      id: `MSG-${Date.now()}-${index}`,
      buyerId,
      buyerName: buyerId,
      subject: title,
      content,
      date: new Date().toLocaleDateString(),
      isRead: false,
      hasReplied: false
    };
  };

  const fetchMessages = async () => {
    setIsLoading(true);
    try {
      const response = await axiosClient.get('/company/messages');
      const parsed = response.data.map(parseNotification);
      setMessages(parsed);
      setSelectedMessage(null);
    } catch (error) {
      console.error("Failed to fetch messages", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectMessage = (msg) => {
    setSelectedMessage(msg);
    setReplyText('');
    if (!msg.isRead) {
      setMessages(messages.map(m => m.id === msg.id ? { ...m, isRead: true } : m));
    }
  };

  const handleSendReply = async () => {
    if (!replyText.trim()) return;
    setIsSending(true);

    const payload = {
      companyName,
      buyerId: selectedMessage.buyerId,
      message: replyText
    };

    try {
      await axiosClient.post('/company/reply-message', payload);
      alert('Reply sent successfully!');
      setMessages(messages.map(m => m.id === selectedMessage.id ? { ...m, hasReplied: true } : m));
      setReplyText('');
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to send reply: ${msg}`);
    } finally {
      setIsSending(false);
    }
  };

  if (isLoading) {
    return (
      <div className="text-on-surface p-10 text-center flex items-center justify-center gap-3">
        <span className="material-symbols-outlined animate-spin text-secondary">sync</span>
        Loading Inbox...
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col h-[75vh]">
      <div className="mb-6 flex justify-between items-end">
        <div>
          <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Producer Inbox</h2>
          <p className="text-on-surface-variant">Manage inquiries, support tickets, and direct messages from buyers.</p>
        </div>
        <div className="flex gap-4">
          <button onClick={fetchMessages} className="bg-background hover:bg-surface-container transition-colors px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-2 text-on-surface">
            <span className="material-symbols-outlined text-[18px]">refresh</span> Refresh
          </button>
          <div className="bg-surface-container px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-3">
            <span className="material-symbols-outlined text-secondary">mail</span>
            <span className="font-bold text-on-surface">{messages.filter(m => !m.isRead).length} Unread</span>
          </div>
        </div>
      </div>

      <div className="flex flex-grow overflow-hidden bg-surface-container border border-outline-variant rounded-xl">
        <div className="w-1/3 border-r border-outline-variant flex flex-col overflow-y-auto">
          {messages.length === 0 ? (
            <div className="p-8 text-center text-on-surface-variant opacity-70">
              <span className="material-symbols-outlined text-4xl mb-2">inbox</span>
              <p>No new messages</p>
            </div>
          ) : (
            messages.map((msg) => (
              <div
                key={msg.id}
                onClick={() => handleSelectMessage(msg)}
                className={`p-4 border-b border-outline-variant cursor-pointer transition-colors border-l-4 ${
                  selectedMessage?.id === msg.id ? 'bg-surface-container-high border-l-secondary' : 'hover:bg-background border-l-transparent'
                }`}
              >
                <div className="flex justify-between items-start mb-1">
                  <span className={`font-semibold text-sm truncate pr-2 ${!msg.isRead ? 'text-on-surface font-bold' : 'text-on-surface-variant'}`}>
                    {msg.buyerName}
                  </span>
                  <span className="text-xs text-on-surface-variant whitespace-nowrap">{msg.date}</span>
                </div>
                <div className="text-sm font-medium text-on-surface truncate mb-1">{msg.subject}</div>
                <div className="text-xs text-on-surface-variant truncate">{msg.content}</div>
                <div className="mt-2 flex gap-2">
                  {!msg.isRead && <span className="w-2 h-2 rounded-full bg-secondary"></span>}
                  {msg.hasReplied && <span className="material-symbols-outlined text-[14px] text-green-400">reply</span>}
                </div>
              </div>
            ))
          )}
        </div>

        <div className="w-2/3 flex flex-col bg-background">
          {selectedMessage ? (
            <>
              <div className="p-6 border-b border-outline-variant bg-surface-container">
                <h3 className="text-xl font-bold text-on-surface mb-2">{selectedMessage.subject}</h3>
                <div className="flex justify-between items-center text-sm">
                  <div className="flex items-center gap-2 text-on-surface-variant">
                    <span className="material-symbols-outlined text-[18px]">person</span>
                    {selectedMessage.buyerId}
                  </div>
                  <span className="text-on-surface-variant">{selectedMessage.date}</span>
                </div>
              </div>

              <div className="p-6 flex-grow overflow-y-auto">
                <div className="bg-surface-container p-5 rounded-lg border border-outline-variant text-on-surface whitespace-pre-wrap leading-relaxed text-sm">
                  {selectedMessage.content}
                </div>
              </div>

              <div className="p-6 border-t border-outline-variant bg-surface-container">
                <h4 className="text-sm font-semibold text-on-surface-variant uppercase tracking-wider mb-3">Reply to Buyer</h4>
                <textarea
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Type your reply here..."
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg p-4 min-h-[120px] focus:border-secondary focus:ring-1 focus:outline-none resize-none mb-4"
                ></textarea>
                <div className="flex justify-end">
                  <button
                    onClick={handleSendReply}
                    disabled={isSending || !replyText.trim()}
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
              <p>Select a message from the list to read and reply</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
