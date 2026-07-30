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
const GATEWAY_NODE_RADIUS = 60;
const GATEWAY_CENTER_RADIUS = 91;
const GATEWAY_TEXT_OFFSET = 102;

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

function hexagonPath(cx: number, cy: number, r: number): string {
    const pts = [];
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const pt = getHexPoint(cx, cy, r, angle);
        pts.push(`${pt.x} ${pt.y}`);
    }
    return `M ${pts.join(' L ')} Z`;
}

function annularHexPath(cx: number, cy: number, outerR: number, innerR: number): string {
    return `${hexagonPath(cx, cy, outerR)} ${hexagonPath(cx, cy, innerR)}`;
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
        <div className="gateway-card gateway-card-fit" data-testid="gateway-card">
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
                    className="hive-map-svg hive-map-svg-fit"
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
                        y={cy - 65}
                        width="208"
                        height="130"
                        preserveAspectRatio="xMidYMid meet"
                    />
                    {nodes.map((node) => {
                        const isHovered = hoveredNode === node.label;
                        const isPressed = pressedNode === node.label;
                        const fillOpacity = isHovered && !isPressed ? 1 : 0.15;
                        const scale = isHovered ? 1.15 : 1;
                        const nodeKey = node.label.toLowerCase().replace(/\s+/g, '-');
                        const nodeState = isPressed ? 'pressed' : isHovered ? 'hovered' : 'idle';

                        return (
                            <g
                                key={node.label}
                                className="hive-node"
                                onMouseEnter={() => setHoveredNode(node.label)}
                                onMouseLeave={() => {
                                    setHoveredNode(null);
                                    setPressedNode(null);
                                }}
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
                                {isPressed && (
                                    <g
                                        data-testid={`gateway-node-glow-${nodeKey}`}
                                        data-glow-style="diffused-gradient-halo"
                                        pointerEvents="none"
                                    >
                                        <path
                                            data-testid={`gateway-node-glow-${nodeKey}-layer-aura`}
                                            data-glow-layer="aura"
                                            d={annularHexPath(node.x, node.y, nodeRadius + 78, nodeRadius + 30)}
                                            fill="url(#gatewayPressedHaloAura)"
                                            fillRule="evenodd"
                                            opacity="0.78"
                                            filter="url(#gatewayPressedHaloSoftBlur)"
                                        />
                                        <path
                                            data-testid={`gateway-node-glow-${nodeKey}-layer-bloom`}
                                            data-glow-layer="bloom"
                                            d={annularHexPath(node.x, node.y, nodeRadius + 60, nodeRadius + 18)}
                                            fill="url(#gatewayPressedHaloBloom)"
                                            fillRule="evenodd"
                                            opacity="0.72"
                                            filter="url(#gatewayPressedHaloBloomBlur)"
                                        />
                                        <path
                                            data-testid={`gateway-node-glow-${nodeKey}-layer-edge`}
                                            data-glow-layer="edge"
                                            d={annularHexPath(node.x, node.y, nodeRadius + 25, nodeRadius + 4)}
                                            fill="url(#gatewayPressedHaloEdge)"
                                            fillRule="evenodd"
                                            opacity="0.9"
                                            filter="url(#gatewayPressedHaloEdgeBlur)"
                                        />
                                        <path
                                            data-testid={`gateway-node-glow-${nodeKey}-layer-spark`}
                                            data-glow-layer="spark"
                                            d={annularHexPath(node.x, node.y, nodeRadius + 12, nodeRadius + 7)}
                                            fill="url(#gatewayPressedHaloSpark)"
                                            fillRule="evenodd"
                                            opacity="0.5"
                                        />
                                    </g>
                                )}
                                {isHovered && !isPressed && (
                                    <polygon
                                        data-testid={`gateway-node-hover-${nodeKey}`}
                                        points={hexagonPoints(node.x, node.y, nodeRadius + 20)}
                                        fill={node.color}
                                        opacity="0.15"
                                    />
                                )}
                                <polygon
                                    data-testid={`gateway-node-portal-${nodeKey}`}
                                    data-radius={nodeRadius}
                                    data-state={nodeState}
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
                                    r={8}
                                    fill={isHovered && !isPressed ? '#ffffff' : node.color}
                                    style={{ transition: 'all 0.16s ease' }}
                                />

                                <text
                                    data-testid={`gateway-node-label-${nodeKey}`}
                                    data-font-size="18"
                                    x={node.x + (node.x > cx ? GATEWAY_TEXT_OFFSET : -GATEWAY_TEXT_OFFSET)}
                                    y={node.y - 8}
                                    textAnchor={node.x > cx ? 'start' : 'end'}
                                    className="hive-node-label"
                                >
                                    {node.label}
                                </text>
                                <text
                                    data-testid={`gateway-node-sub-${nodeKey}`}
                                    data-font-size="14"
                                    x={node.x + (node.x > cx ? GATEWAY_TEXT_OFFSET : -GATEWAY_TEXT_OFFSET)}
                                    y={node.y + 16}
                                    textAnchor={node.x > cx ? 'start' : 'end'}
                                    className="hive-node-sub"
                                >
                                    {node.sub}
                                </text>
                            </g>
                        );
                    })}

                    <defs>
                        <radialGradient id="gatewayPressedHaloAura" cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stopColor="#f6b33b" stopOpacity="0" />
                            <stop offset="48%" stopColor="#f6b33b" stopOpacity="0.06" />
                            <stop offset="66%" stopColor="#ffd66b" stopOpacity="0.34" />
                            <stop offset="84%" stopColor="#f6b33b" stopOpacity="0.16" />
                            <stop offset="100%" stopColor="#f6b33b" stopOpacity="0" />
                        </radialGradient>
                        <radialGradient id="gatewayPressedHaloBloom" cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stopColor="#fff4d6" stopOpacity="0" />
                            <stop offset="42%" stopColor="#fff4d6" stopOpacity="0.1" />
                            <stop offset="62%" stopColor="#fff1b8" stopOpacity="0.5" />
                            <stop offset="78%" stopColor="#f6b33b" stopOpacity="0.28" />
                            <stop offset="100%" stopColor="#f6b33b" stopOpacity="0" />
                        </radialGradient>
                        <radialGradient id="gatewayPressedHaloEdge" cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stopColor="#fff4d6" stopOpacity="0" />
                            <stop offset="52%" stopColor="#fff4d6" stopOpacity="0.18" />
                            <stop offset="68%" stopColor="#ffe09a" stopOpacity="0.86" />
                            <stop offset="86%" stopColor="#f6b33b" stopOpacity="0.38" />
                            <stop offset="100%" stopColor="#f6b33b" stopOpacity="0" />
                        </radialGradient>
                        <radialGradient id="gatewayPressedHaloSpark" cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stopColor="#fff8db" stopOpacity="0" />
                            <stop offset="58%" stopColor="#fff8db" stopOpacity="0.4" />
                            <stop offset="78%" stopColor="#ffd66b" stopOpacity="0.68" />
                            <stop offset="100%" stopColor="#f6b33b" stopOpacity="0" />
                        </radialGradient>
                        <filter id="gatewayPressedHaloSoftBlur" x="-70%" y="-70%" width="240%" height="240%">
                            <feGaussianBlur stdDeviation="11" />
                        </filter>
                        <filter id="gatewayPressedHaloBloomBlur" x="-55%" y="-55%" width="210%" height="210%">
                            <feGaussianBlur stdDeviation="6.5" />
                        </filter>
                        <filter id="gatewayPressedHaloEdgeBlur" x="-45%" y="-45%" width="190%" height="190%">
                            <feGaussianBlur stdDeviation="2.2" />
                        </filter>
                    </defs>
                </svg>
            </div>
        </div>
    );
}
