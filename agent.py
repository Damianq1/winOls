import os
import sys
import asyncio
import subprocess
import re
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from rich.markdown import Markdown

from gemini_core import GeminiConnector

PROJECT_PATH = Path("/storage/emulated/0/Rozne/WinOls")
console = Console(force_terminal=True)

SYSTEM_CONTEXT_PREFIX = (
    "[SYSTEM CONTEXT: Działasz w środowisku Termux na systemie Android jako autonomiczny programista i agent systemowy. "
    "Masz pełny, bezpośredni dostęp do plików projektowych przez ten skrypt. "
    "Nigdy nie twórz odpowiedzi typu 'Nie mam dostępu do Twojego Termuksa' ani nie pouczaj o ograniczeniach chmurowych. "
    "Twoim jedynym zadaniem jest analiza kodu, znajdowanie błędów i zwracanie gotowych poprawek w wymaganym formacie bloków plików.]\n\n"
)

def ensure_valid_cwd():
    try:
        PROJECT_PATH.mkdir(parents=True, exist_ok=True)
        os.chdir(str(PROJECT_PATH))
    except Exception as e:
        console.print(f"[bold red][!] Błąd katalogu: {e}[/bold red]")

def load_env_vars():
    ensure_valid_cwd()
    env_path = PROJECT_PATH / ".env"
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

def apply_changes(response_text):
    ensure_valid_cwd()
    pattern = re.compile(
        r"(?:###\s*([^\n]+)|(?:#|//)\s*FILE:\s*([^\n]+))\s*\n+```[a-zA-Z]*\n(.*?)\n```",
        re.DOTALL,
    )
    matches = pattern.findall(response_text)
    updated_files = []
    for m in matches:
        path_str = (m[0] or m[1]).strip()
        body = m[2]
        if path_str and body:
            target = PROJECT_PATH / path_str
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(body, encoding="utf-8")
            console.print(f"[bold green][+] Zaktualizowano plik: {path_str}[/bold green]")
            updated_files.append(path_str)
    return updated_files

def sync_with_github():
    ensure_valid_cwd()
    try:
        console.print("[yellow][*] Synchronizacja z GitHubem (git pull)...[/yellow]")
        res = subprocess.run(["git", "pull", "--rebase"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(f"[green]{res.stdout.strip()}[/green]")
    except Exception as e:
        console.print(f"[yellow][!] Ostrzeżenie przy git pull: {e}[/yellow]")

def push_to_github():
    ensure_valid_cwd()
    try:
        console.print("[yellow][*] Wysyłanie zmian na GitHub (git push)...[/yellow]")
        res = subprocess.run(["git", "push"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        if res.returncode == 0:
            console.print("[bold green][✓] Zmiany zostały pomyślnie wypchnięte na GitHub![/bold green]")
        else:
            console.print(f"[yellow][!] Git push zwrócił błąd: {res.stderr.strip()}[/yellow]")
    except Exception as e:
        console.print(f"[yellow][!] Błąd wysyłania do Gita: {e}[/yellow]")

def run_git_status():
    ensure_valid_cwd()
    try:
        res = subprocess.run(["git", "status"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(Panel(res.stdout, title="Git Status", border_style="cyan"))
    except Exception as e:
        console.print(f"[red]Błąd git status: {e}[/red]")

def run_git_commit_and_push(msg="Manual commit"):
    ensure_valid_cwd()
    try:
        subprocess.run(["git", "add", "."], cwd=str(PROJECT_PATH), capture_output=True)
        res = subprocess.run(["git", "commit", "-m", msg], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(f"[green]{res.stdout}[/green]")
        push_to_github()
    except Exception as e:
        console.print(f"[red]Błąd commit/push: {e}[/red]")

def run_gradle_build():
    ensure_valid_cwd()
    console.print("[yellow][*] Uruchamianie kompilacji Gradle (./gradlew assembleDebug)...[/yellow]")
    try:
        res = subprocess.run(
            ["./gradlew", "assembleDebug"],
            cwd=str(PROJECT_PATH),
            capture_output=True,
            text=True,
            timeout=180
        )
        if res.returncode == 0:
            console.print("[bold green][✓] Build zakończony sukcesem bez błędów![/bold green]")
            return True, "Build OK"
        else:
            stderr_lines = res.stderr.splitlines() + res.stdout.splitlines()
            error_snippet = "\n".join([line for line in stderr_lines if "error" in line.lower() or "e: " in line or "fail" in line.lower()][-30:])
            if not error_snippet:
                error_snippet = "\n".join(stderr_lines[-25:])
            console.print(Panel(error_snippet, title="Ostatnie błędy Gradle", border_style="red"))
            return False, error_snippet
    except subprocess.TimeoutExpired:
        console.print("[red][!] Timeout kompilacji Gradle (> 180s).[/red]")
        return False, "Timeout kompilacji Gradle (> 180s)."
    except Exception as e:
        return False, str(e)

async def handle_auto_fix_loop(connector, max_retries=3):
    for attempt in range(1, max_retries + 1):
        console.print(f"\n[bold cyan][*] Próba buildu nr {attempt}/{max_retries}[/bold cyan]")
        success, build_output = run_gradle_build()
        
        if success:
            console.print("[bold green][✓] Projekt kompiluje się poprawnie![/bold green]")
            return True

        console.print(f"[yellow][!] Wykryto błędy kompilacji. Przekazuję do Gemini (próba {attempt})...[/yellow]")
        
        prompt = (
            SYSTEM_CONTEXT_PREFIX +
            f"Projekt: WinOls (Android ECU Binary Editor)\n"
            f"Wystąpił błąd podczas kompilacji Gradle (Próba {attempt}/{max_retries}). "
            f"Przeanalizuj poniższy komunikat, wskaż plik i podaj poprawkę:\n\n"
            f"--- BŁĄD ---\n{build_output}\n\n"
            f"ZASADA BEZWZGLĘDNA: Napraw błąd w kodzie i zwróć zmianę dokładnie w formacie:\n"
            f"### ścieżka/do/pliku\n```kotlin\n// kod poprawionego pliku\n```"
        )

        try:
            response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=60), timeout=70)
            if response_text:
                console.print(Markdown(response_text))
                updated = apply_changes(response_text)
                if updated:
                    run_git_commit_and_push(f"Auto-fix próba {attempt}: naprawiono {', '.join(updated)}")
                else:
                    console.print("[yellow][!] Gemini nie zwróciło bloków z plikami do aktualizacji.[/yellow]")
                    break
            else:
                console.print("[yellow][!] Otrzymano pustą odpowiedź lub limit zapytań (429).[/yellow]")
                break
        except TimeoutError:
            console.print("[yellow][!] Przekroczono czas oczekiwania na odpowiedź od Gemini.[/yellow]")
        except Exception as e:
            console.print(f"[red][!] Błąd komunikacji: {e}[/red]")
            break

    console.print("[bold red][!] Nie udało się naprawić projektu automatycznie w wyznaczonej liczbie prób.[/bold red]")
    return False

async def main():
    ensure_valid_cwd()
    console.clear()
    console.print(Panel.fit("[bold green]WinOls Interactive Agent Console[/bold green]"))

    env = load_env_vars()
    psid = env.get("__Secure-1PSID", "")
    psidts = env.get("__Secure-1PSIDTS", "")

    if not psid:
        console.print("[bold red][!] Brak __Secure-1PSID w .env[/bold red]")
        return

    connector = GeminiConnector(psid, psidts)
    console.print("[yellow][*] Inicjalizacja klienta Gemini...[/yellow]")
    try:
        if not await connector.initialize():
            console.print("[bold red][!] Błąd inicjalizacji klienta.[/bold red]")
            return
    except Exception as e:
        console.print(f"[bold red][!] Wyjątek podczas inicjalizacji: {e}[/bold red]")
        return

    console.print("[bold cyan]Dostępne komendy:[/bold cyan]")
    console.print("  [bold green]/prompt <treść>[/bold green] - Wyślij dowolne zapytanie do Gemini")
    console.print("  [bold green]/napraw[/bold green]        - Uruchom automatyczną pętlę: Build -> Błąd -> Gemini -> Poprawka")
    console.print("  [bold green]/git[/bold green]           - Sprawdź status git, zrób commit i push")
    console.print("  [bold green]/pull[/bold green]          - Pobierz zmiany z GitHub (git pull)")
    console.print("  [bold green]/exit[/bold green]          - Wyjście z programu\n")

    while True:
        try:
            ensure_valid_cwd()
            cmd_input = console.input("[bold magenta]WinOls-Agent > [/bold magenta]").strip()
        except (KeyboardInterrupt, EOFError):
            break

        if not cmd_input:
            continue

        parts = cmd_input.split(" ", 1)
        action = parts[0].lower()
        arg = parts[1] if len(parts) > 1 else ""

        if action in {"exit", "quit", "q"}:
            break

        elif action == "/git":
            run_git_status()
            do_push = console.input("[yellow]Czy zrobić commit i push? (t/n) > [/yellow]").strip().lower()
            if do_push == 't':
                msg = console.input("[yellow]Wiadomość commita (Enter domyślna): [/yellow]").strip()
                run_git_commit_and_push(msg if msg else "Interactive checkpoint")

        elif action == "/pull":
            sync_with_github()

        elif action == "/napraw":
            await handle_auto_fix_loop(connector, max_retries=3)

        elif action == "/prompt":
            if not arg:
                console.print("[red]Podaj treść zapytania po /prompt[/red]")
                continue
            console.print("[cyan][*] Wysyłanie zapytania do Gemini...[/cyan]")
            try:
                full_prompt = SYSTEM_CONTEXT_PREFIX + arg
                response_text = await asyncio.wait_for(connector.send_prompt(full_prompt, timeout_sec=60), timeout=70)
                if response_text:
                    console.print(Markdown(response_text))
                    save_file = console.input("[yellow]Czy zapisać pliki z odpowiedzi (jeśli podano)? (t/n) > [/yellow]").strip().lower()
                    if save_file == 't':
                        updated = apply_changes(response_text)
                        if updated:
                            run_git_commit_and_push(f"Applied changes from prompt")
                else:
                    console.print("[yellow][!] Otrzymano pustą odpowiedź lub limit zapytań (429).[/yellow]")
            except TimeoutError:
                console.print("[yellow][!] Przekroczono czas oczekiwania na odpowiedź od Gemini.[/yellow]")
            except Exception as e:
                console.print(f"[red][!] Błąd komunikacji: {e}[/red]")

        else:
            console.print(f"[red]Nieznana komenda: {action}. Użyj /prompt, /napraw, /git, /pull lub /exit[/red]")

    try:
        await connector.close()
    except Exception:
        pass
        
    console.print("[bold green][*] Zakończono pracę agenta.[/bold green]")

if __name__ == "__main__":
    asyncio.run(main())
