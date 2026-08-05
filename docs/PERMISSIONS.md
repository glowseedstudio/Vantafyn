# Permissions

Vantafyn treats permissions as part of the user experience. Runtime permissions must be explained before the Android system dialog appears, must be optional unless the feature cannot work without them, and must have a clear denied state.

## Notifications

- Android permission: `android.permission.POST_NOTIFICATIONS`
- Android versions: runtime permission on Android 13+; not requested on Android 12 and below
- Optional: yes
- Why Vantafyn asks: to show music playback controls in notifications and on the lock screen
- When Vantafyn asks: when the user first starts or resumes mobile music playback, or when the user opens Settings > Permissions and chooses the music controls row
- User-facing explanation: “Vantafyn uses notifications to keep music playing when your phone is locked and to show play, pause, next, and previous controls.”
- Privacy wording: “Vantafyn only uses this notification permission for media playback controls. It does not use notifications for ads or tracking.”

If granted:

- Music can continue in the background.
- The system media notification can appear.
- Lock-screen controls can appear when supported by the OS/device.

If denied or dismissed:

- Music can still play in the app.
- Notification and lock-screen controls may not appear.
- Vantafyn shows a clean explanation instead of silently pretending controls are available.
- The app does not repeatedly prompt after the user chooses `Not now`.

If permanently denied:

- Vantafyn shows the permission as `Not allowed`.
- Settings provides an action that opens Android notification settings for the app.

## Foreground Media Playback

- Android permissions: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- Optional: no runtime prompt; required by Android for the Media3 music playback service on modern target SDKs
- Why Vantafyn declares them: to run the foreground media playback service used by background/lock-screen music controls
- When Vantafyn uses them: only while music playback is active through the Media3 music service
- If unavailable: Android may prevent background media playback service behavior

The music playback notification channel is:

- name: `Music playback`
- description: `Playback controls for music playing in Vantafyn`
- importance: low
- sound/vibration: disabled

## Network

- Android permissions: `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`
- Optional: no runtime prompt
- Why Vantafyn uses them: Jellyfin login, library browsing, artwork loading, playback streams, and connection status
- Denied state: Android does not expose these as runtime user prompts. If network access is unavailable, Vantafyn shows connection or playback errors in the relevant screen.

## Not Requested

Vantafyn does not request camera, microphone, location, contacts, or storage permissions. Local network access is not an Android runtime permission, so Vantafyn does not show a fake permission for it.
