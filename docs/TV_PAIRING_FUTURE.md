# Vantafyn TV Mobile Pairing Roadmap & Status

## 1. Active Implementation (Local-Network Pairing)
Vantafyn now includes a fully functional, zero-cloud local network pairing channel:
- **Android TV**: Runs an ephemeral `HttpServer` (`port 8765`) + UDP Discovery Beacon (`port 8766`).
- **Mobile**: Scans LAN via UDP and sends session payload via HTTP POST with 6-character single-use code verification.
- See complete architecture in [`docs/TV_MOBILE_PAIRING_ARCHITECTURE.md`](file:///home/glowseed/Documents/coding%20projects/Vantafyn/docs/TV_MOBILE_PAIRING_ARCHITECTURE.md).

---

## 2. Future Enhancements

### A. Mobile Camera QR Code Scanning
- Android TV displays a dynamic QR code alongside the 6-character code.
- Vantafyn Mobile includes a built-in camera barcode scanner to automatically parse the code and TV IP, eliminating any manual typing.

### B. Remote / Cloud Companion Relay
- For setups where TV and mobile are on isolated VLANs or WAN without LAN connectivity:
- A Jellyfin companion plugin or lightweight relay negotiates ephemeral ECDH keys and proxies the encrypted payload over WebSockets.

### C. Multi-Profile Bulk Provisioning
- Allows pairing multiple profiles in a single operation, automatically provisioning family accounts onto the shared living room TV.
