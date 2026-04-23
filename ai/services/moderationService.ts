import type { ModerationResult } from '../config/types';
import type { AiProvider } from '../providers/aiProvider';

export class ModerationService {
  constructor(private readonly provider: AiProvider) {}

  async analyze(message: string): Promise<ModerationResult> {
    return this.provider.moderateMessage(message);
  }
}

