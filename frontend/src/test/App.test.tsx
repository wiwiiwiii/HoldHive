import { render, screen } from '@testing-library/react';

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
});

