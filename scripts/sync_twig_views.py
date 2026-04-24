import re
import html
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESKTOP = ROOT / "pulse-desktop-javafx"
APP_CONTROLLER = DESKTOP / "src" / "main" / "java" / "com" / "pulse" / "desktop" / "ui" / "AppController.java"
ROUTES_DIR = DESKTOP / "src" / "main" / "resources" / "fxml" / "pages" / "routes"


def clean_text(value: str) -> str:
    value = re.sub(r"\{#.*?#\}", " ", value, flags=re.S)
    value = re.sub(r"\{%.+?%\}", " ", value, flags=re.S)
    value = re.sub(r"\{\{.+?\}\}", " ", value, flags=re.S)
    value = re.sub(r"<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", value).strip()


def escape_xml(value: str) -> str:
    return html.escape(value or "", quote=True)


def style_from_attrs(attrs: str, default: str = "btn-ghost") -> str:
    attrs = attrs or ""
    if "btn--primary" in attrs or "cta--green" in attrs:
        return "btn-primary"
    if "btn--soft" in attrs or "cta--glass" in attrs:
        return "btn-soft"
    return default


def route_from_attrs(attrs: str) -> str | None:
    match = re.search(r"path\('([^']+)'", attrs or "")
    return match.group(1) if match else None


def template_for_route(route_id: str) -> Path | None:
    if route_id == "front_home":
        file = ROOT / "templates" / "front" / "home" / "index.html.twig"
        return file if file.exists() else None

    slug = route_id.replace("front_", "", 1).replace("_", "-")
    candidates = [
        ROOT / "templates" / "front" / "pages" / f"{slug}.html.twig",
        ROOT / "templates" / "front" / "partials" / f"_{slug}.html.twig",
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def parse_forms(twig: str) -> list[dict]:
    forms = []
    for index, match in enumerate(re.finditer(r"<form\b([^>]*)>(.*?)</form>", twig, flags=re.S | re.I)):
        attrs, body = match.group(1), match.group(2)
        action = route_from_attrs(attrs) or "self"

        fields: list[str] = []
        for name in re.findall(r"<(?:input|select|textarea)\b[^>]*\bname=\"([^\"]+)\"", body, flags=re.I):
            if name not in fields:
                fields.append(name)

        for widget in re.findall(r"form_widget\(\s*[A-Za-z_][A-Za-z0-9_]*\.([A-Za-z0-9_\.]+)", body):
            field_name = widget.replace(".", "_")
            if field_name not in fields:
                fields.append(field_name)

        buttons: list[dict] = []
        for b_match in re.finditer(r"<button\b([^>]*)>(.*?)</button>", body, flags=re.S | re.I):
            b_attrs, b_text = b_match.group(1), b_match.group(2)
            buttons.append({
                "label": clean_text(b_text)[:80] or "Action",
                "route": route_from_attrs(b_attrs) or action,
                "style": style_from_attrs(b_attrs),
            })

        for a_match in re.finditer(r"<a\b([^>]*)>(.*?)</a>", body, flags=re.S | re.I):
            a_attrs, a_text = a_match.group(1), a_match.group(2)
            if "btn" not in a_attrs:
                continue
            target = route_from_attrs(a_attrs)
            if not target:
                continue
            buttons.append({
                "label": clean_text(a_text)[:80] or "Lien",
                "route": target,
                "style": style_from_attrs(a_attrs),
            })

        dedup = []
        seen = set()
        for button in buttons:
            key = (button["label"], button["route"])
            if key in seen:
                continue
            seen.add(key)
            dedup.append(button)

        forms.append({
            "idx": index,
            "action": action,
            "fields": fields[:18],
            "buttons": dedup[:8],
        })

    return forms


def parse_actions(twig: str) -> list[dict]:
    actions: list[dict] = []
    for match in re.finditer(r"<a\b([^>]*)>(.*?)</a>", twig, flags=re.S | re.I):
        attrs, text = match.group(1), match.group(2)
        if "btn" not in attrs and "topbar__item" not in attrs:
            continue
        target = route_from_attrs(attrs)
        if not target:
            continue
        actions.append({
            "label": clean_text(text)[:80] or target,
            "route": target,
            "style": style_from_attrs(attrs),
        })

    dedup = []
    seen = set()
    for action in actions:
        key = (action["label"], action["route"])
        if key in seen:
            continue
        seen.add(key)
        dedup.append(action)
    return dedup[:18]


def parse_headings(twig: str) -> tuple[str | None, list[str]]:
    hero_title = None
    hero_match = re.search(r"hero_title\s*:\s*'([^']+)'", twig)
    if hero_match:
        hero_title = hero_match.group(1).strip()

    headings: list[str] = []
    for match in re.finditer(r"<h[1-3][^>]*>(.*?)</h[1-3]>", twig, flags=re.S | re.I):
        heading = clean_text(match.group(1))
        if heading and heading not in headings:
            headings.append(heading)

    return hero_title, headings[:8]


def get_generic_routes() -> list[dict]:
    text = APP_CONTROLLER.read_text(encoding="utf-8")
    return [
        {"id": m.group(1), "title": m.group(2), "type": m.group(3)}
        for m in re.finditer(r'addRoute\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\)', text)
        if m.group(3) == "generic"
    ]


def generate_view(route: dict) -> str:
    route_id = route["id"]
    route_title = route["title"]
    template = template_for_route(route_id)
    twig = template.read_text(encoding="utf-8", errors="ignore") if template else ""

    hero_title, headings = parse_headings(twig)
    forms = parse_forms(twig)
    actions = parse_actions(twig)

    page_title = hero_title or (headings[0] if headings else route_title)
    rel_template = template.relative_to(ROOT).as_posix() if template else "template introuvable"

    lines: list[str] = []
    add = lines.append
    add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    add("")
    add("<?import javafx.geometry.Insets?>")
    add("<?import javafx.scene.control.Button?>")
    add("<?import javafx.scene.control.Label?>")
    add("<?import javafx.scene.control.ScrollPane?>")
    add("<?import javafx.scene.control.TextField?>")
    add("<?import javafx.scene.layout.FlowPane?>")
    add("<?import javafx.scene.layout.HBox?>")
    add("<?import javafx.scene.layout.Priority?>")
    add("<?import javafx.scene.layout.Region?>")
    add("<?import javafx.scene.layout.VBox?>")
    add("")
    add("<ScrollPane xmlns=\"http://javafx.com/javafx/21\" xmlns:fx=\"http://javafx.com/fxml/1\"")
    add("            fx:controller=\"com.pulse.desktop.ui.RoutePageController\"")
    add("            fitToWidth=\"true\" hbarPolicy=\"NEVER\" styleClass=\"page-scroll\">")
    add("    <content>")
    add("        <VBox fx:id=\"pageRoot\" spacing=\"16.0\" styleClass=\"page-root\">")
    add("            <padding>")
    add("                <Insets top=\"18.0\" right=\"18.0\" bottom=\"22.0\" left=\"18.0\"/>")
    add("            </padding>")
    add("")
    add("            <VBox spacing=\"8.0\" styleClass=\"hero-mini-box\">")
    add("                <Label text=\"PULSE FRONT\" styleClass=\"heroKicker\"/>")
    add(f"                <Label fx:id=\"routeNameLabel\" text=\"{escape_xml(page_title)}\" styleClass=\"heroMini__title\"/>")
    add(f"                <Label fx:id=\"routeDescriptionLabel\" text=\"Structure synchronisee avec {escape_xml(rel_template)}\" wrapText=\"true\" styleClass=\"heroMini__sub\"/>")
    add("                <HBox spacing=\"8.0\" styleClass=\"breadcrumbs-row\">")
    add("                    <Label text=\"Accueil\" styleClass=\"muted\"/>")
    add("                    <Label text=\">\" styleClass=\"muted\"/>")
    add(f"                    <Label fx:id=\"routeIdLabel\" text=\"Route Symfony: {escape_xml(route_id)}\" styleClass=\"muted\"/>")
    add("                </HBox>")
    add("            </VBox>")
    add("")

    if headings:
        add("            <VBox spacing=\"8.0\" styleClass=\"panel\">")
        add("                <Label text=\"SECTIONS\" styleClass=\"panel-title\"/>")
        for heading in headings[:6]:
            add(f"                <Label text=\"- {escape_xml(heading)}\" styleClass=\"muted\"/>")
        add("            </VBox>")
        add("")

    for form in forms:
        add("            <VBox spacing=\"10.0\" styleClass=\"panel\">")
        add("                <HBox alignment=\"CENTER_LEFT\" spacing=\"8.0\" styleClass=\"panel-head-row\">")
        add(f"                    <Label text=\"FORMULAIRE {form['idx'] + 1}\" styleClass=\"panel-title\"/>")
        add("                    <Region HBox.hgrow=\"ALWAYS\"/>")
        add(f"                    <Label text=\"Action: {escape_xml(form['action'])}\" styleClass=\"badge-soft\"/>")
        add("                </HBox>")

        if form["fields"]:
            add("                <FlowPane hgap=\"8.0\" vgap=\"8.0\" prefWrapLength=\"1200.0\">")
            for field in form["fields"]:
                field_text = escape_xml(field)
                add("                    <VBox spacing=\"4.0\" prefWidth=\"280.0\">")
                add(f"                        <Label text=\"{field_text}\" styleClass=\"field-label\"/>")
                add(f"                        <TextField promptText=\"{field_text}\" styleClass=\"input\"/>")
                add("                    </VBox>")
            add("                </FlowPane>")

        add("                <HBox spacing=\"8.0\" styleClass=\"form-actions-row\">")
        if form["buttons"]:
            for index, button in enumerate(form["buttons"]):
                button_id = f"submit__{button['route']}__{form['idx']}_{index}"
                add(f"                    <Button id=\"{escape_xml(button_id)}\" text=\"{escape_xml(button['label'])}\" styleClass=\"{escape_xml(button['style'])}\"/>")
        else:
            button_id = f"submit__{form['action']}__{form['idx']}_0"
            add(f"                    <Button id=\"{escape_xml(button_id)}\" text=\"Valider\" styleClass=\"btn-primary\"/>")
        add("                </HBox>")
        add("            </VBox>")
        add("")

    if actions:
        add("            <VBox spacing=\"10.0\" styleClass=\"panel\">")
        add("                <Label text=\"ACTIONS\" styleClass=\"panel-title\"/>")
        add("                <FlowPane hgap=\"8.0\" vgap=\"8.0\" prefWrapLength=\"1200.0\">")
        for index, action in enumerate(actions):
            button_id = f"nav__{action['route']}__{index}"
            add(f"                    <Button id=\"{escape_xml(button_id)}\" text=\"{escape_xml(action['label'])}\" styleClass=\"{escape_xml(action['style'])}\"/>")
        add("                </FlowPane>")
        add("            </VBox>")
        add("")

    add("            <VBox spacing=\"10.0\" styleClass=\"panel\">")
    add("                <HBox alignment=\"CENTER_LEFT\" spacing=\"8.0\" styleClass=\"panel-head-row\">")
    add("                    <VBox spacing=\"4.0\">")
    add("                        <Label fx:id=\"sectionHeadingLabel\" text=\"RESULTATS\" styleClass=\"panel-title\"/>")
    add("                        <Label fx:id=\"sectionSubheadingLabel\" text=\"Elements lies a cette page.\" styleClass=\"panel-desc\"/>")
    add("                    </VBox>")
    add("                    <Region HBox.hgrow=\"ALWAYS\"/>")
    add("                    <Button id=\"submit__self__refresh\" text=\"Refresh\" onAction=\"#refreshCards\" styleClass=\"btn-ghost\"/>")
    add("                    <Button id=\"nav__front_home__home\" text=\"Accueil\" onAction=\"#goHome\" styleClass=\"btn-soft\"/>")
    add("                </HBox>")
    add("                <FlowPane fx:id=\"cardsPane\" hgap=\"12.0\" vgap=\"12.0\" styleClass=\"cards-grid\" prefWrapLength=\"1300.0\"/>")
    add("            </VBox>")
    add("        </VBox>")
    add("    </content>")
    add("</ScrollPane>")

    return "\n".join(lines) + "\n"


def main():
    ROUTES_DIR.mkdir(parents=True, exist_ok=True)
    count = 0
    for route in get_generic_routes():
        output = generate_view(route)
        target = ROUTES_DIR / f"{route['id']}-view.fxml"
        target.write_text(output, encoding="utf-8")
        count += 1
    print(f"Generated {count} route FXML views from Twig structure.")


if __name__ == "__main__":
    main()
