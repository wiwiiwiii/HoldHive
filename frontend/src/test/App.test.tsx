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
});
