import asyncio
from gemini_webapi import GeminiClient

class GeminiConnector:
    def __init__(self, psid: str, psidts: str):
        self.psid = psid
        self.psidts = psidts
        self.client = None
        self.chat = None

    async def initialize(self) -> bool:
        try:
            self.client = GeminiClient(self.psid, self.psidts)
            await self.client.init(timeout=60, auto_close=False, close_delay=300, auto_refresh=True)
            self.chat = self.client.start_chat()
            return True
        except Exception as e:
            print(f"[!] Błąd inicjalizacji: {e}")
            return False

    async def send_prompt(self, prompt: str, timeout_sec: int = 60) -> str:
        for attempt in range(1, 5):
            try:
                if not self.chat:
                    self.chat = self.client.start_chat()
                
                response = await asyncio.wait_for(
                    self.chat.send_message(prompt), 
                    timeout=timeout_sec
                )
                
                text = getattr(response, "text", None)
                if not text and isinstance(response, str):
                    text = response
                    
                if text:
                    return text
                print(f"[!] Pusta odpowiedź (próba {attempt}/4)...")
            
            except asyncio.TimeoutError:
                print(f"[!] Timeout (> {timeout_sec}s) - próba {attempt}/4.")
            except Exception as e:
                print(f"[!] Błąd wysyłania (próba {attempt}/4): {e}")
            
            await asyncio.sleep(2)
        
        return ""

    async def close(self):
        if self.client:
            try:
                await self.client.close()
            except:
                pass
