import os
import json
from datetime import datetime
from rich.console import Console
from rich.table import Table

console = Console()

STATUS_FILE = "project_status.json"

DEFAULT_MODULES = {
    "core_buffer": {
        "name": "Obsługa plików binarnych (EcuBuffer / BinaryBufferManager)",
        "files": ["app/src/main/java/com/winols/app/core/BinaryBufferManager.kt"],
        "status": "pending"
    },
    "map_definition": {
        "name": "Definicje i wzory przeliczeniowe map (MapDefinition / BinModel)",
        "files": ["app/src/main/java/com/winols/app/model/MapDefinition.kt", "app/src/main/java/com/winols/app/BinModel.kt"],
        "status": "pending"
    },
    "checksum_engine": {
        "name": "Silnik sum kontrolnych (ChecksumEngine)",
        "files": ["app/src/main/java/com/winols/app/core/ChecksumEngine.kt"],
        "status": "pending"
    },
    "map_finder": {
        "name": "Automatyczny skaner i wyszukiwarka map (MapFinderEngine)",
        "files": ["app/src/main/java/com/winols/app/utils/MapFinderEngine.kt"],
        "status": "pending"
    },
    "ui_hex_view": {
        "name": "Widok edytora Hex i tabeli 2D/3D (Custom Views)",
        "files": ["app/src/main/java/com/winols/app/ui/EcuGridView.kt", "app/src/main/res/layout/activity_main.xml"],
        "status": "pending"
    },
    "exporter": {
        "name": "Eksport zmian ORI vs MOD do CSV/XLS (ExcelExporter)",
        "files": ["app/src/main/java/com/winols/app/utils/ExcelExporter.kt"],
        "status": "pending"
    }
}

class ProjectTracker:
    def __init__(self, root_dir="."):
        self.root_dir = root_dir
        self.status_path = os.path.join(root_dir, STATUS_FILE)
        self.state = self._load_or_init_state()

    def _load_or_init_state(self):
        if os.path.exists(self.status_path):
            try:
                with open(self.status_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception:
                pass
        
        return {
            "created_at": datetime.now().isoformat(),
            "last_updated": datetime.now().isoformat(),
            "iteration_count": 0,
            "successful_builds": 0,
            "failed_builds": 0,
            "modules": DEFAULT_MODULES
        }

    def save_state(self):
        self.state["last_updated"] = datetime.now().isoformat()
        with open(self.status_path, 'w', encoding='utf-8') as f:
            json.dump(self.state, f, indent=2, ensure_ascii=False)

    def increment_iteration(self, success=False):
        self.state["iteration_count"] += 1
        if success:
            self.state["successful_builds"] += 1
        else:
            self.state["failed_builds"] += 1
        self.sync_with_filesystem()
        self.save_state()

    def sync_with_filesystem(self):
        """Sprawdza na dysku, czy pliki modułów istnieją i są niepuste."""
        for mod_id, mod_info in self.state["modules"].items():
            all_exist = True
            for fpath in mod_info["files"]:
                full_p = os.path.join(self.root_dir, fpath)
                if not os.path.exists(full_p) or os.path.getsize(full_p) < 100:
                    all_exist = False
                    break
            
            if all_exist and mod_info["status"] == "pending":
                mod_info["status"] = "in_progress"

    def mark_module_completed(self, module_id):
        if module_id in self.state["modules"]:
            self.state["modules"][module_id]["status"] = "completed"
            self.save_state()

    def get_architectural_reminder_prompt(self):
        """Tworzy zwięzły zastrzyk pamięci dla Gemini przypominający o celu głównym."""
        completed = [m["name"] for m in self.state["modules"].values() if m["status"] == "completed"]
        pending = [m["name"] for m in self.state["modules"].values() if m["status"] != "completed"]

        reminder = (
            f"\n\n========================================================\n"
            f"[PRZYPOMNIENIE ARCHITEKTONICZNE AGENTA - ITERACJA #{self.state['iteration_count']}]\n"
            f"Cel: Tworzymy mobilny edytor map ECU WinOls na Androida (com.winols.app).\n"
            f"Moduły zaliczone: {len(completed)}/{len(self.state['modules'])}\n"
            f"Oczekujące/w trakcie: {', '.join(pending) if pending else 'Wszystkie szkielety gotowe!'}\n"
            f"ZASADA: Poprawiaj błędy bez usuwania istniejącej logiki. Zachowaj kompatybilność typów.\n"
            f"========================================================\n"
        )
        return reminder

    def print_summary_table(self):
        """Wyświetla estetyczną tabelę stanu w konsoli Termux."""
        table = Table(title=f"Stan Projektu WinOls (Iteracja {self.state['iteration_count']})", show_header=True, header_style="bold magenta")
        table.add_column("Moduł", style="cyan")
        table.add_column("Status", style="yellow")
        table.add_column("Pliki", style="dim")

        for mod_info in self.state["modules"].values():
            status = mod_info["status"]
            if status == "completed":
                status_str = "[bold green]COMPLETED[/bold green]"
            elif status == "in_progress":
                status_str = "[bold yellow]IN PROGRESS[/bold yellow]"
            else:
                status_str = "[dim red]PENDING[/dim red]"

            table.add_row(mod_info["name"], status_str, ", ".join([os.path.basename(f) for f in mod_info["files"]]))

        console.print(table)
