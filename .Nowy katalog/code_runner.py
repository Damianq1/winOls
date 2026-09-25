import subprocess
import sys
from pathlib import Path

class CodeRunner:
    def __init__(self, work_dir: str):
        self.work_dir = Path(work_dir)

    def run_cmd(self, command: str, timeout: int = 30) -> dict:
        """Uruchamia komendę powłoki w katalogu projektu i zwraca wynik."""
        try:
            res = subprocess.run(
                command,
                shell=True,
                cwd=str(self.work_dir),
                capture_output=True,
                text=True,
                timeout=timeout
            )
            return {
                "success": res.returncode == 0,
                "exit_code": res.returncode,
                "stdout": res.stdout.strip(),
                "stderr": res.stderr.strip()
            }
        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Przekroczono czas wykonania komendy ({timeout}s)."
            }
        except Exception as e:
            return {
                "success": False,
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Wyjątek podczas uruchamiania: {str(e)}"
            }

    def run_python_file(self, relative_path: str, timeout: int = 30) -> dict:
        """Uruchamia plik Pythona z projektu."""
        python_bin = sys.executable
        cmd = f"{python_bin} {relative_path}"
        return self.run_cmd(cmd, timeout=timeout)
