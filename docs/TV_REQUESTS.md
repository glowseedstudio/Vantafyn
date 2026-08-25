# Vantafyn Android TV Requests & Ombi Integration

**Document**: `docs/TV_REQUESTS.md`  
**Status**: Active  

---

## 1. Overview
The Requests sidebar route on Android TV provides access to media discovery and requests powered by server integrations (Ombi and future Jellyseerr adapters).

---

## 2. Feature Gating & Visibility
- **Sidebar Visibility**: The `Requests` route (`Icons.AutoMirrored.Rounded.Send`) appears in the main scrollable section of the expanding TV sidebar when requests are configured or enabled for users (`state.ombiConfigured == true || state.ombiRequestsEnabledForUsers == true || isAdmin == true`).
- **Standard User Safety**:
  - Normal members can view requests and request status.
  - Normal members are **never** exposed to administrative configuration, API keys, host URLs, or backend health credentials.
- **TV Route State**:
  - If Ombi is configured: displays an honest 10-foot request discovery placeholder informing the user of active request sync.
  - If Ombi is unconfigured: displays a helpful prompt instructing the user to configure Ombi via the Vantafyn mobile app.

---

## 3. 10-Foot Roadmap
A future release will introduce full 10-foot D-pad request browsing, trending carousels, and instant request submission.
