import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';

const GoogleCallback: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { refreshUser } = useAuth();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState<string>('');

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token');

      if (!token) {
        setStatus('error');
        setMessage('Authentication token is missing. Please try signing in again.');
        return;
      }

      try {
        // Save token to localStorage
        localStorage.setItem('token', token);
        
        // Refresh user data in auth context
        await refreshUser();
        
        setStatus('success');
        setMessage('Successfully signed in with Google!');
        
        // Redirect to dashboard after a brief delay
        setTimeout(() => {
          navigate('/dashboard');
        }, 1500);
      } catch (error: any) {
        setStatus('error');
        localStorage.removeItem('token');
        const errorMessage = error.response?.data?.message || 
                           'Failed to complete Google sign-in. Please try again.';
        setMessage(errorMessage);
      }
    };

    handleCallback();
  }, [searchParams, navigate, refreshUser]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full bg-white dark:bg-slate-800 rounded-lg shadow-lg p-8 text-center">
        {status === 'loading' && (
          <>
            <div className="flex justify-center mb-4">
              <Loader2 className="w-16 h-16 text-primary-500 animate-spin" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Signing you in...</h1>
            <p className="text-gray-600 dark:text-gray-400">Please wait while we complete your Google sign-in...</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="flex justify-center mb-4">
              <CheckCircle2 className="w-16 h-16 text-green-500" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Welcome!</h1>
            <p className="text-gray-600 dark:text-gray-400 mb-6">{message}</p>
            <p className="text-sm text-gray-500 dark:text-gray-500">
              Redirecting to dashboard...
            </p>
            <button
              onClick={() => navigate('/dashboard')}
              className="mt-4 w-full btn-primary"
            >
              Go to Dashboard
            </button>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="flex justify-center mb-4">
              <XCircle className="w-16 h-16 text-red-500" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Sign-in Failed</h1>
            <p className="text-gray-600 dark:text-gray-400 mb-6">{message}</p>
            <div className="space-y-3">
              <button
                onClick={() => navigate('/login')}
                className="w-full btn-primary"
              >
                Try Again
              </button>
              <button
                onClick={() => navigate('/register')}
                className="w-full btn-secondary"
              >
                Create Account
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default GoogleCallback;

