import { render, screen } from '@testing-library/react';

import { App } from '../App';

describe('App', () => {
  it('renders the HoldHive starter dashboard', () => {
    render(<App />);

    expect(
      screen.getByRole('heading', { name: /portfolio dashboard skeleton/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/HoldHive starter workspace/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add holding/i })).toBeInTheDocument();
  });
});
