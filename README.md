# WinOls Android — Version 2.0

Mobilny edytor plików binarnych ECU dla Androida. Projekt zawiera kod aplikacji, silniki analizy/edycji, zasoby UI, testy jednostkowe i workflow GitHub Actions.

## Budowanie APK w GitHub
1. Wypakuj zawartość archiwum.
2. Wgraj zawartość folderu `WinOls_V2` do głównego katalogu repozytorium.
3. Otwórz **Actions** i uruchom workflow **Android APK** albo wykonaj push do `main`.
4. Pobierz artefakt `WinOls-v2-debug-apk`.

## Budowanie lokalne
Wymagane JDK 17, Android SDK platform 35, Build Tools 35.0.0 i Gradle 8.7+:
`gradle :app:assembleDebug`
`gradle :app:testDebugUnitTest`

## Zakres i ograniczenia
Aplikacja udostępnia funkcje edycji/analizy plików BIN/HEX zaimplementowane w kodzie źródłowym projektu, w tym widoki i narzędzia obecne w module app. To nie jest certyfikowany zamiennik komercyjnego WinOLS. Algorytmy checksum są zależne od ECU; przed zapisem do sterownika należy zweryfikować wynik na kopii i użyć właściwej definicji dla konkretnego sterownika. Automatyczne wykrywanie map ma charakter heurystyczny.
