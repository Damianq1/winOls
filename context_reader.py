import os

FILES_TO_MONITOR = [
    "agent.py",
    "gemini_core.py",
    "agent_supervisor.py",
    "code_runner.py",
    "state_machine.py",
    "project_manager.py",
    "gemini_dev_assistant.py",
    "project_tracker.py"
]

def get_project_context(base_dir="/storage/emulated/0/Rozne/WinOls/"):
    """Automatycznie czyta zawartość plików agenta, imitując lokalny cat, 
    aby Gemini miało pełny wgląd w kod bez pytania użytkownika."""
    context = ""
    for filename in FILES_TO_MONITOR:
        filepath = os.path.join(base_dir, filename)
        if os.path.exists(filepath):
            context += f"\n--- PLIK: {filename} ---\n"
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    context += f.read()
            except Exception as e:
                context += f"[Błąd odczytu pliku: {e}]\n"
    return context

if __name__ == "__main__":
    print(f"Załadowano kontekst dla {len(FILES_TO_MONITOR)} plików.")
