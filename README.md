# BLE Device Manager (Android Java + NDK C++) — multi-peripheral telemetry demo

An Android app that scans for BLE peripherals, connects to multiple devices, subscribes to a telemetry characteristic,
parses packets in a shared C++ library (also used by a desktop tester), and logs RSSI + latency statistics.

## Features
- Scan + connect to multiple BLE peripherals
- GATT service discovery and notification subscription
- Shared **C++ packet parsing + CRC16** library used by:
  - Android app via JNI
  - Desktop tester (CLI)
- Periodic RSSI polling per connected device
- Simple latency estimate from device timestamp embedded in packets
- Export logs to CSV in app storage

## Packet format (demo)
This repo uses a simple packet framing used by the parser:
```
[0]    type (u8)
[1]    seq  (u8)
[2..5] device_timestamp_ms (u32 LE)
[6..7] payload_len (u16 LE)
[8..]  payload bytes (payload_len)
[last-2..last-1] CRC16-CCITT (u16 LE) over all prior bytes
```
If your peripherals use a different format, update `shared/ble_parser/packet.{h,cpp}`.

## Build (Android)
1. Open the project folder in **Android Studio**
2. Let it sync Gradle
3. Build / Run on a device (Android 8+ recommended)

### Permissions
Android 12+ needs runtime permissions:
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
Older devices use location permission for scanning.

## Desktop tester
Build with CMake (Windows/Linux/macOS):
```bash
cmake -S desktop_tester -B desktop_tester/build
cmake --build desktop_tester/build --config Release
./desktop_tester/build/ble_tester --help
```

Test a hex packet:
```bash
echo "01 10 78 56 34 12 03 00 AA BB CC 1F 2A" | ./desktop_tester/build/ble_tester --hex-stdin
```

## License
MIT — see `LICENSE`.
