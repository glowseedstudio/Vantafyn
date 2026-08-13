# Jellyfin Statistics Audit

## Source Of Truth

Vantafyn does not fabricate watch totals. Jellyfin core provides live sessions, users, activity log entries, scheduled tasks, plugins, devices, logs, and item counts, but detailed historical watch-time totals require an external reporting source.

The first supported reporting source is the Jellyfin Playback Reporting plugin.

## Playback Reporting Endpoints Used

The plugin exposes its reporting API under `user_usage_stats`.

- `GET /user_usage_stats/user_activity`
  - Used for per-user watch time, play count, latest watched item, last seen time, and client.
  - Query parameters: `days`, `endDate`, `timezoneOffset`.
- `GET /user_usage_stats/PlayActivity`
  - Used for date-bucketed watch-time trend data.
  - Query parameters: `filter`, `days`, `endDate`, `dataType=time`, `timezoneOffset`.
- `GET /user_usage_stats/type_filter_list`
  - Used to ask the plugin which playback item types it can report.
- `GET /user_usage_stats/ItemName/BreakdownReport`
  - Used best-effort for most-watched title breakdowns.

References:

- https://deepwiki.com/jellyfin/jellyfin-plugin-playbackreporting/3.1-user-report
- https://deepwiki.com/jellyfin/jellyfin-plugin-playbackreporting/3.2-playback-report
- https://raw.githubusercontent.com/jellyfin/jellyfin-plugin-playbackreporting/master/Jellyfin.Plugin.PlaybackReporting/Api/PlaybackReportingActivityController.cs

## Current Vantafyn Coverage

- Admin-only statistics entry in the mobile admin dashboard.
- Real plugin-backed watch time and play counts when Playback Reporting responds.
- Premium summary cards for watch time, plays, top user, and top title.
- 14-bucket watch-time trend chart.
- Top viewers leaderboard with Jellyfin profile images where available.
- Most-watched title list when the plugin breakdown endpoint returns compatible rows.
- Recent user activity based on plugin user activity rows.
- Core Jellyfin fallback when plugin data is unavailable.

## Limitations

- The Playback Reporting plugin does not consistently expose poster image IDs in every breakdown response. Vantafyn therefore only shows poster art when a future endpoint or row shape provides a safe Jellyfin item ID.
- The current UI uses a 30-day range. Additional range controls can be added later without changing the core model.
- If the plugin is installed but has no collected data yet, Vantafyn shows a clear unavailable/collecting state instead of zero-filled fake analytics.
