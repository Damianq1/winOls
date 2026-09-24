import os
import sys
import subprocess
import asyncio
import time
import re
import urllib.request
import json
from pathlib import Path
from loguru import logger
from gemini_webapi import GeminiClient

from rich.console import Console
from rich.panel import Panel
from rich.markdown import Markdown
from rich.live import Live

PROJECT_DIR = Path("/storage/emulated/0/Rozne/WinOls")
GITHUB_REPO_API = "https://api.github.com/repos/Damianq1/winOls/actions/runs?per_page=1"

console = Console(force_terminal=True)

logger.remove()
logger.add(sys.stderr, level="ERROR", format="{time:HH:mm:ss} | <level>{level}</level> | {message}")

def ensure_valid_cwd():
    try:
        if not PROJECT_DIR.exists():
            PROJECT_DIR.mkdir(parents=True, exist_ok=True)
        os.chdir(str(PROJECT_DIR))
    except Exception:
        try:
            subprocess.run(["termux-setup-storage"], capture_output=True, timeout=2)
            PROJECT_DIR.mkdir(parents=True, exist_ok=True)
            os.chdir(str(PROJECT_DIR))
        except Exception as e:
            console.print(f"[bold red][!] Błąd uprawnień/katalogu: {e}[/bold red]")

def load_env_vars():
    ensure_valid_cwd()
    env_path = PROJECT_DIR / ".env"
    if env_path.exists():
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                if "=" in line and not line.startswith("#"):
                    k, v = line.strip().split("=", 1)
                    os.environ[k] = v

load_env_vars()

PAPISID = os.environ.get("PAPISID", "")
PSIDTS = os.environ.get("GEMINI_PSIDTS", "")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")

def build_dynamic_project_context() -> str:
    ensure_valid_cwd()
    file_list = []
    
    for root, dirs, files in os.walk(str(PROJECT_DIR)):
        dirs[:] = [d for d in dirs if d not in ['.git', 'build', '.idea', '.gradle', 'app/build']]
        for f in files:
            file_path = Path(root) / f
            try:
                rel_path = file_path.relative_to(PROJECT_DIR)
                file_list.append(str(rel_path))
            except Exception:
                pass

    context_builder = [
        "[PROFIL I INSTRUKCJA SYSTEMOWA]",
        "Jesteś zaawansowanym asystentem skryptowym i programistycznym w środowisku SmartIDE.",
        "Twórz kompletny kod gotowy do wdrożenia.",
        "\n[STRUKTURA PROJEKTU]"
    ]
    
    context_builder.append(f"Liczba plików: {len(file_list)}")
    for f in sorted(file_list)[:30]:
        context_builder.append(f" - {f}")

    context_builder.append("\n[ZASADA] Zwracaj zmodyfikowane pliki w formacie: ### ścieżka/do/Pliku\n```kotlin\n...\n```")
    return "\n".join(context_builder)

def parse_multi_file_code(response_text: str) -> list:
    files_to_save = []
    pattern = re.compile(
        r'(?:(?://|#)\s*FILE:\s*([^\n]+)|###\s*([^\n]+\.(?:kt|java|xml|gradle|kts|json|txt|md|py)))\s*\n+'
        r'```[a-zA-Z]*\n(.*?)\n```',
        re.DOTALL | re.IGNORECASE
    )
    matches = pattern.findall(response_text)
    for match in matches:
        file_path = (match[0] or match[1]).strip()
        file_content = match[2]
        if file_path and file_content:
            files_to_save.append((file_path, file_content))
            
    if not files_to_save:
        code_block_pattern = re.compile(r'([a-zA-Z0-9_\-/\.]+\.(?:kt|java|xml|gradle|kts|py))\s*\n\s*```[a-zA-Z]*\n(.*?)\n```', re.DOTALL)
        fallback_matches = code_block_pattern.findall(response_text)
        for f_path, content in fallback_matches:
            files_to_save.append((f_path.strip(), content))

    return files_to_save

def apply_file_changes(parsed_files):
    ensure_valid_cwd()
    for file_path, content in parsed_files:
        clean_path = file_path.strip().lstrip('./')
        target_file = PROJECT_DIR / clean_path
        target_file.parent.mkdir(parents=True, exist_ok=True)
        with open(target_file, "w", encoding="utf-8") as f:
            f.write(content)
        console.print(f"[bold green][+] Zaktualizowano lokalnie: {clean_path}[/bold green]")

async def wait_for_github_actions() -> tuple[str, str]:
    start_time = time.time()
    headers = {"User-Agent": "SmartIDE-Assistant", "Accept": "application/vnd.github+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"token {GITHUB_TOKEN}"

    with Live(console=console, refresh_per_second=2) as live:
        while True:
            elapsed = time.time() - start_time
            live.update(f"[bold cyan][*] Oczekiwanie na wynik GitHub Actions... [yellow]({elapsed:.1f}s)[/yellow][/bold cyan]")
            
            try:
                req = urllib.request.Request(GITHUB_REPO_API, headers=headers)
                with urllib.request.urlopen(req, timeout=5) as response:
                    data = json.loads(response.read().decode())
                    runs = data.get("workflow_runs", [])
                    if runs:
                        latest = runs[0]
                        status = latest.get("status")
                        conclusion = latest.get("conclusion")
                        
                        if status == "completed":
                            return conclusion, latest.get("url", "")
            except Exception:
                pass

            await asyncio.sleep(4)

async def git_sync_and_monitor(client) -> tuple[bool, str]:
    ensure_valid_cwd()
    console.print("\n[bold cyan][*] Synchronizacja zmian z GitHubem...[/bold cyan]")
    try:
        subprocess.run(["git", "add", "."], capture_output=True, text=True, check=True, cwd=str(PROJECT_DIR))
        
        status_res = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True, cwd=str(PROJECT_DIR))
        if status_res.stdout.strip():
            subprocess.run(["git", "commit", "-m", "Auto-update via Gemini Assistant"], capture_output=True, text=True, check=True, cwd=str(PROJECT_DIR))
            push_res = subprocess.run(["git", "push"], capture_output=True, text=True, cwd=str(PROJECT_DIR))
            if push_res.returncode == 0:
                console.print("[bold green][ok] Pomyślnie wypchnięto zmiany do GitHub.[/bold green]")
            else:
                console.print(f"[bold red][!] Błąd git push: {push_res.stderr.strip()}[/bold red]")
                return False, ""
        else:
            console.print("[dim cyan][*] Brak nowych zmian do zatwierdzenia w Git.[/dim cyan]")
            return True, ""

        conclusion, run_url = await wait_for_github_actions()
        
        if conclusion == "success":
            console.print("[bold green][ok] GitHub Actions -> Budowanie zakończone SUKCESEM![/bold green]")
            return True, ""
        else:
            console.print("[bold red][!] GitHub Actions -> Budowanie ZAKOŃCZONE BŁĘDEM (failure)![/bold red]")
            prompt_fix = (
                f"[AUTOMATYCZNE ZGŁOSZENIE BŁĘDU GITHUB ACTIONS]\n"
                f"Ostatnia zmiana wywołała błąd w GitHub Actions (status: failure).\n"
                f"Przeanalizuj zmiany, znajdź przyczynę błędu i podaj poprawione pliki."
            )
            return False, prompt_fix

    except Exception as e:
        console.print(f"[bold red][!] Błąd podczas synchronizacji/monitorowania GitHub Actions: {e}[/bold red]")
        return True, ""

async def send_prompt_with_spinner(client, prompt_text: str):
    response_text = ""
    success = False
    start_time = time.time()

    async def send_task():
        nonlocal response_text, success
        chat = client.start_chat()
        response = await chat.send_message(prompt_text)
        response_text = response.text if hasattr(response, "text") else str(response)
        success = True

    task = asyncio.create_task(send_task())

    with Live(console=console, refresh_per_second=4) as live:
        while not task.done():
            elapsed = time.time() - start_time
            live.update(f"[bold cyan][*] Wysyłanie zapytania przez sesję Pro... [yellow]({elapsed:.1f}s)[/yellow][/bold cyan]")
            await asyncio.sleep(0.25)

    elapsed = time.time() - start_time
    if success:
        console.print(f"[bold green][ok] Odpowiedź odebrana w {elapsed:.2f} s[/bold green]")
        if response_text:
            console.print("\n[bold cyan]Gemini >[/bold cyan]")
            console.print(Markdown(response_text))

            parsed_files = parse_multi_file_code(response_text)
            if parsed_files:
                console.print(f"\n[bold yellow][*] Wykryto {len(parsed_files)} plik(i) do automatycznego zapisu:[/bold yellow]")
                apply_file_changes(parsed_files)
                console.print("[bold green][ok] Pliki wdrożone w projekcie.[/bold green]")
                
                is_ok, auto_fix_prompt = await git_sync_and_monitor(client)
                if not is_ok and auto_fix_prompt:
                    await send_prompt_with_spinner(client, auto_fix_prompt)
    else:
        if task.exception():
            console.print(f"[bold red][!] Błąd zapytania po {elapsed:.1f}s: {task.exception()}[/bold red]")

async def main():
    ensure_valid_cwd()
    
    console.clear()
    console.print(Panel.fit(
        "[bold green]Asystent Techniczny - Tryb Pro[/bold green]\n"
        f"Katalog roboczy: [cyan]{PROJECT_DIR}[/cyan]",
        border_style="green"
    ))

    console.print("[bold cyan][*] Inicjalizacja sesji z użyciem PAPISID...[/bold cyan]")
    try:
        client = GeminiClient(secure_1psid=PAPISID, secure_1psidts=PSIDTS)
        await client.init()
        console.print("[bold green][ok] Połączenie z kontem Pro ustanowione.[/bold green]")
    except Exception as e:
        console.print(f"[bold red][!] Błąd inicjalizacji: {e}[/bold red]")
        return

    while True:
        try:
            ensure_valid_cwd()
            user_input = console.input("\n[bold magenta]Ty > [/bold magenta]").strip()

            if user_input.lower() in ["exit", "quit", "q"]:
                break

            if not user_input:
                continue

            dynamic_context = build_dynamic_project_context()
            full_prompt = f"{dynamic_context}\n\nZADANIE UŻYTKOWNIKA:\n{user_input}"

            await send_prompt_with_spinner(client, full_prompt)

        except KeyboardInterrupt:
            console.print("\n[bold yellow][*] Zatrzymano przez użytkownika.[/bold yellow]")
            break
        except Exception as e:
            ensure_valid_cwd()
            console.print(f"\n[bold red][-] Błąd: {e}[/bold red]")

if __name__ == "__main__":
    asyncio.run(main())
