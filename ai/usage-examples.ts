import 'dotenv/config';
import { AiModule } from './index';

async function runExamples(): Promise<void> {
  const ai = new AiModule();

  const invitation = await ai.generateInvitation({
    teamName: 'Pulse Titans',
    targetRole: 'entry fragger',
    context: 'qualifier regional ce week-end'
  });
  console.log('Invitation:\n', invitation, '\n');

  const branding = await ai.generateBranding({
    teamName: 'Pulse Titans',
    identity: 'discipline tactique et mental solide'
  });
  console.log('Branding:\n', branding, '\n');

  const moderation = await ai.moderateMessage(
    'Clique sur ce lien gratuit discord.gg/xxx et envoie ton code OTP maintenant'
  );
  console.log('Moderation:\n', moderation);
}

runExamples().catch((error) => {
  console.error('AI module error:', error);
  process.exitCode = 1;
});

