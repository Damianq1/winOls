# WinOls Mobile

Mobilny edytor map ECU dla Androida, pakiet `com.winols.app`.

## Funkcje
- BIN i Intel HEX (odczyt/zapis)
- direct `java.nio.ByteBuffer` dla bufora roboczego
- 8/16/32-bit, signed/unsigned, Little/Big Endian
- RAW → fizyczne: `RAW * Factor + Offset`
- heurystyczne wykrywanie potencjalnych map z oceną confidence
- masowa edycja: +, -, %, wartość stała, smoothing
- undo
- podgląd HEX/tabelaryczny/2D/3D
- CSV oraz XLSX bez dodatkowej biblioteki arkuszowej
- ADD8, ADD16 LE/BE, CRC16-CCITT i CRC32
- operacje I/O i skanowanie poza głównym wątkiem przez Coroutines
- GitHub Actions budujące debug APK

## Budowanie

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

Jeżeli system nie ma jeszcze Gradle'a, `gradlew` pobierze Gradle 8.2 z oficjalnego serwera.

W GitHub Actions używany jest Gradle 8.2 oraz JDK 17. APK znajduje się w `app/build/outputs/apk/debug/`.

## Ważne
Automatyczny MapFinder jest heurystycznym detektorem kandydatów. Nie jest bazą definicji konkretnych sterowników ECU i nie gwarantuje poprawnego rozpoznania każdej mapy. Przed zapisaniem modyfikowanego pliku należy zweryfikować mapę, typ danych, skalowanie, adresację i właściwy algorytm checksum dla konkretnego ECU.
