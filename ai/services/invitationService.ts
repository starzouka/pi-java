import type { AiProvider } from '../providers/aiProvider';

export interface InvitationRequest {
  teamName: string;
  targetRole?: string;
  context?: string;
}

export class InvitationService {
  constructor(private readonly provider: AiProvider) {}

  async generate(request: InvitationRequest): Promise<string> {
    return this.provider.generateInvitation({
      teamName: request.teamName,
      targetRole: request.targetRole,
      context: request.context
    });
  }
}

