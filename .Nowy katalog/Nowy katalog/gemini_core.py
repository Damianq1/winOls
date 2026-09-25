import asyncio
from gemini_webapi import GeminiClient

class GeminiConnector:
    def __init__(self, psid: str, psidts: str):
        self.psid = psid
        self.psidts = psidts
        self.client = None

    async def initialize(self) -> bool:
        if self.client:
            return True
        try:
            print("[*] Inicjalizacja klienta Gemini...")
            self.client = GeminiClient(self.psid, self.psidts)
            await self.client.init(timeout=30, auto_close=False, close_delay=300, auto_refresh=False)
            print("[+] Klient Gemini gotowy.")
            return True
        except Exception as e:
            print(f"[!] Błąd inicjalizacji: {e}")
            self.client = None
            return False

    async def send_prompt(self, prompt: str, timeout_sec: int = 60) -> str:
        if not await self.initialize():
            return ""

        try:
            # Próbujemy wygenerować odpowiedź bezpośrednio przez klienta, pomijając chat
            response = await asyncio.wait_for(
                self.client.generate_content(prompt),
                timeout=timeout_sec
            )
            
            text = getattr(response, "text", None)
            if not text and isinstance(response, str):
                text = response

            if text:
                return text
            
            print("[!] Otrzymano pustą odpowiedź od modelu.")

        except asyncio.TimeoutError:
            print(f"[!] Timeout (> {timeout_sec}s) podczas generowania odpowiedzi.")
        except Exception as e:
            print(f"[!] Błąd podczas generowania: {e}")
            self.client = None

        return ""

    async def close(self):
        if self.client:
            try:
                await self.client.close()
            except:
                pass
