// Toggle dropdown profile
const profileBtn = document.getElementById("profileBtn");
const profileMenu = document.getElementById("profileMenu");

function closeProfileMenu() {
  profileMenu.style.display = "none";
  profileMenu.setAttribute("aria-hidden", "true");
  profileBtn.setAttribute("aria-expanded", "false");
}

function openProfileMenu() {
  profileMenu.style.display = "block";
  profileMenu.setAttribute("aria-hidden", "false");
  profileBtn.setAttribute("aria-expanded", "true");
}

profileBtn?.addEventListener("click", (e) => {
  e.stopPropagation();
  const isOpen = profileBtn.getAttribute("aria-expanded") === "true";
  if (isOpen) closeProfileMenu();
  else openProfileMenu();
});

wireTabs();

document.addEventListener("click", () => closeProfileMenu());

function wireTabs() {
  document.querySelectorAll(".tabs[data-tabs]").forEach((tabs) => {
    const key = tabs.dataset.tabs;
    const panels = document.querySelector(`.tabPanels[data-panels="${key}"]`);
    if (!panels) return;

    const buttons = tabs.querySelectorAll("[data-tab]");
    const panelItems = panels.querySelectorAll("[data-panel]");
    if (!buttons.length || !panelItems.length) return;

    const setActive = (name) => {
      buttons.forEach((btn) => btn.classList.toggle("isActive", btn.dataset.tab === name));
      panelItems.forEach((panel) => panel.classList.toggle("isActive", panel.dataset.panel === name));
    };

    buttons.forEach((btn) => {
      btn.addEventListener("click", () => setActive(btn.dataset.tab));
    });
  });
}

// Fake search (demo)
const globalSearch = document.getElementById("globalSearch");
globalSearch?.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    const q = globalSearch.value.trim();
    if (!q) return;
    // À remplacer par votre logique backend
    alert(`Search: ${q}`);
  }
});
