import { useState, useCallback, type CSSProperties, type PointerEvent as ReactPointerEvent } from 'react';
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
} from 'lucide-react';
import { Toaster } from 'react-hot-toast';
import { DashboardPage } from './components/DashboardPage';
import { GatewayPage } from './components/GatewayPage';
import { HoldingsPage } from './components/HoldingsPage';
import { PerformancePage } from './components/PerformancePage';
import { AnalysisPage } from './components/AnalysisPage';
import { SettingsPage } from './components/SettingsPage';
import { AddHoldingPage } from './components/AddHoldingPage';
import type { PriceMode } from './api/types';


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

const SIDEBAR_MIN_WIDTH = 180;
const SIDEBAR_MAX_WIDTH = 320;
const SIDEBAR_DEFAULT_WIDTH = 220;

function clampSidebarWidth(width: number): number {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, width));
}

export function App() {
  const [activeNav, setActiveNav] = useState('Gateway');
  const [isDark, setIsDark] = useState(false);
  const [showAddHolding, setShowAddHolding] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [priceMode, setPriceMode] = useState<PriceMode>('BEST_AVAILABLE');
  const [sidebarWidth, setSidebarWidth] = useState(SIDEBAR_DEFAULT_WIDTH);
  const [isSidebarResizing, setIsSidebarResizing] = useState(false);

  const pageInfo = PAGE_INFO[activeNav] ?? PAGE_INFO.Dashboard;

  const handleHoldingsChanged = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  const handleHoldingSaved = useCallback(() => {
    setShowAddHolding(false);
    handleHoldingsChanged();
  }, [handleHoldingsChanged]);

  const handleNavClick = useCallback((label: string) => {
    setActiveNav(label);
    setShowAddHolding(false);
  }, []);

  const handleAddClick = useCallback(() => {
    setShowAddHolding(true);
  }, []);

  const handleBackClick = useCallback(() => {
    setShowAddHolding(false);
  }, []);

  const handleSidebarResizeStart = useCallback((event: ReactPointerEvent<HTMLButtonElement>) => {
    event.preventDefault();
    setIsSidebarResizing(true);

    const handlePointerMove = (moveEvent: PointerEvent) => {
      setSidebarWidth(clampSidebarWidth(moveEvent.clientX));
    };

    const handlePointerUp = () => {
      setIsSidebarResizing(false);
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  }, []);

  const renderPage = () => {
    if (activeNav === 'Gateway') {
      return <GatewayPage onNavigate={handleNavClick} onAddHolding={handleAddClick} isDark={isDark} />;
    }
    if (activeNav === 'Dashboard') return <DashboardPage priceMode={priceMode} />;
    if (activeNav === 'Holdings') {
      return (
          <HoldingsPage
              isDark={isDark}
              refreshTrigger={refreshKey}
              priceMode={priceMode}
              onHoldingsChanged={handleHoldingsChanged}
          />
      );
    }
    if (activeNav === 'Performance') return <PerformancePage priceMode={priceMode} />;
    if (activeNav === 'Analysis') return <AnalysisPage isDark={isDark} refreshTrigger={refreshKey} priceMode={priceMode} />;
    if (activeNav === 'Settings') {
      return (
          <SettingsPage
              isDark={isDark}
              onThemeChange={setIsDark}
              dataMode={priceMode}
              onDataModeChange={setPriceMode}
          />
      );
    }
    return (
        <div className="placeholder-page">
          <Hexagon size={48} className="placeholder-icon" />
          <h2 className="placeholder-title">{pageInfo.title}</h2>
          <p className="placeholder-subtitle">{pageInfo.subtitle}</p>
          <p className="placeholder-hint">Coming soon</p>
        </div>
    );
  };

  return (
      <div
          className={`app-container ${isDark ? 'dark' : ''} ${isSidebarResizing ? 'sidebar-resizing' : ''}`}
          style={{ '--sidebar-width': `${sidebarWidth}px` } as CSSProperties}
      >
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
              const isActive = item.label === activeNav && !showAddHolding;
              return (
                  <button
                      key={item.label}
                      className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
                      onClick={() => handleNavClick(item.label)}
                  >
                    <Icon size={20} />
                    <span>{item.label}</span>
                  </button>
              );
            })}
          </nav>

          <button
              type="button"
              className="sidebar-resize-handle"
              aria-label="Resize sidebar"
              aria-valuemin={SIDEBAR_MIN_WIDTH}
              aria-valuemax={SIDEBAR_MAX_WIDTH}
              aria-valuenow={sidebarWidth}
              onPointerDown={handleSidebarResizeStart}
          />

        </aside>

        {/* Main Content */}
        <main className="main-content">
          <header className="content-header">
            <div>
              <h1 className="page-title">{showAddHolding ? 'Add Holding' : pageInfo.title}</h1>
              <p className="page-subtitle">{showAddHolding ? 'Search a ticker and create a new position' : pageInfo.subtitle}</p>
            </div>
            <div className="header-actions">
              <button className="theme-toggle" onClick={() => setIsDark(!isDark)}>
                {isDark ? <Sun size={16} /> : <Moon size={16} />}
                Day / Night
              </button>
              {activeNav !== 'Gateway' && !showAddHolding && (
                  <button className="add-button" onClick={handleAddClick}>
                    <Plus size={16} />
                    Add
                  </button>
              )}
              {showAddHolding && (
                  <button className="add-button" onClick={handleBackClick}>
                    Back
                  </button>
              )}
            </div>
          </header>

          {showAddHolding ? (
              <AddHoldingPage isDark={isDark} onSaved={handleHoldingSaved} />
          ) : (
              renderPage()
          )}
        </main>

        <Toaster position="top-right" />
      </div>
  );
}
