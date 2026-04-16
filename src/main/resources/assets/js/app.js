// Fallback SVG si jamais une image Internet ne charge pas
const FALLBACK_SVG = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="1400" height="800" viewBox="0 0 1400 800">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#071422"/>
      <stop offset="1" stop-color="#0a1c2f"/>
    </linearGradient>
    <linearGradient id="a" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#28ff8a"/>
      <stop offset="1" stop-color="#2bd2ff"/>
    </linearGradient>
  </defs>
  <rect width="1400" height="800" fill="url(#g)"/>
  <circle cx="1050" cy="200" r="210" fill="#28ff8a" opacity="0.10"/>
  <circle cx="320" cy="580" r="260" fill="#2bd2ff" opacity="0.10"/>
  <rect x="160" y="370" width="1080" height="18" rx="9" fill="url(#a)" opacity="0.85"/>
  <text x="160" y="330" fill="#e9f2ff" font-size="44" font-family="Arial" font-weight="700">Image placeholder</text>
  <text x="160" y="420" fill="#e9f2ff" opacity="0.65" font-size="20" font-family="Arial">
    Fallback si l’image distante est bloquée.
  </text>
</svg>
`)}`;

// Helpers DOM
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

// Safe BG loader
function setBgSafe(el, url) {
  const pre = new Image();
  try { pre.referrerPolicy = "no-referrer"; } catch {}
  pre.onload = () => { el.style.backgroundImage = `url("${url}")`; };
  pre.onerror = () => { el.style.backgroundImage = `url("${FALLBACK_SVG}")`; };
  pre.src = url;
}

function applyDatasetBackgrounds(){
  $$("[data-bg]").forEach(el => {
    if (el.dataset.bg) setBgSafe(el, el.dataset.bg);
  });
  $$("[data-avatar]").forEach(el => {
    if (el.dataset.avatar) setBgSafe(el, el.dataset.avatar);
  });
}

function wireTabs(){
  $$(".tabs[data-tabs]").forEach((tabs) => {
    const key = tabs.dataset.tabs;
    const panelWrap = key ? document.querySelector(`.tabPanels[data-panels="${key}"]`) : null;
    if (!panelWrap) return;

    const buttons = tabs.querySelectorAll("[data-tab]");
    const panels = panelWrap.querySelectorAll("[data-panel]");
    if (!buttons.length || !panels.length) return;

    const activate = (name) => {
      buttons.forEach(btn => btn.classList.toggle("is-active", btn.dataset.tab === name));
      panels.forEach(panel => panel.classList.toggle("is-active", panel.dataset.panel === name));
    };

    buttons.forEach(btn => {
      btn.addEventListener("click", () => activate(btn.dataset.tab));
    });
  });
}

function wireSideNav(){
  const sideNav = document.querySelector(".sideNav");
  const toggleBtn = document.querySelector(".menuBtn");
  if (!sideNav || !toggleBtn) return;

  const media = window.matchMedia("(min-width: 981px)");

  if (!sideNav.id) sideNav.id = "sideNav";
  toggleBtn.setAttribute("aria-controls", sideNav.id);
  toggleBtn.setAttribute("aria-expanded", "false");

  let backdrop = document.querySelector(".sideNavBackdrop");
  if (!backdrop){
    backdrop = document.createElement("div");
    backdrop.className = "sideNavBackdrop";
    document.body.appendChild(backdrop);
  }

  const setOpen = (open) => {
    if (media.matches){
      document.body.classList.toggle("sideNav-collapsed", !open);
      document.body.classList.remove("sideNav-open");
    } else {
      document.body.classList.toggle("sideNav-open", open);
      document.body.classList.remove("sideNav-collapsed");
    }
    toggleBtn.setAttribute("aria-expanded", open ? "true" : "false");
  };

  // Default: open on desktop, closed on mobile
  setOpen(media.matches);

  if (typeof media.addEventListener === "function"){
    media.addEventListener("change", (e) => setOpen(e.matches));
  }

  toggleBtn.addEventListener("click", () => {
    const isOpen = media.matches
      ? !document.body.classList.contains("sideNav-collapsed")
      : document.body.classList.contains("sideNav-open");
    setOpen(!isOpen);
  });

  backdrop.addEventListener("click", () => setOpen(false));
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") setOpen(false);
  });

  sideNav.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      if (!media.matches) setOpen(false);
    });
  });
}

// =====================
// Modal login (inchangé)
// =====================
function openAuth(){ $("#authModal").setAttribute("aria-hidden","false"); }
function closeAuth(){ $("#authModal").setAttribute("aria-hidden","true"); }

function wireAuth(){
  const openBtn = $("#btnOpenAuth");
  const modal = $("#authModal");
  const form = $("#loginForm");
  const forgot = $("#forgotLink");

  if (!openBtn || !modal || !form) return;

  openBtn.addEventListener("click", openAuth);

  modal.addEventListener("click", (e) => {
    if (e.target?.dataset?.close === "true") closeAuth();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeAuth();
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    alert(`Template login: ${data.get("email")} (à connecter au backend)`);
    closeAuth();
  });

  if (forgot){
    forgot.addEventListener("click", (e) => {
      e.preventDefault();
      alert("Template: redirection Mot de passe oublié.");
    });
  }
}

// =========================
// Données fake (template UI)
// =========================

// Gardé depuis ton template (pour ne pas “perdre” des lignes)
// -> mais on ne l’utilise plus tel quel, on a de nouvelles sections plus riches.
const tournaments = [
  { game:"Valorant", date:"Sun, 19 Jan 12:30", title:"Another Tournament Title", desc:"One more contest is coming up. Be sure to sign up for this.", img:"https://picsum.photos/seed/puls_t1/1200/800" },
  { game:"Fortnite", date:"Mon, 20 Jan 10:50", title:"Fortnite Epic Challenge", desc:"The incredible challenge awaits you in this random paragraph.", img:"https://picsum.photos/seed/puls_t2/1200/800" },
  { game:"League",   date:"Sat, 18 Jan 10:00", title:"Winter 2026 Tournament", desc:"Compete with your friends in this winter seasonal themed championship.", img:"https://picsum.photos/seed/puls_t3/1200/800" },
  { game:"CS2",      date:"Wed, 22 Jan 18:00", title:"Counter-Strike Arena Cup", desc:"BO3 bracket — teams fight for the main prize pool.", img:"https://picsum.photos/seed/puls_t4/1200/800" }
];

// Nouveaux datasets : tournois semaine, champions, best sellers, jeux populaires, équipes, joueurs, matchs
const weekTournaments = [
  {
    id: "t1",
    title: "Pulse Invitational — Valorant",
    game: "Valorant",
    format: "BO3",
    status: "OPEN",
    startLabel: "Cette semaine",
    prizePool: "1200 DT",
    progress: 20,
    matchesDone: 4,
    matchesTotal: 20,
    img: "https://picsum.photos/seed/pulse_week_t1/1200/800",
    organizer: "Pulse Org"
  },
  {
    id: "t2",
    title: "CS2 Arena Cup — Winter Split",
    game: "CS2",
    format: "BO1",
    status: "ONGOING",
    startLabel: "Mer 18:00",
    prizePool: "900 DT",
    progress: 55,
    matchesDone: 11,
    matchesTotal: 20,
    img: "https://picsum.photos/seed/pulse_week_t2/1200/800",
    organizer: "Arena League"
  },
  {
    id: "t3",
    title: "League Clash — Weekend Major",
    game: "League of Legends",
    format: "BO5",
    status: "OPEN",
    startLabel: "Sam 10:00",
    prizePool: "2500 DT",
    progress: 0,
    matchesDone: 0,
    matchesTotal: 16,
    img: "https://picsum.photos/seed/pulse_week_t3/1200/800",
    organizer: "Community"
  },
  {
    id: "t4",
    title: "Fortnite Zero Build Cup",
    game: "Fortnite",
    format: "BO1",
    status: "OPEN",
    startLabel: "Ven 20:00",
    prizePool: "700 DT",
    progress: 0,
    matchesDone: 0,
    matchesTotal: 24,
    img: "https://picsum.photos/seed/pulse_week_t4/1200/800",
    organizer: "Pulse Org"
  }
];

const weekChampions = [
  {
    id: "c1",
    team: "Nebula Five",
    tournament: "Pulse Invitational — Valorant",
    game: "Valorant",
    score: "3 - 1",
    img: "https://picsum.photos/seed/pulse_champ_1/1200/800",
    teamLogo: "https://picsum.photos/seed/pulse_team_logo_1/200/200",
    mvp: "ZED_99"
  },
  {
    id: "c2",
    team: "North Hydra",
    tournament: "CS2 Arena Cup — Winter Split",
    game: "CS2",
    score: "2 - 0",
    img: "https://picsum.photos/seed/pulse_champ_2/1200/800",
    teamLogo: "https://picsum.photos/seed/pulse_team_logo_2/200/200",
    mvp: "FrostByte"
  },
  {
    id: "c3",
    team: "Sandstorm",
    tournament: "League Clash — Weekend Major",
    game: "League of Legends",
    score: "3 - 2",
    img: "https://picsum.photos/seed/pulse_champ_3/1200/800",
    teamLogo: "https://picsum.photos/seed/pulse_team_logo_3/200/200",
    mvp: "Kairo"
  },
  {
    id: "c4",
    team: "Aurora Squad",
    tournament: "Fortnite Zero Build Cup",
    game: "Fortnite",
    score: "Final",
    img: "https://picsum.photos/seed/pulse_champ_4/1200/800",
    teamLogo: "https://picsum.photos/seed/pulse_team_logo_4/200/200",
    mvp: "NovaAim"
  }
];

const bestSellers = [
  {
    id: "p1",
    name: "Pulse Hoodie — Neon",
    team: "Nebula Five",
    price: "89 DT",
    stock: 14,
    sold: 122,
    img: "https://picsum.photos/seed/pulse_prod_1/1200/800"
  },
  {
    id: "p2",
    name: "Team Jersey — Pro Edition",
    team: "North Hydra",
    price: "69 DT",
    stock: 7,
    sold: 98,
    img: "https://picsum.photos/seed/pulse_prod_2/1200/800"
  },
  {
    id: "p3",
    name: "Mousepad XL — Arena",
    team: "Aurora Squad",
    price: "39 DT",
    stock: 33,
    sold: 210,
    img: "https://picsum.photos/seed/pulse_prod_3/1200/800"
  },
  {
    id: "p4",
    name: "Cap — Limited Drop",
    team: "Sandstorm",
    price: "29 DT",
    stock: 5,
    sold: 76,
    img: "https://picsum.photos/seed/pulse_prod_4/1200/800"
  }
];

const popularGames = [
  {
    id: "g1",
    name: "Valorant",
    category: "FPS",
    activeTournaments: 6,
    img: "https://picsum.photos/seed/pulse_game_1/1200/800",
    publisher: "Riot"
  },
  {
    id: "g2",
    name: "Counter-Strike 2",
    category: "FPS",
    activeTournaments: 4,
    img: "https://picsum.photos/seed/pulse_game_2/1200/800",
    publisher: "Valve"
  },
  {
    id: "g3",
    name: "League of Legends",
    category: "MOBA",
    activeTournaments: 5,
    img: "https://picsum.photos/seed/pulse_game_3/1200/800",
    publisher: "Riot"
  },
  {
    id: "g4",
    name: "Fortnite",
    category: "BR",
    activeTournaments: 3,
    img: "https://picsum.photos/seed/pulse_game_4/1200/800",
    publisher: "Epic"
  }
];

const teams = [
  { id:"team1", name:"Nebula Five", region:"MENA", members: 12, img:"https://picsum.photos/seed/pulse_team_1/1200/800", logo:"https://picsum.photos/seed/pulse_team_logo_1/200/200" },
  { id:"team2", name:"North Hydra", region:"EU", members: 9, img:"https://picsum.photos/seed/pulse_team_2/1200/800", logo:"https://picsum.photos/seed/pulse_team_logo_2/200/200" },
  { id:"team3", name:"Sandstorm", region:"MENA", members: 15, img:"https://picsum.photos/seed/pulse_team_3/1200/800", logo:"https://picsum.photos/seed/pulse_team_logo_3/200/200" },
  { id:"team4", name:"Aurora Squad", region:"NA", members: 10, img:"https://picsum.photos/seed/pulse_team_4/1200/800", logo:"https://picsum.photos/seed/pulse_team_logo_4/200/200" }
];

const players = [
  { id:"u1", username:"ZED_99", displayName:"Zed", country:"TN", role:"PLAYER", img:"https://picsum.photos/seed/pulse_user_1/1200/800", avatar:"https://picsum.photos/seed/pulse_avatar_1/200/200", teams:["Nebula Five"] },
  { id:"u2", username:"FrostByte", displayName:"Frost", country:"FR", role:"CAPTAIN", img:"https://picsum.photos/seed/pulse_user_2/1200/800", avatar:"https://picsum.photos/seed/pulse_avatar_2/200/200", teams:["North Hydra"] },
  { id:"u3", username:"Kairo", displayName:"Kairo", country:"TN", role:"PLAYER", img:"https://picsum.photos/seed/pulse_user_3/1200/800", avatar:"https://picsum.photos/seed/pulse_avatar_3/200/200", teams:["Sandstorm"] },
  { id:"u4", username:"NovaAim", displayName:"Nova", country:"US", role:"PLAYER", img:"https://picsum.photos/seed/pulse_user_4/1200/800", avatar:"https://picsum.photos/seed/pulse_avatar_4/200/200", teams:["Aurora Squad"] }
];

const matches = [
  { id:"m1", title:"Nebula Five vs North Hydra", tournament:"CS2 Arena Cup", status:"SCHEDULED", when:"Jeu 20:00", img:"https://picsum.photos/seed/pulse_match_1/1200/800" },
  { id:"m2", title:"Sandstorm vs Aurora Squad", tournament:"League Clash", status:"ONGOING", when:"Live", img:"https://picsum.photos/seed/pulse_match_2/1200/800" },
  { id:"m3", title:"Nebula Five vs Sandstorm", tournament:"Pulse Invitational", status:"FINISHED", when:"Terminé", img:"https://picsum.photos/seed/pulse_match_3/1200/800" }
];

// =========================
// Renderers (cards variants)
// =========================
function renderWeekTournaments(){
  const grid = $("#weekTournamentsGrid");
  if (!grid) return;
  grid.innerHTML = weekTournaments.map(t => `
    <article class="card card--tournament" data-type="tournament" data-id="${t.id}">
      <div class="card__media" data-bg="${t.img}">
        <div class="card__chips">
          <span class="chip chip--status">${t.status}</span>
          <span class="chip chip--format">${t.format}</span>
          <span class="chip">${t.game}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${t.title}</h4>
        <p class="card__desc">Organisateur: <b>${t.organizer}</b> • Départ: <b>${t.startLabel}</b></p>

        <div class="card__metaRow">
          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z"/>
            </svg>
            Prize: <b>${t.prizePool}</b>
          </span>

          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M3 5h18v2H3V5zm0 6h18v2H3v-2zm0 6h18v2H3v-2z"/>
            </svg>
            Matchs: <b>${t.matchesDone}/${t.matchesTotal}</b>
          </span>
        </div>

        <div class="progress" aria-label="Progression tournoi">
          <div class="progress__bar" style="width:${t.progress}%"></div>
        </div>
      </div>
    </article>
  `).join("");

  $$("#weekTournamentsGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
}

function renderWeekChampions(){
  const grid = $("#championsGrid");
  if (!grid) return;
  grid.innerHTML = weekChampions.map(c => `
    <article class="card card--champion" data-type="champion" data-id="${c.id}">
      <div class="card__media" data-bg="${c.img}">
        <div class="card__chips">
          <span class="chip chip--trophy">CHAMPION</span>
          <span class="chip">${c.game}</span>
          <span class="chip">${c.score}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${c.team}</h4>
        <p class="card__desc">${c.tournament}</p>

        <div class="avatarRow">
          <div class="avatar" data-avatar="${c.teamLogo}" aria-hidden="true"></div>
          <div class="avatarText">
            <div class="name">MVP: ${c.mvp}</div>
            <div class="sub">Clique → page équipe / tournoi</div>
          </div>
        </div>
      </div>
    </article>
  `).join("");

  $$("#championsGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
  $$("#championsGrid .avatar").forEach(el => setBgSafe(el, el.dataset.avatar));
}

function renderBestSellers(){
  const grid = $("#bestSellersGrid");
  if (!grid) return;
  grid.innerHTML = bestSellers.map(p => `
    <article class="card card--product" data-type="product" data-id="${p.id}">
      <div class="card__media" data-bg="${p.img}">
        <div class="card__chips">
          <span class="chip chip--price">${p.price}</span>
          <span class="chip">Stock: ${p.stock}</span>
          <span class="chip">Sold: ${p.sold}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${p.name}</h4>
        <p class="card__desc">Vendeur: <b>${p.team}</b> • Produit “best seller”</p>

        <div class="card__metaRow">
          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M7 18c-1.1 0-2-.9-2-2V6h2v10h12v2H7zM9 6h10v10H9V6z"/>
            </svg>
            Boutique: <b>${p.team}</b>
          </span>

          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M12 21s-7-4.35-7-10a4 4 0 017-2 4 4 0 017 2c0 5.65-7 10-7 10z"/>
            </svg>
            Favoris: <b>+</b>
          </span>
        </div>
      </div>
    </article>
  `).join("");

  $$("#bestSellersGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
}

function renderPopularGames(){
  const grid = $("#popularGamesGrid");
  if (!grid) return;
  grid.innerHTML = popularGames.map(g => `
    <article class="card card--game" data-type="game" data-id="${g.id}">
      <div class="card__media" data-bg="${g.img}">
        <div class="card__chips">
          <span class="chip chip--category">${g.category}</span>
          <span class="chip">Actifs: ${g.activeTournaments}</span>
          <span class="chip">${g.publisher}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${g.name}</h4>
        <p class="card__desc">Jeu populaire basé sur l’activité des tournois (OPEN/ONGOING).</p>

        <div class="card__metaRow">
          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M3 7h18v10H3V7zm2 2v6h14V9H5z"/>
            </svg>
            Tournois: <b>${g.activeTournaments}</b>
          </span>

          <span class="metaPill">
            <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
              <path d="M12 2l4 8 8 2-8 2-4 8-4-8-8-2 8-2 4-8z"/>
            </svg>
            Trend: <b>HOT</b>
          </span>
        </div>
      </div>
    </article>
  `).join("");

  $$("#popularGamesGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
}

function renderTopTeams(){
  const grid = $("#topTeamsGrid");
  if (!grid) return;
  grid.innerHTML = teams.map(t => `
    <article class="card card--team" data-type="team" data-id="${t.id}">
      <div class="card__media" data-bg="${t.img}">
        <div class="card__chips">
          <span class="chip chip--region">${t.region}</span>
          <span class="chip">Membres: ${t.members}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${t.name}</h4>
        <p class="card__desc">Équipe e-sport • Tournois + Boutique + Recrutement.</p>

        <div class="avatarRow">
          <div class="avatar" data-avatar="${t.logo}" aria-hidden="true"></div>
          <div class="avatarText">
            <div class="name">${t.name}</div>
            <div class="sub">Region: ${t.region} • Members: ${t.members}</div>
          </div>
        </div>
      </div>
    </article>
  `).join("");

  $$("#topTeamsGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
  $$("#topTeamsGrid .avatar").forEach(el => setBgSafe(el, el.dataset.avatar));
}

function renderNewPlayers(){
  const grid = $("#newPlayersGrid");
  if (!grid) return;
  grid.innerHTML = players.map(u => `
    <article class="card card--member" data-type="member" data-id="${u.id}">
      <div class="card__media" data-bg="${u.img}">
        <div class="card__chips">
          <span class="chip chip--role">${u.role}</span>
          <span class="chip">${u.country}</span>
          <span class="chip">@${u.username}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${u.displayName}</h4>
        <p class="card__desc">Équipes: <b>${u.teams.join(", ")}</b></p>

        <div class="avatarRow">
          <div class="avatar" data-avatar="${u.avatar}" aria-hidden="true"></div>
          <div class="avatarText">
            <div class="name">${u.displayName}</div>
            <div class="sub">Profil public • Message • Invitation équipe</div>
          </div>
        </div>
      </div>
    </article>
  `).join("");

  $$("#newPlayersGrid .card__media").forEach(el => setBgSafe(el, el.dataset.bg));
  $$("#newPlayersGrid .avatar").forEach(el => setBgSafe(el, el.dataset.avatar));
}

// =========================
// Recherche globale (multi)
// =========================
function normalize(str){
  return String(str ?? "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function includesAny(haystack, needle){
  return normalize(haystack).includes(normalize(needle));
}

function buildSearchGroups(query){
  const q = normalize(query).trim();

  const findIn = (list, pickText) => {
    return list.filter(item => includesAny(pickText(item), q));
  };

  const resTournaments = findIn(weekTournaments, t => `${t.title} ${t.game} ${t.format} ${t.status} ${t.organizer}`);
  const resChampions  = findIn(weekChampions,  c => `${c.team} ${c.tournament} ${c.game} ${c.mvp}`);
  const resProducts   = findIn(bestSellers,    p => `${p.name} ${p.team} ${p.price}`);
  const resGames      = findIn(popularGames,   g => `${g.name} ${g.category} ${g.publisher}`);
  const resTeams      = findIn(teams,          t => `${t.name} ${t.region} ${t.members}`);
  const resPlayers    = findIn(players,        u => `${u.username} ${u.displayName} ${u.country} ${u.role} ${u.teams.join(" ")}`);
  const resMatches    = findIn(matches,        m => `${m.title} ${m.tournament} ${m.status} ${m.when}`);

  return [
    { key:"tournaments", label:"TOURNOIS", count: resTournaments.length, items: resTournaments },
    { key:"champions",   label:"CHAMPIONS", count: resChampions.length, items: resChampions },
    { key:"products",    label:"PRODUITS", count: resProducts.length, items: resProducts },
    { key:"games",       label:"JEUX", count: resGames.length, items: resGames },
    { key:"teams",       label:"ÉQUIPES", count: resTeams.length, items: resTeams },
    { key:"players",     label:"JOUEURS", count: resPlayers.length, items: resPlayers },
    { key:"matches",     label:"MATCHS", count: resMatches.length, items: resMatches }
  ];
}

function renderSearchResults(query){
  const wrap = $("#searchResultsWrap");
  const groups = buildSearchGroups(query);

  const total = groups.reduce((acc, g) => acc + g.count, 0);
  $("#searchResultsSub").textContent = `Recherche: “${query}” • ${total} résultat(s)`;

  const groupHtml = groups.map(g => {
    const inner = (g.items || []).slice(0, 8).map(item => renderSearchCardByType(g.key, item)).join("");

    return `
      <div class="searchGroup" data-group="${g.key}">
        <div class="searchGroup__head">
          <h3 class="searchGroup__title">${g.label}</h3>
          <div class="searchGroup__count">${g.count} résultat(s)</div>
        </div>

        ${g.count === 0 ? `<div class="muted">Aucun résultat.</div>` : `
          <div class="cardsGrid">
            ${inner}
          </div>
        `}
      </div>
    `;
  }).join("");

  wrap.innerHTML = groupHtml;

  // appliquer backgrounds (media + avatars)
  $$("#searchResultsWrap .card__media").forEach(el => {
    if (el.dataset.bg) setBgSafe(el, el.dataset.bg);
  });
  $$("#searchResultsWrap .avatar").forEach(el => {
    if (el.dataset.avatar) setBgSafe(el, el.dataset.avatar);
  });

  // afficher la section
  $("#searchResultsSection").hidden = false;

  // scroll doux vers résultats
  $("#searchResultsSection").scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderSearchCardByType(typeKey, item){
  // Ici on réutilise les mêmes designs (card--tournament / card--game / etc.)
  // Tout est “template” : tu connecteras plus tard aux vraies tables.
  if (typeKey === "tournaments"){
    return `
      <article class="card card--tournament">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--status">${item.status}</span>
            <span class="chip chip--format">${item.format}</span>
            <span class="chip">${item.game}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.title}</h4>
          <p class="card__desc">Prize: <b>${item.prizePool}</b> • ${item.matchesDone}/${item.matchesTotal} matchs</p>
          <div class="progress"><div class="progress__bar" style="width:${item.progress}%"></div></div>
        </div>
      </article>
    `;
  }

  if (typeKey === "champions"){
    return `
      <article class="card card--champion">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--trophy">CHAMPION</span>
            <span class="chip">${item.game}</span>
            <span class="chip">${item.score}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.team}</h4>
          <p class="card__desc">${item.tournament}</p>
          <div class="avatarRow">
            <div class="avatar" data-avatar="${item.teamLogo}"></div>
            <div class="avatarText">
              <div class="name">MVP: ${item.mvp}</div>
              <div class="sub">Team spotlight</div>
            </div>
          </div>
        </div>
      </article>
    `;
  }

  if (typeKey === "products"){
    return `
      <article class="card card--product">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--price">${item.price}</span>
            <span class="chip">Stock: ${item.stock}</span>
            <span class="chip">Sold: ${item.sold}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.name}</h4>
          <p class="card__desc">Vendeur: <b>${item.team}</b></p>
        </div>
      </article>
    `;
  }

  if (typeKey === "games"){
    return `
      <article class="card card--game">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--category">${item.category}</span>
            <span class="chip">Actifs: ${item.activeTournaments}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.name}</h4>
          <p class="card__desc">Publisher: <b>${item.publisher}</b></p>
        </div>
      </article>
    `;
  }

  if (typeKey === "teams"){
    return `
      <article class="card card--team">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--region">${item.region}</span>
            <span class="chip">Membres: ${item.members}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.name}</h4>
          <p class="card__desc">Équipe • Tournois • Boutique</p>
          <div class="avatarRow">
            <div class="avatar" data-avatar="${item.logo}"></div>
            <div class="avatarText">
              <div class="name">${item.name}</div>
              <div class="sub">${item.region} • ${item.members} membres</div>
            </div>
          </div>
        </div>
      </article>
    `;
  }

  if (typeKey === "players"){
    return `
      <article class="card card--member">
        <div class="card__media" data-bg="${item.img}">
          <div class="card__chips">
            <span class="chip chip--role">${item.role}</span>
            <span class="chip">${item.country}</span>
            <span class="chip">@${item.username}</span>
          </div>
        </div>
        <div class="card__body">
          <h4 class="card__title">${item.displayName}</h4>
          <p class="card__desc">Équipes: <b>${item.teams.join(", ")}</b></p>
          <div class="avatarRow">
            <div class="avatar" data-avatar="${item.avatar}"></div>
            <div class="avatarText">
              <div class="name">${item.displayName}</div>
              <div class="sub">Profil • Message • Invite</div>
            </div>
          </div>
        </div>
      </article>
    `;
  }

  // matches
  return `
    <article class="card card--tournament">
      <div class="card__media" data-bg="${item.img}">
        <div class="card__chips">
          <span class="chip">${item.status}</span>
          <span class="chip">${item.when}</span>
          <span class="chip">${item.tournament}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${item.title}</h4>
        <p class="card__desc">Match (template) — clique → détail match</p>
      </div>
    </article>
  `;
}

function wireGlobalSearch(){
  const input = $("#globalSearchInput");
  const form = $("#globalSearchForm");
  const clear = $("#globalSearchClear");
  const close = $("#searchResultsClose");
  const section = $("#searchResultsSection");
  if (!input || !form || !clear || !close || !section) return;

  const runSearch = () => {
    const q = input.value.trim();
    if (q.length < 2){
      section.hidden = true;
      return;
    }
    renderSearchResults(q);
  };

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    runSearch();
  });

  input.addEventListener("input", () => {
    // recherche “live” douce, mais on garde un seuil 2 caractères
    const q = input.value.trim();
    if (q.length < 2){
      section.hidden = true;
      return;
    }
    renderSearchResults(q);
  });

  clear.addEventListener("click", () => {
    input.value = "";
    input.focus();
    section.hidden = true;
  });

  close.addEventListener("click", () => {
    section.hidden = true;
  });
}

// =========================
// Placeholder old cards grid
// =========================
function renderCards(){
  // Ancienne fonction conservée (pour respecter le template),
  // mais on n’a plus l’élément #tournamentsGrid sur la page.
  // On garde le code + on le neutralise proprement.

  const maybeGrid = $("#tournamentsGrid");
  if (!maybeGrid) return;

  maybeGrid.innerHTML = tournaments.map(t => `
    <article class="card">
      <div class="card__media" data-bg="${t.img}">
        <div class="card__chips">
          <span class="chip">${t.game}</span>
          <span class="chip">${t.date}</span>
        </div>
      </div>
      <div class="card__body">
        <h4 class="card__title">${t.title}</h4>
        <p class="card__desc">${t.desc}</p>
      </div>
    </article>
  `).join("");

  $$(".card__media").forEach(el => setBgSafe(el, el.dataset.bg));
}

// =========================
// Init
// =========================
function init(){
  // Grande image hero (remplace par ton image après)
  const heroImage = "/assets/img/ll.png";
  const heroBg = $("#heroBg");
  if (heroBg && !heroBg.dataset.bg) setBgSafe(heroBg, heroImage);

  // Wire auth + search
  wireAuth();
  wireGlobalSearch();
  wireSideNav();
  wireTabs();
  applyDatasetBackgrounds();

  // Render homepage sections
  renderWeekTournaments();
  renderWeekChampions();
  renderBestSellers();
  renderPopularGames();
  renderTopTeams();
  renderNewPlayers();

  // Keep old call (no-op now unless #tournamentsGrid existe)
  renderCards();
}

init();
