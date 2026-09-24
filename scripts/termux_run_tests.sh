#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== [WinOLS-Mobile] Weryfikacja środowiska Termux ==="

# Weryfikacja OpenJDK i Gradle
if ! command -v java > /dev/null 2>&1; then
    echo "[!] Brak Java. Instalowanie OpenJDK 17..."
    pkg update -y && pkg install -y openjdk-17
fi

if ! command -v gradle > /dev/null 2>&1; then
    echo "[!] Brak systemowego Gradle. Używanie gradlew lub instalacja..."
    if [ ! -f "./gradlew" ]; then
        pkg install -y gradle
    fi
fi

# Nadanie uprawnień wykonywalnych dla wrappera Gradle
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE_BIN="./gradlew"
else
    GRADLE_BIN="gradle"
fi

echo "[*] Uruchamianie testów silnika (CoreEngineTest)..."
$GRADLE_BIN test --no-daemon -Dorg.gradle.jvmargs="-Xmx512m -XX:+UseSerialGC"

echo "=== [WinOLS-Mobile] Testy zakończone sukcesem ==="