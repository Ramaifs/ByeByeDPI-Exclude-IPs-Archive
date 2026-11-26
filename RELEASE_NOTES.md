# ByeByeDPI 1.8.2 — IP Exceptions

Обновление на базе [ByeByeDPI](https://github.com/romanvht/ByeByeDPI) добавляет управление исключениями маршрутизации в режиме VPN.

- Откройте **Настройки → Исключения IP-адресов**. Можно добавлять, редактировать, отключать и удалять IPv4, IPv6 и CIDR. Изменения применяются после переподключения VPN.
- По умолчанию включены `192.168.0.0/24` и `224.0.0.251/32` для совместимости с прежней конфигурацией LAN Bypass. В меню доступны пресеты VRChat OSC Trackers, Private Networks, mDNS Discovery, SSDP Discovery и Local IPv6.
- Исключения сохраняются и входят в экспорт настроек. Маршрутизация через исключения поддерживается с Android 13 (API 33). Для исключений IPv6 включите IPv6 в настройках VPN.
- Для Meta Quest 3S подходит APK `arm64-v8a`. Сборка и 9 модульных тестов прошли. Проверка нового интерфейса и маршрутизации на гарнитуре остаётся открытой.

## Build from source

Clone with submodules. With JDK 21 and Android SDK 36 configured, run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Debug APKs are written to `app/build/outputs/apk/debug/`.
