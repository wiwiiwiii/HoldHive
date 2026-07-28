import { useState, useCallback } from 'react';
import {
  Hexagon,
  LayoutDashboard,
  ListChecks,
  TrendingUp,
  BarChart3,
  Settings,
  Plus,
  Moon,
  Sun,
  Activity,
} from 'lucide-react';
import { Toaster } from 'react-hot-toast';
import { DashboardPage } from './components/DashboardPage';
import { GatewayPage } from './components/GatewayPage';
import { HoldingsPage } from './components/HoldingsPage';
import { PerformancePage } from './components/PerformancePage';
import { AnalysisPage } from './components/AnalysisPage';
import { SettingsPage } from './components/SettingsPage';
import { AddHoldingPage } from './components/AddHoldingPage';


const NAV_ITEMS = [
  { label: 'Gateway', icon: Hexagon },
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'Holdings', icon: ListChecks },
  { label: 'Performance', icon: TrendingUp },
  { label: 'Analysis', icon: BarChart3 },
  { label: 'Settings', icon: Settings },
];

const PAGE_INFO: Record<string, { title: string; subtitle: string }> = {
  Gateway: { title: 'Hive Gateway', subtitle: 'Choose a workspace from the hive map' },
  Dashboard: { title: 'Dashboard', subtitle: 'Value, allocation, and data status' },
  Holdings: { title: 'Holdings', subtitle: 'Review positions and maintain records' },
  Performance: { title: 'Performance', subtitle: 'Track returns and trends' },
  Analysis: { title: 'Analysis', subtitle: 'Risk view and portfolio X-Ray' },
  Settings: { title: 'Settings', subtitle: 'Theme and preferences' },
};

export function App() {
  const [activeNav, setActiveNav] = useState('Gateway');
  const [isDark, setIsDark] = useState(false);
  const [showAddHolding, setShowAddHolding] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const pageInfo = PAGE_INFO[activeNav] ?? PAGE_INFO.Dashboard;
  const handleHoldingSaved = useCallback(() => {
    setShowAddHolding(false);
    setRefreshKey((k) => k + 1);
  }, []);

  return (
      <div className={`app-container ${isDark ? 'dark' : ''}`}>
        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebar-brand">
            <img
                src={isDark ? '/FullLogoBlack.png' : '/FullLogoWhite.png'}
                alt="HoldHive"
                className="sidebar-logo"
            />
          </div>

          <p className="sidebar-section-label">Portfolio workspace</p>

          <nav className="sidebar-nav">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const isActive = item.label === activeNav;
              return (
                  <button
                      key={item.label}
                      className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
                      onClick={() => setActiveNav(item.label)}
                  >
                    <Icon size={20} />
                    <span>{item.label}</span>
                  </button>
              );
            })}
          </nav>

        </aside>

        {/* Main Content */}
        <main className="main-content">
          <header className="content-header">
            <div>
              <h1 className="page-title">{pageInfo.title}</h1>
              <p className="page-subtitle">{pageInfo.subtitle}</p>
            </div>
            <div className="header-actions">
              <button className="theme-toggle" onClick={() => setIsDark(!isDark)}>
                {isDark ? <Sun size={16} /> : <Moon size={16} />}
                Day / Night
              </button>
              {activeNav !== 'Gateway' && !showAddHolding && (
                  <button className="add-button" onClick={() => setShowAddHolding(true)}>
                    <Plus size={16} />
                    Add
                  </button>
              )}
              {showAddHolding && (
                  <button className="add-button" onClick={() => setShowAddHolding(false)}>
                    Back
                  </button>
              )}
            </div>
          </header>

          {showAddHolding ? (
              <AddHoldingPage isDark={isDark} onSaved={handleHoldingSaved} />
          ) : activeNav === 'Gateway' ? (
              <GatewayPage onNavigate={setActiveNav} isDark={isDark} />
          ) : activeNav === 'Dashboard' ? (
              <DashboardPage />
          ) : activeNav === 'Holdings' ? (
              <HoldingsPage isDark={isDark} refreshTrigger={refreshKey} />
          ) : activeNav === 'Performance' ? (
              <PerformancePage />
          ) : activeNav === 'Analysis' ? (
              <AnalysisPage isDark={isDark} />
          ) : activeNav === 'Settings' ? (
              <SettingsPage isDark={isDark} onThemeChange={setIsDark} />
          ) : (
              <div className="placeholder-page">
                <Hexagon size={48} className="placeholder-icon" />
                <h2 className="placeholder-title">{pageInfo.title}</h2>
                <p className="placeholder-subtitle">{pageInfo.subtitle}</p>
                <p className="placeholder-hint">Coming soon</p>
              </div>
          )}
        </main>

        <Toaster position="top-right" />
      </div>
  );
}
