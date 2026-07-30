import type { PriceMode } from '../api/types';

const DATA_MODES: { value: PriceMode; label: string; hint: string }[] = [
    {
        value: 'BEST_AVAILABLE',
        label: 'Best available',
        hint: 'Uses live quotes first, then valid cache when live data is not available.',
    },
    {
        value: 'LIVE_ONLY',
        label: 'Live only',
        hint: 'Only accepts live quotes; unavailable prices remain clearly unpriced.',
    },
    {
        value: 'DEMO_ALLOWED',
        label: 'Demo allowed',
        hint: 'Allows deterministic demo prices for classroom walkthroughs.',
    },
];

const MOTION_PREFS = [
    'Cards: 160ms fade/slide',
    'Charts: 600ms ease-out',
    'Reduced motion: disable non-essential transitions',
];

interface SettingsPageProps {
    isDark?: boolean;
    onThemeChange?: (dark: boolean) => void;
    dataMode: PriceMode;
    onDataModeChange: (mode: PriceMode) => void;
}

export function SettingsPage({ isDark, onThemeChange, dataMode, onDataModeChange }: SettingsPageProps) {
    const theme = isDark ? 'night' : 'day';
    const selectedMode = DATA_MODES.find((mode) => mode.value === dataMode) ?? DATA_MODES[0];

    const handleThemeClick = (newTheme: 'day' | 'night') => {
        onThemeChange?.(newTheme === 'night');
    };

    return (
        <div className="settings-page-wrapper">
            <section className="settings-page-section">
                <div className="settings-card settings-card-wide">
                    <h2 className="settings-card-title">Theme system</h2>
                    <p className="settings-card-subtitle">Logo variants switch with the theme.</p>
                    <div className="theme-preview-row">
                        <div className={`theme-logo-preview ${theme === 'day' ? 'active' : ''}`}>
                            <img src="/FullLogoWhite.png" alt="HoldHive Light" className="theme-logo-img"/>
                        </div>
                        <div className={`theme-logo-preview dark ${theme === 'night' ? 'active' : ''}`}>
                            <img src="/FullLogoBlack.png" alt="HoldHive Dark" className="theme-logo-img"/>
                        </div>
                        <div className="theme-toggle-group">
                            <button
                                className={`theme-mode-btn ${theme === 'day' ? 'active' : ''}`}
                                onClick={() => handleThemeClick('day')}
                            >
                                Day
                            </button>
                            <button
                                className={`theme-mode-btn ${theme === 'night' ? 'active' : ''}`}
                                onClick={() => handleThemeClick('night')}
                            >
                                Night
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            <section className="settings-bottom-row">
                <div className="settings-card">
                    <h2 className="settings-card-title">Data mode</h2>
                    <div className="data-mode-buttons">
                        {DATA_MODES.map((mode) => (
                            <button
                                key={mode.value}
                                className={`data-mode-btn ${dataMode === mode.value ? 'active' : ''}`}
                                onClick={() => onDataModeChange(mode.value)}
                            >
                                {mode.label}
                            </button>
                        ))}
                    </div>
                    <p className="settings-hint">{selectedMode.hint}</p>
                </div>

                <div className="settings-card">
                    <h2 className="settings-card-title">Motion preferences</h2>
                    <ul className="motion-list">
                        {MOTION_PREFS.map((pref) => (
                            <li key={pref} className="motion-item">{pref}</li>
                        ))}
                    </ul>
                </div>
            </section>
        </div>
    );
}
