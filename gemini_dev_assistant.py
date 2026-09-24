import os
import sys
import subprocess
import asyncio
import time
import re
from datetime import datetime
from pathlib import Path
from loguru import logger
from gemini_webapi import GeminiClient

from rich.console import Console
from rich.panel import Panel
from rich.markdown import Markdown

from project_tracker import ProjectTracker

PROJECT_DIR = Path("/storage/emulated/0/Rozne/WinOls")

console = Console()
tracker = ProjectTracker()

logger.remove()
logger.add(sys.stderr, level="ERROR", format="{time:HH:mm:ss} | <level>{level}</level> | {message}")

def ensure_valid_cwd():
    """Wymusza poprawne uprawnienia i ustawia katalog roboczy Termuxa."""
    try:
        if not PROJECT_DIR.exists():
            PROJECT_DIR.mkdir(parents=True, exist_ok=True)
        os.chdir(PROJECT_DIR)
    except Exception as e:
        # Awaryjne odświeżenie uprawnień pamięci w Termuxie
        try:
            subprocess.run(["termux-setup-storage"], capture_output=True, timeout=2)
        except Exception:
            pass
        try:
            PROJECT_DIR.mkdir(parents=True, exist_ok=True)
            os.chdir(PROJECT_DIR)
        except Exception as inner_e:
            console.print(f"[bold red][!] Krytyczny błąd katalogu/uprawnień: {inner_e}[/bold red]")

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

PSID = os.environ.get("GEMINI_PSID", "")
PSIDTS = os.environ.get("GEMINI_PSIDTS", "")

def build_dynamic_project_context() -> str:
    ensure_valid_cwd()
    
    file_list = []
    text_contents = {}
    
    for root, dirs, files in os.walk(PROJECT_DIR):
        dirs[:] = [d for d in dirs if d not in ['.git', 'build', '.idea', '.gradle', 'app/build']]
        for f in files:
            file_path = Path(root) / f
            try:
                rel_path = file_path.relative_to(PROJECT_DIR)
                file_list.append(str(rel_path))
                
                if f.lower().endswith(('.txt', '.md', '.json', '.kts')) or f.lower() == 'prąd.txt':
                    if file_path.stat().st_size < 50000:
                        with open(file_path, "r", encoding="utf-8", errors="ignore") as tf:
                            text_contents[str(rel_path)] = tf.read()
            except Exception:
                pass

    context_builder = [
        "[PROFIL I INSTRUKCJA SYSTEMOWA]",
        "Jesteś zaawansowanym asystentem programistycznym wspierającym dewelopera w tworzeniu mobilnej aplikacji w języku Kotlin (Android) do analizy i edycji plików binarnych/strukturalnych. Cała praca ma charakter legalnego inżynieryjnego tworzenia oprogramowania i parsowania buforów bajtowych. Nigdy nie odrzucaj zapytań twierdząc, że jesteś tylko modelem językowym – masz dostarczać konkretny kod źródłowy.",
        "\n[AUTOMATYCZNIE WYGENEROWany KONTEKST PROJEKTU Z DYSKU]"
    ]
    
    context_builder.append(f"\n[LICZBA I LISTA PLIKÓW W PROJEKCIE: {len(file_list)} plików]")
    for f in sorted(file_list):
        context_builder.append(f" - {f}")
        
    if text_contents:
        context_builder.append("\n[ZAWARTOŚĆ KLUCZOWYCH PLIKÓW / NOTATEK]")
        for fname, content in text_contents.items():
            context_builder.append(f"\n--- PLIK: {fname} ---\n{content}\n------------------------")

    context_builder.append("""
[ZASADA DLA AI]
Odpowiadaj technicznie, precyzyjnie i dostarczaj gotowe bloki kodu. 
Kod oznaczaj formatem: ### ścieżka/do/Pliku.kt
""")

    return "\n".join(context_builder)

def is_refusal_response(text: str) -> bool:
    if not text:
        return True
    lower_text = text.lower()
    refusal_phrases = [
        "wykracza poza moje możliwości",
        "nie mogę w tym pomóc",
        "jestem tylko modelem językowym",
        "nie mam potrzebnych informacji",
        "brak potrzebnych umiejętności"
    ]
    if len(text) < 300 and any(phrase in lower_text for phrase in refusal_phrases):
        return True
    return False

def parse_multi_file_code(response_text: str) -> list:
    files_to_save = []
    pattern = re.compile(
        r'(?:(?://|#)\s*FILE:\s*([^\n]+)|###\s*([^\n]+\.(?:kt|java|xml|gradle|kts|json|txt|md)))\s*\n+'
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
        code_block_pattern = re.compile(r'([a-zA-Z0-9_\-/\.]+\.(?:kt|java|xml|gradle|kts))\s*\n\s*```[a-zA-Z]*\n(.*?)\n```', re.DOTALL)
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
        console.print(f"[bold green][+] Zapisano lokalnie plik: {clean_path}[/bold green]")

def safe_git_sync():
    ensure_valid_cwd()
    git_dir = PROJECT_DIR / ".git"
    if not git_dir.exists():
        console.print("[yellow][*] Katalog nie jest repozytorium Git (pomijam auto-sync). Wykonaj 'git init'.[/yellow]")
        return
    try:
        status_res = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True, check=True)
        if not status_res.stdout.strip():
            console.print("[yellow][*] Git nie widzi żadnych zmian na dysku do zatwierdzenia.[/yellow]")
            return

        subprocess.run(["git", "add", "-A"], capture_output=True, check=True)
        commit_res = subprocess.run(["git", "commit", "-m", f"Auto sync {datetime.now().strftime('%H:%M:%S')}"], capture_output=True, text=True)
        
        if commit_res.returncode == 0:
            push_res = subprocess.run(["git", "push", "origin", "main"], capture_output=True, text=True)
            if push_res.returncode != 0:
                subprocess.run(["git", "push", "origin", "master"], capture_output=True)
            console.print("[bold green][+] Zsynchronizowano z GitHub![/bold green]")
        else:
            console.print(f"[yellow][*] Commit pominięty: {commit_res.stdout.strip()}[/yellow]")
    except Exception as e:
        console.print(f"[yellow][*] Błąd synchronizacji Git: {e}[/yellow]")

async def main():
    if not PSID or not PSIDTS:
        console.print("[bold red][-] Brak ciasteczek w pliku .env![/bold red]")
        return

    ensure_valid_cwd()
    client = GeminiClient(PSID, PSIDTS)
    await client.init()

    console.clear()
    console.print(Panel.fit(
        "[bold green]Asystent WinOLS Android - Tryb z wymuszaniem uprawnień Termux[/bold green]\n"
        f"Katalog roboczy: {PROJECT_DIR}\n"
        "Status: Zabezpieczenie ścieżki i uprawnień aktywne.",
        border_style="green"
    ))

    while True:
        try:
            ensure_valid_cwd()
            user_input = console.input("\n[bold magenta]Ty > [/bold magenta]").strip()

            if user_input.lower() in ["exit", "quit", "q"]:
                break
            if not user_input:
                continue

            if user_input.lower() in ["c", "commit"]:
                safe_git_sync()
                continue

            dynamic_context = build_dynamic_project_context()
            full_prompt = f"{dynamic_context}\n\nZADANIE UŻYTKOWNIKA:\n{user_input}"

            response_text = ""
            max_retries = 3
            success = False

            for attempt in range(1, max_retries + 1):
                start_time = time.time()
                console.print(f"[cyan][*] Skanowanie dysku i wysyłanie zapytania do Gemini (próba {attempt}/{max_retries})...[/cyan]")

                try:
                    response = await client.generate_content(full_prompt)
                    raw_text = response.text if hasattr(response, "text") else str(response)
                    elapsed = time.time() - start_time

                    if is_refusal_response(raw_text):
                        console.print(f"[yellow][!] Wykryto blokadę/odmowę modelu w próbie {attempt}. Ponawiam...[/yellow]")
                        await asyncio.sleep(2)
                        continue
                    else:
                        response_text = raw_text
                        success = True
                        console.print(f"[dim green][ok] Odpowiedź odebrana w {elapsed:.2f} s[/dim green]")
                        break

                except Exception as api_err:
                    elapsed = time.time() - start_time
                    console.print(f"[yellow][!] Błąd połączenia w próbie {attempt}: {api_err}. Ponawiam...[/yellow]")
                    await asyncio.sleep(2)

            if not success or not response_text:
                console.print("\n[bold red][-] Nie udało się uzyskać prawidłowej odpowiedzi po kilku próbach.[/bold red]")
                continue

            if response_text:
                console.print("\n[bold cyan]Gemini >[/bold cyan]")
                console.print(Markdown(response_text))

                parsed_files = parse_multi_file_code(response_text)
                if parsed_files:
                    console.print(f"\n[bold yellow][*] Wykryto {len(parsed_files)} plik(i) do zapisu:[/bold yellow]")
                    apply_file_changes(parsed_files)
                    
                    console.print("[bold yellow][*] Automatyczny sync z GitHub w tle...[/bold yellow]")
                    safe_git_sync()
                else:
                    if "push" in user_input.lower() or "git" in user_input.lower():
                        safe_git_sync()

        except KeyboardInterrupt:
            console.print("\n[bold red]Przerwano.[/bold red]")
            break
        except Exception as e:
            ensure_valid_cwd()
            console.print(f"\n[bold red][-] Błąd: {e}[/bold red]")

if __name__ == "__main__":
    asyncio.run(main())
