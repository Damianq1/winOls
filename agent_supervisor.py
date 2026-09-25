import os
import sys
import asyncio
import subprocess
import re
import json
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from rich.markdown import Markdown

from gemini_core import GeminiConnector

PROJECT_PATH = Path("/storage/emulated/0/Rozne/WinOls")
console = Console(force_terminal=True)

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

def get_project_snapshot(max_depth=3):
    """Generuje strukturę katalogu (tree) oraz podczytuje kluczowe pliki projektu dla Gemini."""
    ensure_valid_cwd()
    snapshot = "### STRUKTURA PROJEKTU (TREE):\n"
    
    # Generowanie struktury tree w Pythonie
    ignore_dirs = {".git", ".gradle", "build", ".idea", "__pycache__"}
    tree_lines = []
    for root, dirs, files in os.walk(str(PROJECT_PATH)):
        dirs[:] = [d for d in dirs if d not in ignore_dirs]
        rel_path = os.path.relpath(root, str(PROJECT_PATH))
        depth = 0 if rel_path == "." else rel_path.count(os.sep) + 1
        if depth <= max_depth:
            indent = "    " * depth
            folder_name = os.path.basename(root) if rel_path != "." else PROJECT_PATH.name
            tree_lines.append(f"{indent}📂 {folder_name}/")
            for file in sorted(files):
                if file.endswith((".py", ".yml", ".yaml", ".kts", ".xml", ".properties", ".json", ".gradle")):
                    tree_lines.append(f"{indent}    📄 {file}")
    
    snapshot += "\n".join(tree_lines[:100]) + "\n\n"
    
    # Podgląd kluczowych plików konfiguracyjnych i źródłowych
    snapshot += "### ZAWARTOŚĆ KLUCZOWYCH PLIKÓW:\n"
    key_files = ["build.gradle", "settings.gradle", "app/build.gradle", "app/src/main/AndroidManifest.xml"]
    
    for kf in key_files:
        f_path = PROJECT_PATH / kf
        if f_path.exists():
            try:
                content = f_path.read_text(encoding="utf-8", errors="ignore")
                # Ograniczenie długości pojedynczego pliku do 3000 znaków, żeby nie przepalić limitu
                if len(content) > 3000:
                    content = content[:3000] + "\n... [przycięto plik]"
                snapshot += f"\n--- PLIK: {kf} ---\n{content}\n"
            except Exception as e:
                snapshot += f"\n--- PLIK: {kf} (błąd odczytu: {e}) ---\n"
                
    return snapshot

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
            
    # Autonomiczne sprzątanie zbędnych plików workflow
    workflows_dir = PROJECT_PATH / ".github" / "workflows"
    if workflows_dir.exists():
        allowed_workflow = "android.yml"
        for wf_file in workflows_dir.iterdir():
            if wf_file.is_file() and wf_file.name != allowed_workflow:
                try:
                    wf_file.unlink()
                    console.print(f"[bold yellow][!] Autonomicznie usunięto zbędny plik workflow: .github/workflows/{wf_file.name}[/bold yellow]")
                    subprocess.run(["git", "rm", "-f", str(wf_file.relative_to(PROJECT_PATH))], cwd=str(PROJECT_PATH), capture_output=True)
                except Exception as e:
                    console.print(f"[dim]Nie udało się usunąć {wf_file.name}: {e}[/dim]")
                    
    return updated_files

def sync_with_github():
    ensure_valid_cwd()
    try:
        console.print("[yellow][*] Synchronizacja z GitHubem (git pull)...[/yellow]")
        res = subprocess.run(["git", "pull", "--rebase"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(f"[green]{res.stdout.strip()}[/green]")
    except Exception as e:
        console.print(f"[yellow][!] Ostrzeżenie przy git pull: {e}[/yellow]")

def push_to_github(msg="Auto-fix / Update code"):
    ensure_valid_cwd()
    try:
        console.print("[yellow][*] Dodawanie zmian do Gita (git add)...[/yellow]")
        subprocess.run(["git", "add", "."], cwd=str(PROJECT_PATH), capture_output=True)
        
        res_commit = subprocess.run(["git", "commit", "-m", msg], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        if res_commit.returncode == 0:
            console.print(f"[green]{res_commit.stdout.strip()}[/green]")
        else:
            console.print(f"[dim]{res_commit.stdout.strip()} {res_commit.stderr.strip()}[/dim]")

        console.print("[yellow][*] Wysyłanie zmian na GitHub (git push)...[/yellow]")
        res = subprocess.run(["git", "push"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        if res.returncode == 0:
            console.print("[bold green][✓] Zmiany zostały pomyślnie wypchnięte na GitHub![/bold green]")
            return True
        else:
            console.print(f"[yellow][!] Git push zwrócił błąd: {res.stderr.strip()}[/yellow]")
            return False
    except Exception as e:
        console.print(f"[yellow][!] Błąd wysyłania do Gita: {e}[/yellow]")
        return False

def get_github_actions_failed_logs():
    ensure_valid_cwd()
    console.print("[yellow][*] Pobieranie i filtrowanie logów z GitHub Actions...[/yellow]")
    try:
        check_gh = subprocess.run(["gh", "auth", "status"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        if check_gh.returncode != 0:
            return "[!] GitHub CLI (gh) nie jest zalogowany w Termuxue."

        res_list = subprocess.run(
            ["gh", "run", "list", "--limit", "1", "--json", "databaseId,status,conclusion"],
            cwd=str(PROJECT_PATH), capture_output=True, text=True
        )
        
        run_id = None
        if res_list.returncode == 0 and res_list.stdout.strip():
            runs = json.loads(res_list.stdout)
            if runs:
                run_id = runs[0].get("databaseId")

        raw_logs = ""
        if run_id:
            res_logs = subprocess.run(["gh", "run", "view", str(run_id), "--log-failed"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
            if res_logs.returncode == 0 and len(res_logs.stdout.strip()) > 50:
                raw_logs = res_logs.stdout.strip()
            else:
                res_all = subprocess.run(["gh", "run", "view", str(run_id), "--log"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
                if res_all.returncode == 0:
                    raw_logs = res_all.stdout.strip()

        if not raw_logs:
            res = subprocess.run(["gh", "run", "view", "--log-failed"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
            raw_logs = res.stdout.strip() if res.returncode == 0 else "Brak logów."

        lines = raw_logs.splitlines()
        filtered_lines = []
        ignore_keywords = ["java_home", "gradle_user_home", "develocity", "http://", "https://", "cache", "job", "steps", "git clone"]

        for line in lines:
            lower_line = line.lower()
            if any(ign in lower_line for ign in ignore_keywords) and not any(err in lower_line for err in ["error", "fail", "unresolved", "exception", "failed"]):
                continue
            
            if any(kw in lower_line for kw in ["error", "fail", "exception", "e: ", "unresolved", "cannot", "does not exist", "syntax", "compilation", "FAILURE:"]):
                filtered_lines.append(line)
        
        if filtered_lines:
            result = "\n".join(filtered_lines[-120:])
        else:
            clean_lines = [l for l in lines if "2026-" not in l or "Z  " not in l]
            result = "\n".join((clean_lines if clean_lines else lines)[-60:])
            
        return result[-3500:] if len(result) > 3500 else result

    except Exception as e:
        return f"[!] Nie udało się pobrać logów przez GitHub CLI: {e}"

def check_latest_workflow_status():
    ensure_valid_cwd()
    try:
        res = subprocess.run(
            ["gh", "run", "list", "--limit", "1", "--json", "status,conclusion"],
            cwd=str(PROJECT_PATH), capture_output=True, text=True
        )
        if res.returncode == 0 and res.stdout.strip():
            runs = json.loads(res.stdout)
            if runs:
                return runs[0].get("status"), runs[0].get("conclusion")
    except Exception:
        pass
    return "unknown", "unknown"

async def monitor_and_auto_fix(connector, max_attempts=3, check_interval_sec=120):
    for attempt in range(1, max_attempts + 1):
        console.print(f"\n[bold cyan][*] [Próba {attempt}/{max_attempts}] Oczekiwanie na GitHub Actions...[/bold cyan]")
        await asyncio.sleep(20)
        elapsed = 20
        
        while elapsed < 1200:
            status, conclusion = check_latest_workflow_status()
            if status == "completed":
                if conclusion == "success":
                    console.print("[bold green][✓] Sukces! GitHub Actions zakończyło budowanie pomyślnie.[/bold green]")
                    return True
                elif conclusion == "failure":
                    console.print("[bold red][!] Wykryto błąd budowania na GitHub Actions![/bold red]")
                    break
            await asyncio.sleep(check_interval_sec)
            elapsed += check_interval_sec

        snapshot = get_project_snapshot()
        issue_desc = get_github_actions_failed_logs()
        
        prompt = (
            f"Projekt: WinOls (Android ECU Binary Editor)\n"
            f"{snapshot}\n"
            f"Błąd CI:\n{issue_desc}\n\n"
            f"ZASADA BEZWZGLĘDNA: Jesteś autonomicznym programistą w Termuxie. Przeanalizuj strukturę i pliki, popraw błąd i zwróć pliki w formacie:\n"
            f"### ścieżka/do/pliku\n```kotlin lub yaml\n// kod\n```"
        )

        console.print("[cyan][*] Wysyłanie zapytania naprawczego z pełnym kontekstem projektu do Gemini...[/cyan]")
        try:
            response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=200), timeout=220)
            if response_text:
                console.print(Markdown(response_text))
                updated = apply_changes(response_text)
                if updated:
                    pushed = push_to_github(f"Auto-fix pętli CI (próba {attempt}): {', '.join(updated)}")
                    if not pushed:
                        return False
                else:
                    return False
            else:
                return False
        except Exception as e:
            console.print(f"[red][!] Wyjątek: {e}[/red]")
            return False
    return False

def run_git_status():
    ensure_valid_cwd()
    try:
        res = subprocess.run(["git", "status"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(Panel(res.stdout, title="Git Status", border_style="cyan"))
    except Exception as e:
        console.print(f"[red]Błąd git status: {e}[/red]")

async def main():
    ensure_valid_cwd()
    console.clear()
    console.print(Panel.fit("[bold green]WinOls Interactive Agent Console (Autonomous + Snapshot Mode)[/bold green]"))

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
    console.print("  [bold green]/prompt <treść>[/bold green]       - Wyślij zapytanie wraz z automatycznym snapshotem folderu")
    console.print("  [bold green]/napraw[/bold green]              - Pobierz logi, dołącz snapshot, napraw i monitoruj")
    console.print("  [bold green]/git[/bold green]                 - Sprawdź status git")
    console.print("  [bold green]/pull[/bold green]                - Pobierz zmiany z GitHub")
    console.print("  [bold green]/exit[/bold green]                - Wyjście\n")

    while True:
        try:
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
                msg = console.input("[yellow]Wiadomość commita: [/yellow]").strip()
                push_to_github(msg if msg else "Manual checkpoint")

        elif action == "/pull":
            sync_with_github()

        elif action == "/napraw":
            issue_desc = arg if arg else get_github_actions_failed_logs()
            snapshot = get_project_snapshot()

            prompt = (
                f"Projekt: WinOls (Android ECU Binary Editor)\n"
                f"{snapshot}\n"
                f"Zadanie / Błąd:\n{issue_desc}\n\n"
                f"ZASADA BEZWZGLĘDNA: Jesteś autonomicznym programistą w Termuxie. Masz pełną strukturę projektu powyżej. "
                f"Przeanalizuj pliki, dokonaj poprawek i zwróć je w formacie:\n### ścieżka/do/pliku\n```kotlin\n// kod\n```"
            )

            console.print("[cyan][*] Skanowanie projektu i wysyłanie zapytania do Gemini z pełnym kontekstem...[/cyan]")
            try:
                response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=200), timeout=220)
                if response_text:
                    console.print(Markdown(response_text))
                    updated = apply_changes(response_text)
                    if updated:
                        pushed = push_to_github(f"Auto-fix / Maintenance: {', '.join(updated)}")
                        if pushed:
                            await monitor_and_auto_fix(connector)
                    else:
                        console.print("[yellow][!] Gemini nie zwróciło zmian w plikach do zapytania.[/yellow]")
                else:
                    console.print("[yellow][!] Otrzymano pustą odpowiedź.[/yellow]")
            except TimeoutError:
                console.print("[yellow][!] Przekroczono czas oczekiwania na odpowiedź od Gemini.[/yellow]")
            except Exception as e:
                console.print(f"[red][!] Błąd komunikacji: {e}[/red]")

        elif action == "/prompt":
            if not arg:
                console.print("[red]Podaj treść zapytania po /prompt[/red]")
                continue
                
            snapshot = get_project_snapshot()
            prompt = (
                f"Projekt: WinOls (Android ECU Binary Editor)\n"
                f"{snapshot}\n"
                f"Zapytanie użytkownika:\n{arg}\n\n"
                f"ZASADA BEZWZGLĘDNA: Przeanalizuj strukturę projektu i pliki powyżej. Odpowiedz merytorycznie. "
                f"Jeśli modyfikujesz pliki, zwróć je w formacie:\n### ścieżka/do/pliku\n```kotlin lub yaml\n// kod\n```"
            )

            console.print("[cyan][*] Skanowanie folderu (tree + zawartość) i wysyłanie do Gemini...[/cyan]")
            try:
                response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=200), timeout=220)
                if response_text:
                    console.print(Markdown(response_text))
                    updated = apply_changes(response_text)
                    if updated:
                        push_to_github(f"Applied changes from prompt: {arg[:30]}")
                else:
                    console.print("[yellow][!] Otrzymano pustą odpowiedź.[/yellow]")
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
