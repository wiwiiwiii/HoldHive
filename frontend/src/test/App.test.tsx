import { act, fireEvent, render, screen } from '@testing-library/react';

import { App } from '../App';

describe('App', () => {
  it('renders the HoldHive gateway page', () => {
    render(<App />);

    expect(
        screen.getByRole('heading', { name: /hive gateway/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/choose a workspace from the hive map/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /day \/ night/i })).toBeInTheDocument();
  });

  it('lets users resize the sidebar within bounds', () => {
    const { container } = render(<App />);
    const appContainer = container.firstElementChild as HTMLElement;
    const resizeHandle = screen.getByRole('button', { name: /resize sidebar/i });

    expect(resizeHandle).toHaveAttribute('aria-valuemin', '180');
    expect(resizeHandle).toHaveAttribute('aria-valuemax', '320');
    expect(resizeHandle).toHaveAttribute('aria-valuenow', '220');

    act(() => {
      fireEvent.pointerDown(resizeHandle, { clientX: 220 });
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 500 }));
      window.dispatchEvent(new MouseEvent('pointerup'));
    });

    expect(resizeHandle).toHaveAttribute('aria-valuenow', '320');
    expect(appContainer.style.getPropertyValue('--sidebar-width')).toBe('320px');

    act(() => {
      fireEvent.pointerDown(resizeHandle, { clientX: 320 });
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 120 }));
      window.dispatchEvent(new MouseEvent('pointerup'));
    });

    expect(resizeHandle).toHaveAttribute('aria-valuenow', '180');
    expect(appContainer.style.getPropertyValue('--sidebar-width')).toBe('180px');
  });

  it('renders the enlarged gateway hive map', () => {
    render(<App />);

    expect(screen.getByTestId('gateway-hive-map')).toHaveAttribute('viewBox', '0 0 1040 728');
    expect(screen.queryByText(/single portfolio context/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/^HoldHive$/i)).not.toBeInTheDocument();
    expect(screen.getAllByTestId(/gateway-node-portal-/)).toHaveLength(6);
    screen.getAllByTestId(/gateway-node-portal-/).forEach((nodePortal) => {
      expect(nodePortal).toHaveAttribute('data-radius', '60');
    });
    expect(screen.getAllByTestId(/gateway-node-label-/)).toHaveLength(6);
    screen.getAllByTestId(/gateway-node-label-/).forEach((label) => {
      expect(label).toHaveAttribute('data-font-size', '18');
    });
    screen.getAllByTestId(/gateway-node-sub-/).forEach((subLabel) => {
      expect(subLabel).toHaveAttribute('data-font-size', '14');
    });
  });

  it('uses a viewport-fit gateway layout to avoid vertical scrolling', () => {
    render(<App />);

    expect(screen.getByTestId('gateway-card')).toHaveClass('gateway-card-fit');
    expect(screen.getByTestId('gateway-hive-map')).toHaveClass('hive-map-svg-fit');
  });

  it('uses pressed glow without tooltip and clears pressed state on leave', () => {
    render(<App />);

    const holdingsPortal = screen.getByTestId('gateway-node-portal-holdings');
    const holdingsNode = holdingsPortal.closest('.hive-node');

    expect(holdingsNode).not.toBeNull();

    fireEvent.mouseEnter(holdingsNode!);

    expect(holdingsPortal).toHaveAttribute('data-state', 'hovered');
    expect(screen.getAllByText(/positions/i)).toHaveLength(1);

    fireEvent.mouseDown(holdingsNode!);

    expect(holdingsPortal).toHaveAttribute('data-state', 'pressed');
    const pressedGlow = screen.getByTestId('gateway-node-glow-holdings');
    expect(pressedGlow).toBeInTheDocument();
    expect(pressedGlow).toHaveAttribute('data-glow-style', 'diffused-gradient-halo');
    expect(screen.getAllByTestId(/gateway-node-glow-holdings-layer-/)).toHaveLength(4);
    expect(screen.getByTestId('gateway-node-glow-holdings-layer-aura')).toHaveAttribute('fill', 'url(#gatewayPressedHaloAura)');
    expect(screen.getByTestId('gateway-node-glow-holdings-layer-bloom')).toHaveAttribute('fill', 'url(#gatewayPressedHaloBloom)');
    expect(screen.getByTestId('gateway-node-glow-holdings-layer-edge')).toHaveAttribute('fill', 'url(#gatewayPressedHaloEdge)');
    expect(screen.getByTestId('gateway-node-glow-holdings-layer-edge')).not.toHaveAttribute('stroke', '#f6b33b');
    expect(holdingsPortal).toHaveAttribute('fill', '#4F86F7');
    expect(holdingsPortal).toHaveAttribute('fill-opacity', '0.15');

    fireEvent.mouseLeave(holdingsNode!);

    expect(holdingsPortal).toHaveAttribute('data-state', 'idle');
    expect(screen.queryByTestId('gateway-node-glow-holdings')).not.toBeInTheDocument();
  });
});
