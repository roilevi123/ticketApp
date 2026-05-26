import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

export default function InboxTab() {
  const [messages, setMessages] = useState([]);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // משיכת ההודעות מהשרת ברגע שהטאב נטען
  useEffect(() => {
    fetchMessages();
  }, []);

  const fetchMessages = async () => {
    setIsLoading(true);
    try {
      const response = await axiosClient.get('/company/messages');
      const data = response.data;

      // מפרקים את המחרוזת ("Complaint from X: Y") לאובייקטים
      const parsedMessages = data.map((msgString, index) => {
          const splitIndex = msgString.indexOf(':');
          const senderPart = splitIndex !== -1 ? msgString.substring(0, splitIndex) : "Unknown Sender";
          const contentPart = splitIndex !== -1 ? msgString.substring(splitIndex + 1).trim() : msgString;
          
          const buyerId = senderPart.replace("Complaint from ", "").trim();

          return {
            id: `MSG-${Date.now()}-${index}`,
            buyerId: buyerId,
            buyerName: buyerId, 
            subject: 'User Inquiry',
            content: contentPart,
            date: new Date().toLocaleDateString(),
            isRead: false,
            hasReplied: false
          };
        });

      setMessages(parsedMessages);
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

    // תואם בדיוק ל- ReplyMessageRequestDTO ב-Java שלך!
    const payload = {
      companyName: "BGU Events", 
      buyerId: selectedMessage.buyerId,
      message: replyText
    };

    try {
      await axiosClient.post('/company/reply-message', payload);
      alert('Reply sent successfully to the buyer!');
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
    return <div className="text-on-surface p-10 text-center flex items-center justify-center gap-3">
        <span className="material-symbols-outlined animate-spin text-secondary">sync</span>
        Loading Inbox...
    </div>;
  }

  return (
    <div className="w-full flex flex-col h-[75vh]">
      <div className="mb-6 flex justify-between items-end">
        <div>
          <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Producer Inbox</h2>
          <p className="text-on-surface-variant">Manage inquiries, support tickets, and direct messages from buyers.</p>
        </div>
        <div className="flex gap-4">
          <button onClick={fetchMessages} className="bg-[#101415] hover:bg-[#1d2022] transition-colors px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-2 text-on-surface">
            <span className="material-symbols-outlined text-[18px]">refresh</span> Refresh
          </button>
          <div className="bg-[#191c1e] px-4 py-2 rounded-lg border border-outline-variant flex items-center gap-3">
            <span className="material-symbols-outlined text-secondary">mail</span>
            <span className="font-bold text-on-surface">{messages.filter(m => !m.isRead).length} Unread</span>
          </div>
        </div>
      </div>

      <div className="flex flex-grow overflow-hidden bg-[#191c1e] border border-outline-variant rounded-xl">
        
        {/* אזור רשימת ההודעות (שמאל) */}
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
                  selectedMessage?.id === msg.id ? 'bg-[#1d2022] border-l-secondary' : 'hover:bg-[#101415] border-l-transparent'
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

        {/* אזור קריאת ההודעה וכתיבת תגובה (ימין) */}
        <div className="w-2/3 flex flex-col bg-[#101415]">
          {selectedMessage ? (
            <>
              <div className="p-6 border-b border-outline-variant bg-[#1d2022]">
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
                <div className="bg-[#191c1e] p-5 rounded-lg border border-outline-variant text-on-surface whitespace-pre-wrap leading-relaxed text-sm">
                  {selectedMessage.content}
                </div>
              </div>

              <div className="p-6 border-t border-outline-variant bg-[#1d2022]">
                <h4 className="text-sm font-semibold text-on-surface-variant uppercase tracking-wider mb-3">Reply to Buyer</h4>
                <textarea 
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Type your reply here..."
                  className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg p-4 min-h-[120px] focus:border-secondary focus:ring-1 focus:outline-none resize-none mb-4"
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