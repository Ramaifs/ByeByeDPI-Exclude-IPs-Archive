<div align="center">
  <p><img src=".github/images/app.svg" alt="ByeDPI" width="200" /></p>
  <h1>ByeByeDPI — IP Exceptions</h1>
  <p>Русский | <a href="README-en.md">English</a></p>
  <p><a href="https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private/releases">Releases</a> | <a href="LICENSE">License</a></p>
</div>

Android-приложение на базе [ByeByeDPI 1.8.2](https://github.com/romanvht/ByeByeDPI), которое добавляет исключения по IP-адресам.

Трафик к выбранным адресам и подсетям может идти напрямую, минуя VPN-туннель ByeByeDPI.

P.S: Изначально репозиторий рассматривается для подключения VRChat OSC Trackers, но вполне может использоваться для других целей

### Возможности

* Исключения для IPv4, IPv6 и подсетей CIDR.
* Общий переключатель и отдельный переключатель для каждой записи.
* Пресеты для VRChat OSC Trackers, частных сетей, mDNS, SSDP и локального IPv6.
* Сохранение исключений и перенос через импорт и экспорт настроек.

### Использование

1. Скачайте APK в [Releases](https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private/releases). (Для Meta Quest выберите `arm64-v8a`)
2. В режиме VPN откройте **Настройки → Исключения IP-адресов**. Добавьте адрес или выберите пресет.
3. Отключите и снова подключите VPN. Исключения применяются с Android 13 (API 33). Для исключений IPv6 включите IPv6 в настройках VPN.

### Начальные исключения

Обе записи включены при первом запуске, чтобы сохранить прежнюю конфигурацию для VRChat и OSC-трекеров.

* **Local Network** - `192.168.0.0/24`. Позволяет обращаться напрямую к устройствам в этой подсети, например к компьютеру или трекеру. Если у вашей сети другая подсеть, измените адрес записи.
* **mDNS Discovery** - `224.0.0.251/32`. Это multicast-адрес IPv4 для обнаружения устройств и сервисов в локальной сети через mDNS. Запись исключает только этот адрес назначения.

Обе записи можно отключить, изменить или удалить.

### Сборка

Для сборки используйте Linux или WSL, JDK 21 и Android SDK 36.

```bash
git clone --recurse-submodules https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private.git
cd ByeByeDPI-Exclude-IPs-Private
./gradlew testDebugUnitTest assembleDebug
```

Готовые APK появятся в `app/build/outputs/apk/debug/`.

### Зависимости
- [ByeDPI](https://github.com/hufrea/byedpi)
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)

### Благодарность
- [hufrea](https://github.com/hufrea) - за [ByeDPI](https://github.com/hufrea/byedpi)
- [dovecoteescapee](https://github.com/dovecoteescapee) - за изначальную реализацию [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid)
