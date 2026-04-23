import type { AiConfig } from './types';

const DEFAULT_MODEL = 'gpt-4o-mini';

function parseBoolean(value: string | undefined, fallback: boolean): boolean {
  if (!value) {
    return fallback;
  }
  const normalized = value.trim().toLowerCase();
  if (['1', 'true', 'yes', 'on'].includes(normalized)) {
    return true;
  }
  if (['0', 'false', 'no', 'off'].includes(normalized)) {
    return false;
  }
  return fallback;
}

export function loadAiConfig(overrides?: Partial<AiConfig>): AiConfig {
  const envApiKey = process.env.OPENAI_API_KEY ?? '';
  const envModel = process.env.OPENAI_MODEL ?? DEFAULT_MODEL;
  const defaultRemote = envApiKey.trim().length > 0;

  const config: AiConfig = {
    openAiApiKey: overrides?.openAiApiKey ?? envApiKey,
    openAiModel: overrides?.openAiModel ?? envModel,
    remoteEnabled:
      overrides?.remoteEnabled ??
      parseBoolean(process.env.APP_AI_ENABLE_REMOTE, defaultRemote)
  };

  return {
    ...config,
    openAiApiKey: config.openAiApiKey.trim(),
    openAiModel: config.openAiModel.trim() || DEFAULT_MODEL
  };
}

