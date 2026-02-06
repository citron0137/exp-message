import { useMemo, useState } from 'react';
import { useAuthStore } from '../lib/authStore';

type AuthMode = 'login' | 'signup';

type AuthViewProps = {
    defaultMode?: AuthMode;
};

export default function AuthView({ defaultMode = 'login' }: AuthViewProps) {
    const [mode, setMode] = useState<AuthMode>(defaultMode);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [nickname, setNickname] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const errorMessage = useAuthStore((s) => s.errorMessage);
    const clearError = useAuthStore((s) => s.clearError);
    const signUp = useAuthStore((s) => s.signUp);
    const login = useAuthStore((s) => s.login);

    const canSubmit = useMemo(() => {
        if (!email.trim() || !password.trim()) {
            return false;
        }
        if (mode === 'signup' && !nickname.trim()) {
            return false;
        }
        return true;
    }, [email, password, nickname, mode]);

    const onSubmit = async () => {
        if (!canSubmit || isSubmitting) {
            return;
        }

        setIsSubmitting(true);
        try {
            if (mode === 'signup') {
                const ok = await signUp({ email, password, nickname });
                if (ok) {
                    setMode('login');
                    clearError();
                }
                return;
            }

            await login({ email, password });
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="flex-1 p-4 bg-white">
            <div className="mb-4">
                <h3 className="text-lg font-semibold">{mode === 'login' ? 'Sign in' : 'Sign up'}</h3>
                <p className="text-sm text-gray-500">
                    {mode === 'login' ? 'Login to continue' : 'Create an account to continue'}
                </p>
            </div>

            <div className="space-y-3">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                {mode === 'signup' && (
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Nickname</label>
                        <input
                            type="text"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                )}

                {errorMessage && (
                    <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                        {errorMessage}
                    </div>
                )}

                <button
                    onClick={onSubmit}
                    disabled={!canSubmit || isSubmitting}
                    className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg px-4 py-2 transition-colors font-medium text-sm"
                >
                    {isSubmitting ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Sign up'}
                </button>

                <div className="text-sm text-gray-600">
                    {mode === 'login' ? (
                        <button
                            type="button"
                            className="text-blue-600 hover:text-blue-700"
                            onClick={() => {
                                setMode('signup');
                                clearError();
                            }}
                        >
                            Need an account? Sign up
                        </button>
                    ) : (
                        <button
                            type="button"
                            className="text-blue-600 hover:text-blue-700"
                            onClick={() => {
                                setMode('login');
                                clearError();
                            }}
                        >
                            Already have an account? Sign in
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
