import { useEffect, useState } from 'react';

export default function GlobalErrorBanner() {
    const [message, setMessage] = useState(null);

    useEffect(() => {
        let timer;

        const handler = (event) => {
            setMessage(event.detail.message);

            clearTimeout(timer);
            timer = setTimeout(() => {
                setMessage(null);
            }, 5000);
        };

        window.addEventListener('global-error', handler);

        return () => {
            clearTimeout(timer);
            window.removeEventListener('global-error', handler);
        };
    }, []);

    if (!message) return null;

    return (
        <div className="fixed top-4 left-1/2 z-[9999] -translate-x-1/2 rounded-xl bg-red-600 px-6 py-3 text-white shadow-lg">
            {message}
        </div>
    );
}