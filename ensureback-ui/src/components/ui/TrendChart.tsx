import { useReducedMotion } from 'framer-motion';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

interface TrendChartProps<T extends Record<string, unknown>> {
  data: T[];
  xKey: keyof T;
  yKey: keyof T;
  color?: string;
  yTickFormatter?: (value: number) => string;
  className?: string;
}

export const TrendChart = <T extends Record<string, unknown>>({
  data,
  xKey,
  yKey,
  color = '#7C3AED',
  yTickFormatter,
  className
}: TrendChartProps<T>) => {
  const prefersReducedMotion = useReducedMotion();

  if (!data.length) {
    return (
      <div className="flex h-48 items-center justify-center rounded-2xl border border-dashed border-muted bg-card text-sm text-muted-foreground">
        No data available
      </div>
    );
  }

  return (
    <div className={className}>
      <ResponsiveContainer width="100%" height={220}>
        <AreaChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={color} stopOpacity={0.25} />
              <stop offset="95%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(17, 24, 39, 0.08)" vertical={false} />
          <XAxis dataKey={xKey as string} tickLine={false} axisLine={false} tick={{ fontSize: 12, fill: '#6B7280' }} />
          <YAxis
            dataKey={yKey as string}
            tickLine={false}
            axisLine={false}
            width={48}
            tick={{ fontSize: 12, fill: '#6B7280' }}
            tickFormatter={(value) => (yTickFormatter ? yTickFormatter(Number(value)) : String(value))}
          />
          <Tooltip
            cursor={{ stroke: color, strokeWidth: 1, strokeDasharray: '4 4' }}
            contentStyle={{ borderRadius: '12px', border: '1px solid rgba(124, 58, 237, 0.15)' }}
            labelStyle={{ color: '#111827', fontWeight: 600 }}
          />
          <Area
            type="monotone"
            dataKey={yKey as string}
            stroke={color}
            strokeWidth={2.5}
            fill="url(#trendGradient)"
            isAnimationActive={!prefersReducedMotion}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
