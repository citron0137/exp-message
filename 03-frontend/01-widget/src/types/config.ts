export interface WidgetConfig {
  apiUrl?: string;
  wsUrl?: string;
  publicKey?: string;
  visitor?: {
    externalId?: string;
    displayName?: string;
    email?: string;
    metadata?: Record<string, string>;
  };
  theme?: 'light' | 'dark';
}
