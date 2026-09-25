import os
import asyncio
from pathlib import Path
from gemini_webapi import GeminiClient

PROJECT_DIR = Path("/storage/emulated/0/Rozne/WinOls")

async def main():
    env_path = PROJECT_DIR / ".env"
    psid, psidts = "", ""
    if env_path.exists():
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                if "=" in line and not line.startswith("#"):
                    k, v = line.strip().split("=", 1)
                    if k == "GEMINI_PSID":
                        psid = v
                    elif k == "GEMINI_PSIDTS":
                        psidts = v

    print("[*] Inicjalizacja klienta i sesji czatu...")
    client = GeminiClient(psid, psidts)
    await client.init()
    
    chat = client.start_chat()
    print("[*] Wysyłanie 'Ping' przez sesję czatu...")
    response = await chat.send_message("Ping")
    print(f"[+] Sukces! Odpowiedź:\n{response.text if hasattr(response, 'text') else response}")

if __name__ == "__main__":
    asyncio.run(main())
