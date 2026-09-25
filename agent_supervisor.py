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

def apply_changes(response_text):
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
    try:
        console.print("[yellow][*] Synchronizacja z GitHubem (git pull)...[/yellow]")
        res = subprocess.run(["git", "pull", "--rebase"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(f"[green]{res.stdout.strip()}[/green]")
    except Exception as e:
        console.print(f"[yellow][!] Ostrzeżenie przy git pull: {e}[/yellow]")

def push_to_github(msg="Auto-fix / Update code"):
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
            console.print("[bold cyan][i] GitHub Actions przejmie teraz kompilację w chmurze.[/bold cyan]")
            return True
        else:
            console.print(f"[yellow][!] Git push zwrócił błąd: {res.stderr.strip()}[/yellow]")
            return False
    except Exception as e:
        console.print(f"[yellow][!] Błąd wysyłania do Gita: {e}[/yellow]")
        return False

def get_github_actions_failed_logs():
    """Pobiera logi z GitHub Actions, odrzuca nagłówki środowiskowe i wyciąga faktyczne błędy."""
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
        console.print(f"\n[bold cyan][*] [Próba {attempt}/{max_attempts}] Oczekiwanie na GitHub Actions (sprawdzanie co {check_interval_sec//60} min)...[/bold cyan]")
        
        await asyncio.sleep(20)
        elapsed = 20
        
        while elapsed < 1200:
            status, conclusion = check_latest_workflow_status()
            console.print(f"[dim]Status CI: status={status}, conclusion={conclusion} (Minęło {elapsed}s)[/dim]")
            
            if status == "completed":
                if conclusion == "success":
                    console.print("[bold green][✓] Sukces! GitHub Actions zakończyło budowanie pomyślnie.[/bold green]")
                    return True
                elif conclusion == "failure":
                    console.print("[bold red][!] Wykryto błąd budowania (failure) na GitHub Actions![/bold red]")
                    break
            
            await asyncio.sleep(check_interval_sec)
            elapsed += check_interval_sec

        console.print("[yellow][*] Pobieranie logów błędów z GitHub Actions do automatycznej naprawy...[/yellow]")
        issue_desc = get_github_actions_failed_logs()
        console.print(Panel(issue_desc[:500] + "..." if len(issue_desc) > 500 else issue_desc, title="Pobrane logi błędów CI", border_style="yellow"))

        prompt = (
            f"Projekt: WinOls (Android ECU Binary Editor)\n"
            f"Automatyczna pętla naprawcza (Próba {attempt}/{max_attempts}). Wykryto błąd w GitHub Actions:\n{issue_desc}\n\n"
            f"ZASADA BEZWZGLĘDNA: Jesteś autonomicznym programistą. Znajdź błąd w kodzie, popraw go i zwróć zmianę w formacie:\n"
            f"### ścieżka/do/pliku\n```kotlin\n// kod poprawionego pliku\n```"
        )

        console.print("[cyan][*] Wysyłanie zapytania naprawczego do Gemini (zwiększony timeout)...[/cyan]")
        try:
            response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=200), timeout=220)
            if response_text:
                console.print(Markdown(response_text))
                updated = apply_changes(response_text)
                if updated:
                    pushed = push_to_github(f"Auto-fix pętli CI (próba {attempt}): {', '.join(updated)}")
                    if not pushed:
                        console.print("[red][!] Błąd wypychania poprawki na GitHub. Przerywam pętlę.[/red]")
                        return False
                else:
                    console.print("[yellow][!] Gemini nie zwróciło zmian w plikach do aktualizacji.[/yellow]")
                    return False
            else:
                console.print("[yellow][!] Otrzymano pustą odpowiedź lub limit zapytań (429).[/yellow]")
                return False
        except Exception as e:
            console.print(f"[red][!] Wyjątek w pętli naprawczej: {e}[/red]")
            return False

    console.print("[bold red][!] Osiągnięto maksymalną liczbę prób automatycznej naprawy.[/bold red]")
    return False

def run_git_status():
    try:
        res = subprocess.run(["git", "status"], cwd=str(PROJECT_PATH), capture_output=True, text=True)
        console.print(Panel(res.stdout, title="Git Status", border_style="cyan"))
    except Exception as e:
        console.print(f"[red]Błąd git status: {e}[/red]")

async def main():
    ensure_valid_cwd()
    console.clear()
    console.print(Panel.fit("[bold green]WinOls Interactive Agent Console (Auto-Loop CI/CD Mode)[/bold green]"))

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
    console.print("  [bold green]/prompt <treść>[/bold green]       - Wyślij dowolne zapytanie do Gemini")
    console.print("  [bold green]/napraw[/bold green]              - Pobierz logi, napraw, wyślij i uruchom pętlę monitorującą")
    console.print("  [bold green]/napraw <opis błędu>[/bold green] - Napraw na podstawie opisu, wyślij i monitoruj w pętli")
    console.print("  [bold green]/git[/bold green]                 - Sprawdź status git, zrób commit i push")
    console.print("  [bold green]/pull[/bold green]                - Pobierz zmiany z GitHub (git pull)")
    console.print("  [bold green]/exit[/bold green]                - Wyjście z programu\n")

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
                msg = console.input("[yellow]Wiadomość commita (Enter domyślna): [/yellow]").strip()
                push_to_github(msg if msg else "Manual checkpoint")

        elif action == "/pull":
            sync_with_github()

        elif action == "/napraw":
            if arg:
                issue_desc = arg
                console.print(f"[cyan][*] Analiza opisu błędu: {issue_desc}[/cyan]")
            else:
                issue_desc = get_github_actions_failed_logs()
                console.print(Panel(issue_desc[:500] + "..." if len(issue_desc) > 500 else issue_desc, title="Pobrane logi GitHub Actions", border_style="yellow"))

            prompt = (
                f"Projekt: WinOls (Android ECU Binary Editor)\n"
                f"Błąd / Logi z GitHub Actions:\n{issue_desc}\n\n"
                f"ZASADA BEZWZGLĘDNA: Jesteś autonomicznym programistą. Znajdź błąd w kodzie, popraw go i zwróć zmianę w formacie:\n"
                f"### ścieżka/do/pliku\n```kotlin\n// kod poprawionego pliku\n```"
            )

            console.print("[cyan][*] Wysyłanie zapytania naprawczego do Gemini (zwiększony timeout do 200s)...[/cyan]")
            try:
                response_text = await asyncio.wait_for(connector.send_prompt(prompt, timeout_sec=200), timeout=220)
                if response_text:
                    console.print(Markdown(response_text))
                    updated = apply_changes(response_text)
                    if updated:
                        pushed = push_to_github(f"Auto-fix z logów CI: {', '.join(updated)}")
                        if pushed:
                            await monitor_and_auto_fix(connector)
                else:
                    console.print("[yellow][!] Otrzymano pustą odpowiedź lub limit zapytań (429).[/yellow]")
            except TimeoutError:
                console.print("[yellow][!] Przekroczono czas oczekiwania na odpowiedź od Gemini (timeout 220s).[/yellow]")
            except Exception as e:
                console.print(f"[red][!] Błąd komunikacji: {e}[/red]")

        elif action == "/prompt":
            if not arg:
                console.print("[red]Podaj treść zapytania po /prompt[/red]")
                continue
            console.print("[cyan][*] Wysyłanie zapytania do Gemini...[/cyan]")
            try:
                response_text = await asyncio.wait_for(connector.send_prompt(arg, timeout_sec=200), timeout=220)
                if response_text:
                    console.print(Markdown(response_text))
                    save_file = console.input("[yellow]Czy zapisać pliki z odpowiedzi (jeśli podano)? (t/n) > [/yellow]").strip().lower()
                    if save_file == 't':
                        updated = apply_changes(response_text)
                        if updated:
                            push_to_github(f"Applied changes from prompt")
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
