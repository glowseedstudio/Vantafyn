# Vantafyn Android TV Requests & Ombi Integration

## 1. Overview
The Requests sidebar route on Android TV provides access to media requests powered by Jellyfin server integrations (such as Ombi or Jellyseerr).

---

## 2. Feature Gating & Visibility
- **Sidebar Visibility**: The `Requests` tab (`Icons.AutoMirrored.Rounded.Send`) appears in the expanding TV sidebar when Ombi/Requests integration is enabled on the server (`state.ombiConfigured == true`).
- **Standard User Safety**:
  - Normal members can browse and request approved media.
  - Normal members are **never** exposed to administrative configuration, API keys, host URLs, or backend health checks.
- **Unconfigured State**:
  - If a user navigates to Requests without active configuration, a clean 10-foot notice is displayed:
    *"Requests need setup. Use the Vantafyn mobile app to connect or manage Ombi."*

---

## 3. Future 10-Foot Roadmap
A future release will bring full 10-foot request browsing, trending request carousels, and quick voice-assisted request submission.
