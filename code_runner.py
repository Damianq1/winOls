import os
import stat
import subprocess
import sys
from pathlib import Path


def ensure_executable(path: Path) -> None:
    """Upewnia się, że plik ma uprawnienia do wykonywania."""
    if path.exists():
        current_mode = os.stat(path).st_mode
        os.chmod(path, current_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def run_gradle_task(task: str, project_root: str = ".") -> subprocess.CompletedProcess:
    root = Path(project_root).resolve()
    gradlew_path = root / "gradlew"

    if gradlew_path.exists():
        ensure_executable(gradlew_path)
        # Wywołanie przez sh zapobiega Errno 13 w przypadku braku praw do bezpośredniego exec
        cmd = ["sh", str(gradlew_path), task]
    else:
        cmd = ["gradle", task]

    print(f"Uruchamianie: {' '.join(cmd)}")
    return subprocess.run(cmd, cwd=root, capture_output=True, text=True)


if __name__ == "__main__":
    task_name = sys.argv[1] if len(sys.argv) > 1 else "test"
    result = run_gradle_task(task_name)
    sys.stdout.write(result.stdout)
    sys.stderr.write(result.stderr)
    sys.exit(result.returncode)