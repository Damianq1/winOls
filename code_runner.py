import os
import stat
import subprocess
import sys
from pathlib import Path


def ensure_executable(path: Path) -> None:
    """Nadaje uprawnienia wykonywania plikowi, jeśli ich brakuje."""
    if path.exists():
        st = os.stat(path)
        os.chmod(path, st.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def run_gradle(args: list[str]) -> int:
    gradlew = Path("./gradlew").resolve()

    if gradlew.exists():
        ensure_executable(gradlew)
        # Jawne wywołanie 'sh ./gradlew' eliminuje błąd [Errno 13] Permission denied
        # w środowiskach bez bezpośrednich praw wykonywania (np. Termux, montowane partycje)
        cmd = ["sh", str(gradlew)] + args
    else:
        cmd = ["gradle"] + args

    result = subprocess.run(cmd)
    return result.returncode


if __name__ == "__main__":
    task_args = sys.argv[1:] if len(sys.argv) > 1 else ["test"]
    sys.exit(run_gradle(task_args))