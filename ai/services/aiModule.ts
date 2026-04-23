import { loadAiConfig } from '../config/env';
import type { AiConfig, BrandingResult, ModerationResult } from '../config/types';
import { LocalAiProvider } from '../providers/localAiProvider';
import { OpenAiProvider } from '../providers/openAiProvider';
import { ResilientAiProvider } from '../providers/resilientAiProvider';
import { BrandingService } from './brandingService';
import { InvitationService } from './invitationService';
import { ModerationService } from './moderationService';

export interface InvitationInput {
  teamName: string;
  targetRole?: string;
  context?: string;
}

export interface BrandingInput {
  teamName: string;
  identity?: string;
}

export class AiModule {
  private readonly invitationService: InvitationService;
  private readonly brandingService: BrandingService;
  private readonly moderationService: ModerationService;
  private readonly config: AiConfig;

  constructor(configOverrides?: Partial<AiConfig>) {
    this.config = loadAiConfig(configOverrides);

    const localProvider = new LocalAiProvider();
    const remoteProvider =
      this.config.remoteEnabled && this.config.openAiApiKey
        ? new OpenAiProvider(this.config.openAiApiKey, this.config.openAiModel)
        : undefined;

    const provider = new ResilientAiProvider(localProvider, remoteProvider);
    this.invitationService = new InvitationService(provider);
    this.brandingService = new BrandingService(provider);
    this.moderationService = new ModerationService(provider);
  }

  getConfig(): AiConfig {
    return this.config;
  }

  async generateInvitation(input: InvitationInput): Promise<string> {
    return this.invitationService.generate(input);
  }

  async generateBranding(input: BrandingInput): Promise<BrandingResult> {
    return this.brandingService.generate(input);
  }

  async moderateMessage(message: string): Promise<ModerationResult> {
    return this.moderationService.analyze(message);
  }
}

