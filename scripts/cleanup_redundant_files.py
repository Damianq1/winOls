import os
import shutil
from pathlib import Path

FILES_TO_REMOVE = [
    "build.gradle.kts",
    "settings.gradle.kts",
    "app/build.gradle.kts",
    "gemini_dev_assistant (1).py",
]

DIRS_TO_REMOVE = [
    "Proponowana Architektura Kodu (Clean Architecture + MVI",
    "src",  # Kod powinien być w app/src/
]

def run_cleanup(base_dir: str = "."):
    root = Path(base_dir)
    print("Rozpoczynanie czyszczenia zbędnych plików projektu WinOls...")

    for file_rel in FILES_TO_REMOVE:
        p = root / file_rel
        if p.is_file():
            p.unlink()
            print(f"[USUNIĘTO PLIK] {file_rel}")

    for dir_rel in DIRS_TO_REMOVE:
        p = root / dir_rel
        if p.is_dir():
            shutil.rmtree(p)
            print(f"[USUNIĘTO KATALOG] {dir_rel}")

    print("Czyszczenie zakończone.")

if __name__ == "__main__":
    run_cleanup()