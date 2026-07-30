import { useState } from 'react';

const GATEWAY_NODES = [
    { label: 'Settings', sub: 'Theme', color: '#8a94a6', angle: -90 },
    { label: 'Dashboard', sub: 'Open portfolio overview', color: '#f6b33b', angle: -30 },
    { label: 'Holdings', sub: 'Positions', color: '#4F86F7', angle: 30 },
    { label: 'Performance', sub: 'Returns', color: '#27ae60', angle: 90 },
    { label: 'Analysis', sub: 'Risk view', color: '#9B59B6', angle: 150 },
    { label: 'Add Holding', sub: 'New record', color: '#e8a020', angle: 210 },
];

const GATEWAY_VIEWBOX_WIDTH = 1040;
const GATEWAY_VIEWBOX_HEIGHT = 728;
const GATEWAY_CENTER_X = 520;
const GATEWAY_CENTER_Y = 364;
const GATEWAY_OUTER_RADIUS = 260;
const GATEWAY_NODE_RADIUS = 36;
const GATEWAY_CENTER_RADIUS = 91;
const GATEWAY_TEXT_OFFSET = 55;

function getHexPoint(cx: number, cy: number, radius: number, angleDeg: number) {
    const rad = (angleDeg * Math.PI) / 180;
    return { x: cx + radius * Math.cos(rad), y: cy + radius * Math.sin(rad) };
}

function hexagonPoints(cx: number, cy: number, r: number): string {
    const pts = [];
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const pt = getHexPoint(cx, cy, r, angle);
        pts.push(`${pt.x},${pt.y}`);
    }
    return pts.join(' ');
}

interface GatewayPageProps {
    onNavigate: (page: string) => void;
    onAddHolding?: () => void;
    isDark?: boolean;
}

export function GatewayPage({ onNavigate, onAddHolding, isDark }: GatewayPageProps) {
    const [hoveredNode, setHoveredNode] = useState<string | null>(null);
    const [pressedNode, setPressedNode] = useState<string | null>(null);

    const cx = GATEWAY_CENTER_X;
    const cy = GATEWAY_CENTER_Y;
    const outerRadius = GATEWAY_OUTER_RADIUS;
    const nodeRadius = GATEWAY_NODE_RADIUS;

    const nodes = GATEWAY_NODES.map((node) => {
        const pt = getHexPoint(cx, cy, outerRadius, node.angle);
        return { ...node, x: pt.x, y: pt.y };
    });

    const hexPath = hexagonPoints(cx, cy, outerRadius);

    return (
        <div className="gateway-card">
            <div className="gateway-header-row">
                <div className="gateway-brand-area">
                    <img src={isDark ? '/LogoBlack.png' : '/LogoWhite.png'} alt="HoldHive"
                         className="gateway-logo"/>
                    <div>
                        <h2 className="gateway-main-title">Pick a workspace</h2>
                        <p className="gateway-main-subtitle">
                            Every vertex opens a focused part of the same portfolio.
                        </p>
                    </div>
                </div>
            </div>

            <div className="hive-map-container">
                <svg
                    viewBox={`0 0 ${GATEWAY_VIEWBOX_WIDTH} ${GATEWAY_VIEWBOX_HEIGHT}`}
                    className="hive-map-svg"
                    role="img"
                    aria-label="Hive gateway workspace map"
                    data-testid="gateway-hive-map"
                >
                    <polygon
                        points={hexPath}
                        fill="none"
                        stroke="#f6b33b"
                        strokeWidth="2.5"
                        opacity="0.5"
                    />

                    <polygon
                        points={hexagonPoints(cx, cy, outerRadius - 13)}
                        fill={isDark ? '#16161c' : '#fff9ed'}
                        opacity="0.5"
                    />

                    <circle cx={cx} cy={cy} r={GATEWAY_CENTER_RADIUS} fill={isDark ? '#111827' : '#ffffff'} stroke="#f0f1f5" strokeWidth="1.3" />
                    <image
                        href={isDark ? '/LogoBlack.png' : '/LogoWhite.png'}
                        x={cx - 104}
                        y={cy - 78}
                        width="208"
                        height="130"
                        preserveAspectRatio="xMidYMid meet"
                    />
                    <text x={cx} y={cy + 68} textAnchor="middle" className="hive-center-label">
                        HoldHive
                    </text>
                    <text x={cx} y={cy + 88} textAnchor="middle" className="hive-center-sub">
                        single portfolio context
                    </text>

                    {nodes.map((node) => {
                        const isHovered = hoveredNode === node.label;
                        const isPressed = pressedNode === node.label;
                        const fillOpacity = isHovered && !isPressed ? 1 : 0.15;
                        const scale = isHovered ? 1.15 : 1;

                        return (
                            <g
                                key={node.label}
                                className="hive-node"
                                onMouseEnter={() => setHoveredNode(node.label)}
                                onMouseLeave={() => setHoveredNode(null)}
                                onMouseDown={() => setPressedNode(node.label)}
                                onMouseUp={() => setPressedNode(null)}
                                onClick={() => {
                                    if (node.label === 'Add Holding') {
                                        onAddHolding?.();
                                    } else {
                                        onNavigate(node.label);
                                    }
                                }}
                                style={{ cursor: 'pointer' }}
                            >
                                {isHovered && !isPressed && (
                                    <polygon
                                        points={hexagonPoints(node.x, node.y, nodeRadius + 16)}
                                        fill={node.color}
                                        opacity="0.15"
                                    />
                                )}
                                <polygon
                                    points={hexagonPoints(node.x, node.y, nodeRadius * scale)}
                                    fill={node.color}
                                    fillOpacity={fillOpacity}
                                    stroke={node.color}
                                    strokeWidth={isHovered && !isPressed ? 2.5 : 1.5}
                                    style={{ transition: 'all 0.16s ease' }}
                                />
                                <circle
                                    cx={node.x}
                                    cy={node.y}
                                    r={5}
                                    fill={isHovered && !isPressed ? '#ffffff' : node.color}
                                    style={{ transition: 'all 0.16s ease' }}
                                />

                                <text
                                    x={node.x + (node.x > cx ? GATEWAY_TEXT_OFFSET : -GATEWAY_TEXT_OFFSET)}
                                    y={node.y - 8}
                                    textAnchor={node.x > cx ? 'start' : 'end'}
                                    className="hive-node-label"
                                >
                                    {node.label}
                                </text>
                                <text
                                    x={node.x + (node.x > cx ? GATEWAY_TEXT_OFFSET : -GATEWAY_TEXT_OFFSET)}
                                    y={node.y + 16}
                                    textAnchor={node.x > cx ? 'start' : 'end'}
                                    className="hive-node-sub"
                                >
                                    {node.sub}
                                </text>

                                {!isPressed && isHovered && (
                                    <g>
                                        <rect
                                            x={node.x + 52}
                                            y={node.y - 65}
                                            width="234"
                                            height="73"
                                            rx="16"
                                            fill="#ffffff"
                                            stroke="#eef0f5"
                                            strokeWidth="1"
                                            filter="url(#shadow)"
                                        />
                                        <text x={node.x + 68} y={node.y - 33} className="tooltip-title">
                                            {node.label}
                                        </text>
                                        <text x={node.x + 68} y={node.y - 12} className="tooltip-sub">
                                            {node.sub}
                                        </text>
                                    </g>
                                )}
                            </g>
                        );
                    })}

                    <defs>
                        <filter id="shadow" x="-10%" y="-10%" width="130%" height="130%">
                            <feDropShadow dx="0" dy="4" stdDeviation="8" floodOpacity="0.08" />
                        </filter>
                    </defs>
                </svg>
            </div>
        </div>
    );
}
