import type { BrandingResult } from '../config/types';
import type { AiProvider } from '../providers/aiProvider';

export interface BrandingRequest {
  teamName: string;
  identity?: string;
}

export class BrandingService {
  constructor(private readonly provider: AiProvider) {}

  async generate(request: BrandingRequest): Promise<BrandingResult> {
    return this.provider.generateBranding({
      teamName: request.teamName,
      identity: request.identity
    });
  }
}

