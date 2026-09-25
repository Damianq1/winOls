#!/usr/bin/env bash
set -e

echo "=== [1/5] Czyszczenie konfliktów Gradle (KTS vs Groovy) ==="
rm -f build.gradle.kts settings.gradle.kts app/build.gradle.kts

echo "=== [2/5] Tworzenie struktury katalogów aplikacji ==="
mkdir -p app/src/main/kotlin/com/winols/app/model
mkdir -p app/src/main/kotlin/com/winols/app/ui
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/layout

echo "=== [3/5] Generowanie zasobów (Theme & Strings) ==="
cat << 'EOF' > app/src/main/res/values/themes.xml
<resources>
    <style name="Theme.WinOls" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">#1A1A1A</item>
        <item name="colorPrimaryVariant">#000000</item>
        <item name="colorOnPrimary">#00FF66</item>
    </style>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">WinOls Mobile AI</string>
</resources>
EOF

cat << 'EOF' > app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#121212"
    android:padding="16dp">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="WinOLS Mobile - ECU Hex Editor"
        android:textColor="#00FF66"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tvHexDump"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="12dp"
        android:fontFamily="monospace"
        android:textColor="#CCCCCC"
        android:textSize="12sp" />

    <Button
        android:id="@+id/btnLoadDummy"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Wczytaj przykładowy wsad ECU" />
</LinearLayout>
EOF

echo "=== [4/5] Generowanie kodu Kotlin (Parser + MainActivity) ==="
cat << 'EOF' > app/src/main/kotlin/com/winols/app/model/EcuBinary.kt
package com.winols.app.model

class EcuBinary(val data: ByteArray) {
    val size: Int get() = data.size

    fun readByte(offset: Int): Byte = data[offset]

    fun readWord(offset: Int, littleEndian: Boolean = false): Int {
        val b1 = data[offset].toInt() and 0xFF
        val b2 = data[offset + 1].toInt() and 0xFF
        return if (littleEndian) (b2 shl 8) or b1 else (b1 shl 8) or b2
    }

    fun getHexDump(startOffset: Int = 0, length: Int = 256): String {
        val end = (startOffset + length).coerceAtMost(data.size)
        val sb = StringBuilder()
        for (i in startOffset until end step 16) {
            sb.append(String.format("%06X: ", i))
            val lineEnd = (i + 16).coerceAtMost(end)
            for (j in i until lineEnd) {
                sb.append(String.format("%02X ", data[j]))
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
EOF

cat << 'EOF' > app/src/main/kotlin/com/winols/app/MainActivity.kt
package com.winols.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.model.EcuBinary

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoadDummy.setOnClickListener {
            // Przykładowy blok danych ECU (256 bajtów)
            val dummyBytes = ByteArray(256) { it.toByte() }
            val ecu = EcuBinary(dummyBytes)
            binding.tvHexDump.text = ecu.getHexDump(0, 256)
        }
    }
}
EOF

echo "=== [5/5] Uruchamianie kompilacji weryfikacyjnej ==="
chmod +x gradlew || true
./gradlew assembleDebug