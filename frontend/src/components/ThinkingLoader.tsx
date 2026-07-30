interface ThinkingLoaderProps {
    label?: string;
    detail?: string;
    compact?: boolean;
}

const CELLS = Array.from({ length: 6 }, (_, index) => index);

export function ThinkingLoader({
    label = 'Thinking through portfolio data',
    detail,
    compact = false,
}: ThinkingLoaderProps) {
    return (
        <div className={`thinking-loader ${compact ? 'thinking-loader-compact' : ''}`} role="status" aria-live="polite">
            <div className="thinking-hive" aria-hidden="true">
                {CELLS.map((cell) => (
                    <span className="thinking-cell" key={cell} />
                ))}
            </div>
            <p className="thinking-loader-text">
                {label}
                <span className="thinking-dots">
                    <span>.</span>
                    <span>.</span>
                    <span>.</span>
                </span>
            </p>
            {detail && <p className="thinking-loader-detail">{detail}</p>}
        </div>
    );
}
