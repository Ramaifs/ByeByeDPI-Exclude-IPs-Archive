<div align="center">
  <p><img src=".github/images/app.svg" alt="ByeDPI" width="200" /></p>
  <h1>ByeByeDPI — IP Exceptions</h1>
  <p><a href="README.md">Русский</a> | English</p>
  <p><a href="https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private/releases">Releases</a> | <a href="LICENSE">License</a></p>
</div>

An Android app based on [ByeByeDPI 1.8.2](https://github.com/romanvht/ByeByeDPI) that adds IP exceptions. Traffic to selected addresses and subnets can go directly to its destination, bypassing the ByeByeDPI VPN tunnel.

### Features

* Exceptions for IPv4, IPv6, and CIDR subnets.
* A global switch and a separate switch for each entry.
* Presets for VRChat OSC Trackers, private networks, mDNS, SSDP, and local IPv6.
* Exceptions are saved and included in settings import and export.

### Usage

1. Download an APK from [Releases](https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private/releases). For Meta Quest 3S, choose `arm64-v8a`.
2. In VPN mode, open **Settings → IP Exceptions**. Add an address or choose a preset.
3. Disconnect and reconnect the VPN. Exceptions apply on Android 13 (API 33) or later. For IPv6 exceptions, enable IPv6 in the VPN settings.

### Default exceptions

Both entries are enabled on first launch to preserve the previous setup for VRChat and OSC trackers.

* **Local Network** — `192.168.0.0/24`. Allows direct access to devices on this subnet, such as a computer or tracker at `192.168.0.149`. Edit the entry if your network uses a different subnet.
* **mDNS Discovery** — `224.0.0.251/32`. This is the IPv4 multicast address used by mDNS to discover devices and services on the local network. The entry excludes only this destination address.

Both entries can be disabled, edited, or deleted.

This build passed unit tests. The new interface and routing have yet to be tested on a Meta Quest 3S.

### Building

Build on Linux or WSL with JDK 21 and Android SDK 36.

```bash
git clone --recurse-submodules https://github.com/Ramaifs/ByeByeDPI-Exclude-IPs-Private.git
cd ByeByeDPI-Exclude-IPs-Private
./gradlew testDebugUnitTest assembleDebug
```

The APKs are written to `app/build/outputs/apk/debug/`.

### Dependencies

* [ByeDPI](https://github.com/hufrea/byedpi)
* [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)

### Acknowledgements

* [hufrea](https://github.com/hufrea) — for [ByeDPI](https://github.com/hufrea/byedpi)
* [dovecoteescapee](https://github.com/dovecoteescapee) — for the original implementation of [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid)
