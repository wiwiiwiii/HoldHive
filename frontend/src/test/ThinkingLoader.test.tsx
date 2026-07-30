import { render, screen } from '@testing-library/react';

import { ThinkingLoader } from '../components/ThinkingLoader';

describe('ThinkingLoader', () => {
  it('renders an accessible loading status', () => {
    render(<ThinkingLoader label="Thinking through test data" detail="Checking the hive." />);

    expect(screen.getByRole('status')).toHaveTextContent(/thinking through test data/i);
    expect(screen.getByText(/checking the hive/i)).toBeInTheDocument();
  });
});
