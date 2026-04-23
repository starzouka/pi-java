import type { BrandingResult, ModerationResult } from '../config/types';

export interface InvitationInput {
  teamName: string;
  targetRole?: string;
  context?: string;
}

export interface BrandingInput {
  teamName: string;
  identity?: string;
}

export interface AiProvider {
  generateInvitation(input: InvitationInput): Promise<string>;
  generateBranding(input: BrandingInput): Promise<BrandingResult>;
  moderateMessage(message: string): Promise<ModerationResult>;
}

