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

    client = GeminiClient(psid, psidts)
    print("Metody klienta:", [m for m in dir(client) if not m.startswith('_')])
    
    await client.init()
    
    if hasattr(client, 'chat'):
        print("Metody obiektu chat:", [m for m in dir(client.chat) if not m.startswith('_')])
    else:
        print("Brak bezpośredniego atrybutu client.chat, sprawdźmy start_chat:")
        chat = client.start_chat()
        print("Metody zwrotu start_chat:", [m for m in dir(chat) if not m.startswith('_')])

if __name__ == "__main__":
    asyncio.run(main())
