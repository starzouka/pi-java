import type { BrandingResult, ModerationResult } from '../config/types';
import type { AiProvider, BrandingInput, InvitationInput } from './aiProvider';

export class ResilientAiProvider implements AiProvider {
  constructor(
    private readonly localProvider: AiProvider,
    private readonly remoteProvider?: AiProvider
  ) {}

  async generateInvitation(input: InvitationInput): Promise<string> {
    if (!this.remoteProvider) {
      return this.localProvider.generateInvitation(input);
    }
    try {
      return await this.remoteProvider.generateInvitation(input);
    } catch {
      return this.localProvider.generateInvitation(input);
    }
  }

  async generateBranding(input: BrandingInput): Promise<BrandingResult> {
    if (!this.remoteProvider) {
      return this.localProvider.generateBranding(input);
    }
    try {
      return await this.remoteProvider.generateBranding(input);
    } catch {
      return this.localProvider.generateBranding(input);
    }
  }

  async moderateMessage(message: string): Promise<ModerationResult> {
    if (!this.remoteProvider) {
      return this.localProvider.moderateMessage(message);
    }
    try {
      return await this.remoteProvider.moderateMessage(message);
    } catch {
      return this.localProvider.moderateMessage(message);
    }
  }
}

