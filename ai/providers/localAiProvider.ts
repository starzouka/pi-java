import type {
  BrandingResult,
  ModerationAction,
  ModerationCategory,
  ModerationResult
} from '../config/types';
import type { AiProvider, BrandingInput, InvitationInput } from './aiProvider';

const INVITATION_TEMPLATES = [
  "Salut ! L'equipe {team} cherche un {role} motive pour viser la victoire sur les prochains tournois.",
  "On veut renforcer {team} avec un {role} solide et regulier pour monter au classement.",
  "{team} recrute un {role} pret a performer sous pression et a jouer collectif."
];

const BRANDING_ENDINGS = [
  'discipline, ambition et esprit d equipe',
  'performance, regularite et mental d acier',
  'jeu intelligent, confiance et execution propre'
];

function normalize(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function countMatches(text: string, terms: string[]): number {
  const normalized = normalize(text);
  return terms.reduce((count, term) => {
    const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`\\b${escaped}\\b`, 'g');
    const matches = normalized.match(regex);
    return count + (matches ? matches.length : 0);
  }, 0);
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

function clampScore(score: number): number {
  return Math.max(0, Math.min(100, score));
}

export class LocalAiProvider implements AiProvider {
  async generateInvitation(input: InvitationInput): Promise<string> {
    const teamName = input.teamName.trim() || 'notre equipe';
    const role = (input.targetRole?.trim() || 'joueur').toLowerCase();
    const context = input.context?.trim();

    const index = Math.abs((teamName + role).length) % INVITATION_TEMPLATES.length;
    const firstSentence = INVITATION_TEMPLATES[index]
      .replace('{team}', teamName)
      .replace('{role}', role);

    const secondSentence = context
      ? `Ton profil correspond bien a notre objectif: ${context}.`
      : 'Si tu veux gagner serieusement, on peut te faire une place des maintenant.';

    return `${firstSentence} ${secondSentence}`;
  }

  async generateBranding(input: BrandingInput): Promise<BrandingResult> {
    const teamName = input.teamName.trim() || 'Equipe';
    const identity = input.identity?.trim() || 'esport competitif';
    const ending = BRANDING_ENDINGS[Math.abs(teamName.length + identity.length) % BRANDING_ENDINGS.length];

    const bio = `${teamName} est une equipe orientee ${identity}. Nous jouons chaque match avec ${ending}. Notre objectif est d imposer un style propre et de gagner durablement.`;
    const slogan = `${teamName} - Plus vite, plus juste, plus fort.`;

    return { bio, slogan };
  }

  async moderateMessage(message: string): Promise<ModerationResult> {
    const raw = message ?? '';
    const categories: ModerationCategory[] = [];
    let score = 0;

    const spamTerms = ['gratuit', 'promo', 'abonne', 'discord.gg', 'clique', 'http', 'www', '.com'];
    const scamTerms = ['bitcoin', 'crypto', 'iban', 'virement', 'carte bancaire', 'mot de passe', 'code otp'];
    const harassmentTerms = ['idiot', 'imbecile', 'nul', 'degage', 'ferme-la', 'sale', 'haine'];
    const aggressionTerms = ['je vais te', 'tuer', 'frapper', 'casser', 'detruire', 'menace'];

    const spamHits = countMatches(raw, spamTerms);
    const scamHits = countMatches(raw, scamTerms);
    const harassmentHits = countMatches(raw, harassmentTerms);
    const aggressionHits = countMatches(raw, aggressionTerms);

    if (spamHits > 0) {
      categories.push('spam');
      score += Math.min(45, 15 + spamHits * 10);
    }
    if (scamHits > 0) {
      categories.push('scam');
      score += Math.min(70, 30 + scamHits * 20);
    }
    if (harassmentHits > 0) {
      categories.push('harassment');
      score += Math.min(60, 20 + harassmentHits * 15);
    }
    if (aggressionHits > 0) {
      categories.push('aggression');
      score += Math.min(80, 30 + aggressionHits * 20);
    }

    if (/([!?])\1{4,}/.test(raw) || /(.)\1{8,}/.test(raw.toLowerCase())) {
      if (!categories.includes('spam')) {
        categories.push('spam');
      }
      score += 12;
    }

    score = clampScore(score);
    return {
      score,
      categories,
      action: resolveAction(score)
    };
  }
}

