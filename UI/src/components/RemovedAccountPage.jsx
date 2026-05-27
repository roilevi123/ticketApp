import { Link } from 'react-router-dom';

export default function RemovedAccountPage() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-4">
      <div className="max-w-md w-full text-center">
        <div className="mb-6 flex justify-center">
          <span
            className="material-symbols-outlined text-error"
            style={{ fontSize: '80px', fontVariationSettings: "'FILL' 0" }}
          >
            block
          </span>
        </div>
        <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-error mb-4 tracking-tight">
          Account Removed
        </h1>
        <p className="text-body-lg text-on-surface-variant mb-8">
          Your account has been removed by an administrator. You no longer have access to UNI-TICKETS.
        </p>
        <p className="text-body-md text-on-surface-variant mb-10">
          If you believe this was a mistake, please contact your university's support office.
        </p>
        <Link
          to="/login"
          className="inline-flex items-center gap-2 text-label-md font-medium text-on-surface-variant hover:text-secondary transition-colors border border-outline-variant px-6 py-2.5 rounded-lg"
        >
          <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
          Back to Login
        </Link>
      </div>
    </div>
  );
}
