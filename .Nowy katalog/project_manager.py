import os
import re
from pathlib import Path

class ProjectManager:
    def __init__(self, project_dir: str):
        self.project_dir = Path(project_dir)

    def ensure_cwd(self):
        self.project_dir.mkdir(parents=True, exist_ok=True)
        os.chdir(str(self.project_dir))

    def get_lightweight_context(self) -> str:
        self.ensure_cwd()
        files = []
        for root, dirs, names in os.walk(str(self.project_dir)):
            dirs[:] = [d for d in dirs if d not in {".git", "build", ".idea", "__pycache__"}]
            for f in names:
                try:
                    files.append(str((Path(root).relative_to(self.project_dir) / f)))
                except Exception:
                    pass
        
        lines = [
            "[ZASADY PROJEKTU]",
            "Jesteś zaawansowanym programistą pracującym w tym repozytorium.",
            "Zwracaj zmiany kodu wyłącznie w formacie:",
            "### sciezka/do/pliku.py",
            "```python",
            "# kod",
            "```",
            f"\nStruktura projektu ({len(files)} plików):"
        ]
        lines += [f" - {p}" for p in sorted(files)[:35]]
        return "\n".join(lines)

    def read_file_content(self, relative_path: str) -> str:
        target = self.project_dir / relative_path
        if target.exists() and target.is_file():
            try:
                return target.read_text(encoding="utf-8")
            except Exception:
                return ""
        return ""

    def apply_changes(self, response_text: str):
        pattern = re.compile(
            r"(?:###\s*([^\n]+)|(?:#|//)\s*FILE:\s*([^\n]+))\s*\n+```[a-zA-Z]*\n(.*?)\n```",
            re.DOTALL,
        )
        matches = pattern.findall(response_text)
        for m in matches:
            path_str = (m[0] or m[1]).strip()
            new_content = m[2]
            if path_str and new_content:
                target = self.project_dir / path_str
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(new_content, encoding="utf-8")
                print(f"[+] Zapisano/Zaktualizowano plik: {path_str}")
