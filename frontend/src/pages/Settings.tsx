import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authService } from '../api';
import { getProfile } from '../api/quiz';
import {
  Save,
  Loader2,
  BookOpen,
  Clock,
  MessageSquare,
  CheckCircle,
  Brain,
  RefreshCw,
  TrendingUp,
} from 'lucide-react';

const Settings: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [quizProfile, setQuizProfile] = useState<{
    quizStatus: string;
    reliabilityPercentage: number;
    message: string;
  } | null>(null);
  const [formData, setFormData] = useState({
    topicsOfInterest: user?.topicsOfInterest?.join(', ') || '',
    proficiencyLevel: user?.proficiencyLevel || 'intermediate',
    preferredLanguages: user?.preferredLanguages?.join(', ') || '',
    availability: user?.availability || '',
    collaborationStyle: user?.collaborationStyle || 'balanced',
  });

  useEffect(() => {
    loadQuizProfile();
  }, []);

  const loadQuizProfile = async () => {
    try {
      const profile = await getProfile();
      setQuizProfile({
        quizStatus: profile.quizStatus,
        reliabilityPercentage: profile.reliabilityPercentage,
        message: profile.message,
      });
    } catch (error) {
      console.error('Error loading quiz profile:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setSuccess(false);

    try {
      await authService.updateProfile({
        topicsOfInterest: formData.topicsOfInterest
          .split(',')
          .map((t) => t.trim())
          .filter(Boolean),
        proficiencyLevel: formData.proficiencyLevel,
        preferredLanguages: formData.preferredLanguages
          .split(',')
          .map((l) => l.trim())
          .filter(Boolean),
        availability: formData.availability,
        collaborationStyle: formData.collaborationStyle,
      });
      await refreshUser();
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (error) {
      console.error('Error updating profile:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-500 mt-1">Manage your profile and preferences</p>
      </div>

      {/* Profile Card */}
      <div className="card p-6">
        <div className="flex items-center gap-4 mb-6">
          <div className="avatar avatar-lg">
            {user?.fullName?.charAt(0) || user?.username?.charAt(0) || 'U'}
          </div>
          <div>
            <h2 className="text-xl font-bold text-gray-900">
              {user?.fullName || user?.username}
            </h2>
            <p className="text-gray-500">{user?.email}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded-xl">
          <div className="text-center">
            <p className="text-2xl font-bold text-gray-900">
              {user?.topicsOfInterest?.length || 0}
            </p>
            <p className="text-sm text-gray-500">Topics</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-gray-900 capitalize">
              {user?.proficiencyLevel || 'N/A'}
            </p>
            <p className="text-sm text-gray-500">Level</p>
          </div>
        </div>
      </div>

      {/* Quiz Profile Card */}
      {quizProfile && (
        <div className="card p-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-600 dark:text-primary-300">
                <Brain className="w-6 h-6" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-gray-900 dark:text-white">Learning Profile Quiz</h2>
                <p className="text-sm text-gray-500 dark:text-gray-400">Helps match you with compatible study groups</p>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-800 rounded-xl">
              <div>
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Status</p>
                <p className="text-lg font-semibold text-gray-900 dark:text-white capitalize">
                  {quizProfile.quizStatus.replace('_', ' ')}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Reliability</p>
                <p className="text-lg font-semibold text-gray-900 dark:text-white">
                  {Math.round(quizProfile.reliabilityPercentage * 100)}%
                </p>
              </div>
            </div>

            {quizProfile.quizStatus !== 'COMPLETED' && (
              <div className="flex items-start gap-3 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl">
                <TrendingUp className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-blue-900 dark:text-blue-100">
                    {quizProfile.quizStatus === 'NOT_STARTED' 
                      ? 'Complete the quiz for better group matches!'
                      : quizProfile.quizStatus === 'SKIPPED'
                      ? 'Take the quiz to improve your recommendations'
                      : 'Complete more questions for better matches'}
                  </p>
                </div>
              </div>
            )}

            <button
              onClick={() => navigate('/quiz-onboarding')}
              className="btn-secondary w-full flex items-center justify-center gap-2"
            >
              <RefreshCw className="w-5 h-5" />
              {quizProfile.quizStatus === 'NOT_STARTED' ? 'Take Quiz' : 'Retake Quiz'}
            </button>
          </div>
        </div>
      )}

      {/* Preferences Form */}
      <form onSubmit={handleSubmit} className="card p-6 space-y-6">
        <h2 className="text-xl font-bold text-gray-900">Learning Preferences</h2>

        {success && (
          <div className="flex items-center gap-2 p-4 bg-green-50 border border-green-200 rounded-xl text-green-700">
            <CheckCircle className="w-5 h-5" />
            <span>Profile updated successfully!</span>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <BookOpen className="w-4 h-4 inline-block mr-2" />
            Topics of Interest
          </label>
          <input
            type="text"
            value={formData.topicsOfInterest}
            onChange={(e) =>
              setFormData({ ...formData, topicsOfInterest: e.target.value })
            }
            className="input"
            placeholder="e.g., Machine Learning, Data Structures, Algorithms"
          />
          <p className="text-xs text-gray-500 mt-1">Separate topics with commas</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Proficiency Level
          </label>
          <select
            value={formData.proficiencyLevel}
            onChange={(e) =>
              setFormData({ ...formData, proficiencyLevel: e.target.value })
            }
            className="input"
          >
            <option value="beginner">Beginner</option>
            <option value="intermediate">Intermediate</option>
            <option value="advanced">Advanced</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Preferred Languages
          </label>
          <input
            type="text"
            value={formData.preferredLanguages}
            onChange={(e) =>
              setFormData({ ...formData, preferredLanguages: e.target.value })
            }
            className="input"
            placeholder="e.g., English, Spanish"
          />
          <p className="text-xs text-gray-500 mt-1">Separate languages with commas</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <MessageSquare className="w-4 h-4 inline-block mr-2" />
            Collaboration Style
          </label>
          <select
            value={formData.collaborationStyle}
            onChange={(e) =>
              setFormData({ ...formData, collaborationStyle: e.target.value })
            }
            className="input"
          >
            <option value="quiet_focus">Quiet Focus - Prefer async communication</option>
            <option value="balanced">Balanced - Mix of sync and async</option>
            <option value="discussion_heavy">Discussion Heavy - Love live discussions</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <Clock className="w-4 h-4 inline-block mr-2" />
            Availability
          </label>
          <textarea
            value={formData.availability}
            onChange={(e) =>
              setFormData({ ...formData, availability: e.target.value })
            }
            className="input min-h-[100px] resize-none"
            placeholder="e.g., Weekdays 6pm-9pm, Weekends 10am-2pm"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {isLoading ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <>
              <Save className="w-5 h-5" />
              Save Changes
            </>
          )}
        </button>
      </form>
    </div>
  );
};

export default Settings;
