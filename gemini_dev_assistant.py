import os
import re
import asyncio
from pathlib import Path

from rich.console import Console
from rich.panel import Panel
from rich.markdown import Markdown
from rich.live import Live
from rich.table import Table

from gemini_webapi import GeminiClient

PROJECT_DIR = Path("/storage/emulated/0/Rozne/WinOls")
console = Console(force_terminal=True)

COOKIE_LENGTH_RANGES = {
    "__Secure-1PAPISID": (34, 34),
    "__Secure-1PSID": (150, 160),
    "__Secure-1PSIDCC": (70, 80),
    "__Secure-3PAPISID": (34, 34),
    "__Secure-3PSID": (150, 160),
    "__Secure-3PSIDCC": (70, 80),
    "__Secure-ENID": (400, 550),
    "__Secure-OSID": (150, 160),
    "__Secure-STRP": (90, 110),
    "AEC": (50, 70),
    "SEARCH_SAMESITE": (8, 12),
    "NID": (450, 550),
    "SID": (150, 160),
    "HSID": (16, 20),
    "SSID": (16, 20),
    "APISID": (34, 34),
    "SAPISID": (34, 34),
    "SIDCC": (70, 80),
    "OSID": (150, 160),
    "OTZ": (25, 35),
}


def ensure_valid_cwd():
    try:
        PROJECT_DIR.mkdir(parents=True, exist_ok=True)
        os.chdir(str(PROJECT_DIR))
    except Exception as e:
        console.print(f"[bold red][!] Błąd katalogu: {e}[/bold red]")


def load_env_vars():
    ensure_valid_cwd()
    env_path = PROJECT_DIR / ".env"
    env_dict = {}
    if not env_path.exists():
        return env_dict
    with open(env_path, "r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            line = line.strip("\r\n").strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                k, v = line.split("=", 1)
                env_dict[k.strip()] = v.strip().strip('"').strip("'")
            else:
                parts = re.split(r"[\t ]+", line, maxsplit=1)
                if len(parts) == 2:
                    env_dict[parts[0].strip()] = parts[1].strip()
    return env_dict


def render_cookie_summary_table(env_vars):
    table = Table(title="[*] DIAGNOSTYKA CIASTECZEK Z .ENV", show_header=True, header_style="bold cyan")
    table.add_column("Nazwa", style="white")
    table.add_column("Podgląd", style="dim")
    table.add_column("Długość", justify="right")
    table.add_column("Status", justify="center")
    for name, (lo, hi) in COOKIE_LENGTH_RANGES.items():
        val = env_vars.get(name, "")
        if not val:
            table.add_row(name, "[red]BRAK[/red]", "0", "[bold red]✗ BRAK[/bold red]")
            continue
        n = len(val)
        masked = (val[:6] + "..." + val[-6:]) if n > 14 else val
        ok = lo <= n <= hi
        table.add_row(name, masked, f"{n}", "[bold green]✓ OK[/bold green]" if ok else f"[bold red]✗ ({n})[/bold red]")
    console.print(table)


def build_dynamic_project_context():
    ensure_valid_cwd()
    files = []
    for root, dirs, names in os.walk(str(PROJECT_DIR)):
        dirs[:] = [d for d in dirs if d not in {".git", "build", ".idea", "__pycache__"}]
        for f in names:
            try:
                files.append(str((Path(root).relative_to(PROJECT_DIR) / f)))
            except Exception:
                pass
    lines = [
        "[PROFIL]",
        "Jesteś zaawansowanym programistą.",
        "Zwracaj pliki w formacie:",
        "### sciezka/do/Pliku",
        "```python",
        "# kod",
        "```",
        "",
        f"Struktura ({len(files)} plików):",
    ]
    lines += [f" - {p}" for p in sorted(files)[:30]]
    return "\n".join(lines)


def parse_and_apply_files(text):
    pattern = re.compile(
        r"(?:###\s*([^\n]+)|(?:#|//)\s*FILE:\s*([^\n]+))\s*\n+```[a-zA-Z]*\n(.*?)\n```",
        re.DOTALL,
    )
    for m in pattern.findall(text):
        path = (m[0] or m[1]).strip()
        body = m[2]
        if path and body:
            target = PROJECT_DIR / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(body, encoding="utf-8")
            console.print(f"[bold green][+] Zapisano: {path}[/bold green]")


async def main():
    ensure_valid_cwd()
    console.clear()
    console.print(Panel.fit("[bold green]Gemini Web API Assistant[/bold green]"))

    env = load_env_vars()
    if not env:
        console.print("[bold red][!] Brak danych w .env![/bold red]")
        return

    render_cookie_summary_table(env)

    psid = env.get("__Secure-1PSID", "")
    psidts = env.get("__Secure-1PSIDTS", "")
    if not psid:
        console.print("[bold red][!] W .env brakuje __Secure-1PSID.[/bold red]")
        return

    client = GeminiClient(psid, psidts)

    console.print("\n[yellow][*] Inicjalizacja klienta (auto-refresh cookies)...[/yellow]")
    try:
        await client.init(timeout=60, auto_close=False, close_delay=300, auto_refresh=True)
    except Exception as e:
        console.print(f"[bold red][!] Init nieudany: {e}[/bold red]")
        console.print("[yellow]Zaloguj się na gemini.google.com i wyeksportuj świeże __Secure-1PSID oraz __Secure-1PSIDTS.[/yellow]")
        return

    console.print("[bold green]Gotowy. Wpisz 'exit' aby zakończyć.[/bold green]")

    chat = None
    while True:
        try:
            user_input = console.input("\n[bold magenta]Ty > [/bold magenta]").strip()
            if user_input.lower() in {"exit", "quit", "q"}:
                break
            if not user_input:
                continue

            prompt = f"{build_dynamic_project_context()}\n\n[ZADANIE]: {user_input}"

            with Live(console=console, refresh_per_second=4) as live:
                live.update("[bold cyan][*] Wysyłanie zapytania...[/bold cyan]")
                if chat is None:
                    chat = client.start_chat()
                resp = await chat.send_message(prompt)
                live.stop()

            # Poprawka: resp.text to string, nie wywołujemy na nim await
            text = resp.text
            if text:
                console.print("\n[bold cyan]Gemini >[/bold cyan]")
                console.print(Markdown(text))
                parse_and_apply_files(text)
            else:
                console.print("[yellow]Pusta odpowiedź.[/yellow]")

        except KeyboardInterrupt:
            break
        except Exception as e:
            console.print(f"[bold red][-] Błąd: {e}[/bold red]")

    await client.close()


if __name__ == "__main__":
    asyncio.run(main())
