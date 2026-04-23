import OpenAI from 'openai';
import type {
  BrandingResult,
  ModerationAction,
  ModerationCategory,
  ModerationResult
} from '../config/types';
import type { AiProvider, BrandingInput, InvitationInput } from './aiProvider';

const ALLOWED_CATEGORIES: ModerationCategory[] = ['spam', 'scam', 'harassment', 'aggression'];

function extractJsonObject(text: string): Record<string, unknown> {
  const trimmed = text.trim();
  try {
    return JSON.parse(trimmed) as Record<string, unknown>;
  } catch {
    const start = trimmed.indexOf('{');
    const end = trimmed.lastIndexOf('}');
    if (start >= 0 && end > start) {
      const slice = trimmed.slice(start, end + 1);
      return JSON.parse(slice) as Record<string, unknown>;
    }
    throw new Error('Invalid JSON payload from OpenAI');
  }
}

function sanitizeTwoSentences(maxText: string): string {
  const normalized = maxText.replace(/\s+/g, ' ').trim();
  const parts = normalized.split(/(?<=[.!?])\s+/).filter(Boolean);
  return parts.slice(0, 2).join(' ').trim();
}

function resolveAction(score: number): ModerationAction {
  if (score < 30) {
    return 'ok';
  }
  if (score <= 70) {
    return 'warn';
  }
  return 'block';
}

function sanitizeScore(value: unknown): number {
  const n = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(n)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(n)));
}

export class OpenAiProvider implements AiProvider {
  private readonly client: OpenAI;
  private readonly model: string;

  constructor(apiKey: string, model: string) {
    if (!apiKey.trim()) {
      throw new Error('OPENAI_API_KEY is missing');
    }
    this.client = new OpenAI({ apiKey });
    this.model = model;
  }

  async generateInvitation(input: InvitationInput): Promise<string> {
    const prompt = [
      'Tu es assistant de recrutement e-sport.',
      'Ecris en francais uniquement.',
      'Genere un message d invitation court (max 2 phrases).',
      'Ton amical mais competitif.',
      `Equipe: ${input.teamName}`,
      `Role recherche: ${input.targetRole ?? 'joueur'}`,
      `Contexte: ${input.context ?? 'tournoi a venir'}`
    ].join('\n');

    const response = await this.client.chat.completions.create({
      model: this.model,
      temperature: 0.7,
      messages: [{ role: 'user', content: prompt }]
    });

    const content = response.choices[0]?.message?.content?.trim() ?? '';
    if (!content) {
      throw new Error('OpenAI returned empty invitation');
    }
    return sanitizeTwoSentences(content);
  }

  async generateBranding(input: BrandingInput): Promise<BrandingResult> {
    const prompt = [
      'Tu es un expert branding e-sport.',
      'Reponds en JSON strict sans markdown.',
      'Format attendu: {"bio":"...","slogan":"..."}',
      'bio: 2 a 3 phrases en francais.',
      'slogan: court et accrocheur en francais.',
      `Nom equipe: ${input.teamName}`,
      `Identite: ${input.identity ?? 'competition et esprit d equipe'}`
    ].join('\n');

    const response = await this.client.chat.completions.create({
      model: this.model,
      temperature: 0.8,
      messages: [{ role: 'user', content: prompt }]
    });

    const content = response.choices[0]?.message?.content ?? '';
    const json = extractJsonObject(content);
    const bio = String(json.bio ?? '').trim();
    const slogan = String(json.slogan ?? '').trim();

    if (!bio || !slogan) {
      throw new Error('Invalid branding JSON');
    }
    return { bio, slogan };
  }

  async moderateMessage(message: string): Promise<ModerationResult> {
    const prompt = [
      'Analyse ce message de chat e-sport.',
      'Detecte seulement ces categories: spam, scam, harassment, aggression.',
      'Retourne du JSON strict sans markdown:',
      '{"score":0-100,"categories":["..."]}',
      'La langue de sortie est francaise mais les cles JSON restent en anglais.',
      `Message: ${message}`
    ].join('\n');

    const response = await this.client.chat.completions.create({
      model: this.model,
      temperature: 0,
      messages: [{ role: 'user', content: prompt }]
    });

    const content = response.choices[0]?.message?.content ?? '';
    const json = extractJsonObject(content);
    const score = sanitizeScore(json.score);

    const categoriesRaw = Array.isArray(json.categories) ? json.categories : [];
    const categories = categoriesRaw
      .map((entry) => String(entry).toLowerCase().trim())
      .filter((entry): entry is ModerationCategory =>
        ALLOWED_CATEGORIES.includes(entry as ModerationCategory)
      );

    return {
      score,
      categories,
      action: resolveAction(score)
    };
  }
}

