# PULSE Desktop (JavaFX)

Application desktop JavaFX alignee sur la structure de navigation Symfony et connectee a la meme base MySQL `pulsedb`.

## Structure Symfony reproduite

- Topbar: `Accueil`, `Tournois`, `Jeux`, `Matchs`, `Boutique`, `Equipe`
- Sidebar sections:
  - Navigation principale
  - Competition
  - Boutique
  - Joueur (selon role)
  - Capitaine (selon role)
  - Organisateur (selon role)
  - Administration (selon role)
  - Connexion (Guest)
  - Support
- Pages speciales implementees:
  - `front_home` (accueil)
  - `front_login` (connexion)
  - `front_register` (inscription)
  - `front_dashboard` (dashboard)
  - `front_forgot_password` (mot de passe oublie)
  - `front_reset_password` (reinitialisation par token)
  - `front_two_factor_challenge` (challenge 2FA)
  - `front_profile` (profil joueur avec photo + publications + onglets)
  - `front_profile_edit` (edition profil + upload photo + gestion 2FA)
- Les autres routes front Symfony sont disponibles via des vues dediees:
  - `src/main/resources/fxml/pages/routes/front_<route>-view.fxml`
  - actuellement 58 vues dediees generees.

## Contrainte UI respectee

- Aucun `TableView` dans les FXML.
- Interfaces Scene Builder basees sur `VBox`, `HBox`, `FlowPane`, `ScrollPane`, `GridPane`.

## Tech stack

- Java 21
- JavaFX (`javafx-controls`, `javafx-fxml`)
- JDBC (MySQL Connector/J)
- BCrypt pour login/register
- Maven

## Configuration DB par defaut

Fichier: `src/main/resources/application.properties`

```properties
db.host=127.0.0.1
db.port=3306
db.name=pulsedb
db.user=root
db.password=
```

Variables d'environnement supportees:

- `PULSE_DB_HOST`
- `PULSE_DB_PORT`
- `PULSE_DB_NAME`
- `PULSE_DB_USER`
- `PULSE_DB_PASSWORD`

## Configuration email (meme logique Symfony)

L'application JavaFX lit automatiquement les variables Symfony depuis:

- `../.env`
- `../.env.local`

Variables utilisees:

- `MAILER_DSN` (SMTP)
- `MAILER_FROM_ADDRESS`
- `APP_SECRET` (signature du lien de verification email compatible Symfony)

Optionnel (override desktop):

- `PULSE_MAILER_DSN`
- `PULSE_MAILER_FROM_ADDRESS`
- `PULSE_APP_SECRET`
- `PULSE_WEB_BASE_URL` (par defaut `http://127.0.0.1:8000`)

## Assistant boutique OpenRouter

L'assistant IA est limite a la boutique et s'ouvre depuis `front_shop`.

Variables utilisees:

- `PULSE_OPENROUTER_API_KEY`
- `PULSE_OPENROUTER_BASE_URL` (par defaut `https://openrouter.ai/api/v1/chat/completions`)
- `PULSE_OPENROUTER_MODEL` (par defaut `openai/gpt-4o-mini`)
- `PULSE_OPENROUTER_APP_NAME`
- `PULSE_OPENROUTER_SITE_URL`

Configuration desktop par defaut:

```properties
openrouter.api.key=
openrouter.base-url=https://openrouter.ai/api/v1/chat/completions
openrouter.model=openai/gpt-4o-mini
openrouter.app-name=PULSE Desktop
openrouter.site-url=http://127.0.0.1:8000
```

## Lancer le projet

```bash
mvn clean javafx:run
```

## Compiler

```bash
mvn clean package
```

## Synchroniser les vues avec les templates Twig Symfony

Script de generation:

```bash
python scripts/sync_twig_views.py
```

Le script lit `templates/front/**/*.twig` puis regenere les vues de `fxml/pages/routes` avec:

- sections/entetes detectees
- formulaires (champs + boutons)
- actions/lien route `front_*`

## Ouvrir avec Scene Builder

FXML:

- `src/main/resources/fxml/app-shell.fxml`
- `src/main/resources/fxml/pages/*.fxml`

IntelliJ:

1. `File -> Settings -> Languages & Frameworks -> JavaFX`
2. Renseigner le chemin de `SceneBuilder.exe`
3. Clic droit sur un `.fxml` -> `Open in Scene Builder`

## Tester le module user (IntelliJ)

1. Demarrer MySQL avec la base `pulsedb`.
2. Demarrer Symfony web sur `http://127.0.0.1:8000`:
   - `symfony server:start` (ou votre commande habituelle).
3. Dans IntelliJ (module `pulse-desktop-javafx`):
   - Maven tool window -> `javafx` -> `javafx:run`.
4. Parcours de test:
   - inscription -> verifier l'email recu -> cliquer le lien `/verify/email` (web Symfony) -> retour desktop login
   - login (si 2FA activee: challenge 6 chiffres)
   - mot de passe oublie -> email reset -> reset via token
   - profil -> verifier photo/publications/onglets
   - modifier profil -> changer infos + upload photo + activer/desactiver 2FA
