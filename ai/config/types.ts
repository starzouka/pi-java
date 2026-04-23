export type ModerationAction = 'ok' | 'warn' | 'block';

export type ModerationCategory = 'spam' | 'scam' | 'harassment' | 'aggression';

export interface ModerationResult {
  score: number;
  categories: ModerationCategory[];
  action: ModerationAction;
}

export interface BrandingResult {
  bio: string;
  slogan: string;
}

export interface AiConfig {
  openAiApiKey: string;
  openAiModel: string;
  remoteEnabled: boolean;
}

