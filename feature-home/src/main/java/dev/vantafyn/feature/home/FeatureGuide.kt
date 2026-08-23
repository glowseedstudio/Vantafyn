package dev.vantafyn.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArtTrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

data class DiscoverFeature(
    val id: String,
    val title: String,
    val shortDescription: String,
    val category: DiscoverCategory,
    val icon: ImageVector,
    val detailedDescription: String = shortDescription,
    val steps: List<String> = emptyList(),
    val adminOnly: Boolean = false,
    val isNew: Boolean = false,
    val deepLinkAction: String? = null,
)

enum class DiscoverCategory(val label: String, val order: Int) {
    MakeItYours("Make it yours", 0),
    Social("Social & Friends", 1),
    Achievements("Achievements", 2),
    Watching("Watching", 3),
    Music("Music", 4),
    YourLibrary("Your library", 5),
    Requests("Requests", 6),
    AdminTools("Admin tools", 7),
}

fun discoverFeatureGuide(): List<DiscoverFeature> = listOf(
    DiscoverFeature(
        id = "themes",
        title = "Themes",
        shortDescription = "Six premium themes — Nebula, Midnight, Aurora, Amethyst, Ember, and OLED.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.AutoAwesome,
        detailedDescription = "Open Settings \u2192 Appearance to switch themes. Each theme smoothly animates the entire app\u2019s colors, glass surfaces, and gradients.",
        deepLinkAction = "open_settings",
    ),
    DiscoverFeature(
        id = "home_layout",
        title = "Customize your Home",
        shortDescription = "Reorder, resize and style your home rows to match how you browse.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.Widgets,
        detailedDescription = "Long-press the hero banner on Home and choose Customize Home. Drag to reorder sections, toggle visibility, and adjust card display modes, artwork types, shapes, sizes and spacing per section.",
        steps = listOf("Hold hero banner", "Tap Customize Home", "Drag to reorder", "Tap a section to adjust its style"),
        deepLinkAction = "open_home_layout",
    ),
    DiscoverFeature(
        id = "smart_rows",
        title = "Smart Rows",
        shortDescription = "Smart queries like New in Crime, Highly Rated, and Unwatched \u2014 personalized for you.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.Bolt,
        detailedDescription = "Smart Rows use custom queries to surface content based on genres, ratings, unwatched status and release dates. Enable and reorder them in Customize Home.",
    ),
    DiscoverFeature(
        id = "profile_images",
        title = "Profile Pictures",
        shortDescription = "Upload a profile image to personalize your experience.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.Person,
        detailedDescription = "Open Settings \u2192 tap your profile card \u2192 upload or delete your profile image.",
        deepLinkAction = "open_settings",
    ),
    DiscoverFeature(
        id = "soundscapes",
        title = "Soundscapes",
        shortDescription = "Subtle tactile sound effects for chat, alerts, and achievements.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.VolumeUp,
        isNew = true,
        detailedDescription = "Enjoy custom ambient audio feedback when sending messages, receiving invites, unlocking achievements, or navigating menus. Toggle on or off in Settings \u2192 Appearance \u2192 Soundscapes.",
        deepLinkAction = "open_settings",
    ),
    DiscoverFeature(
        id = "backgrounds",
        title = "App Backgrounds",
        shortDescription = "Five distinct backgrounds \u2014 Nebula, Glass, Twilight, Aurora, and Deep Space.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.Flight,
        detailedDescription = "Open Settings \u2192 Appearance \u2192 Background to choose the backdrop behind your glass UI. Each background subtly animates for a living feel.",
        deepLinkAction = "open_settings",
    ),
    DiscoverFeature(
        id = "bottom_rail",
        title = "Bottom Rail Accent",
        shortDescription = "Add a glow, breathing effect, or touch ripple to your bottom navigation.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.FiberManualRecord,
        detailedDescription = "Open Settings \u2192 Appearance \u2192 Bottom Rail Accent. Choose Off, Still Glow, Breathing, or Touch Ripple.",
        deepLinkAction = "open_settings",
    ),
    DiscoverFeature(
        id = "theme_music",
        title = "Theme Music",
        shortDescription = "Optional background music while browsing Home \u2014 with volume control.",
        category = DiscoverCategory.MakeItYours,
        icon = Icons.Rounded.MusicNote,
        detailedDescription = "Open Settings \u2192 Appearance \u2192 Theme Music. Toggle on/off and choose volume: Soft, Medium, High, or Full.",
        deepLinkAction = "open_settings",
    ),

    // ----------------------------------------------------
    // Social & Friends
    // ----------------------------------------------------
    DiscoverFeature(
        id = "social_hub",
        title = "Social Hub",
        shortDescription = "Real-time 1:1 chat with friends, typing sounds, and emoji reactions.",
        category = DiscoverCategory.Social,
        icon = Icons.Rounded.Forum,
        isNew = true,
        detailedDescription = "Chat with server friends in real-time, react to messages with animated emoji quick-bars, and manage conversation threads with live presence indicators.",
        deepLinkAction = "open_social",
    ),
    DiscoverFeature(
        id = "floating_social_dock",
        title = "Floating Dock",
        shortDescription = "Draggable chat bubble with live unread counts and alerts.",
        category = DiscoverCategory.Social,
        icon = Icons.Rounded.NotificationsActive,
        isNew = true,
        detailedDescription = "The floating dock stays accessible as you browse movies, shows, and music. Drag anywhere on screen to reposition, tap to expand your social panel, or drag down to the dismiss target to hide it.",
        steps = listOf("Tap bubble to open social panel", "Drag to reposition anywhere", "Drag down to 'X' to hide bubble"),
        deepLinkAction = "open_social",
    ),
    DiscoverFeature(
        id = "media_recommendations",
        title = "Media Sharing",
        shortDescription = "Share movies, TV series, episodes and albums directly into friend chats.",
        category = DiscoverCategory.Social,
        icon = Icons.Rounded.Recommend,
        isNew = true,
        detailedDescription = "Tap the Share icon in chat or long-press any media item to send a recommendation. Friends receive rich, interactive glass cards with poster artwork, year, and instant playback shortcuts.",
        deepLinkAction = "open_social",
    ),
    DiscoverFeature(
        id = "online_presence",
        title = "Live Presence",
        shortDescription = "See who is online and what titles they are currently watching.",
        category = DiscoverCategory.Social,
        icon = Icons.Rounded.PersonAdd,
        isNew = true,
        detailedDescription = "The Add Friend tab features an Online Users rail showing real-time presence, device info, and marquee now-playing titles across all server members.",
        deepLinkAction = "open_social",
    ),
    DiscoverFeature(
        id = "friend_safety_actions",
        title = "Friend Actions",
        shortDescription = "Star favorites, cancel requests with touch & hold, and manage privacy.",
        category = DiscoverCategory.Social,
        icon = Icons.Rounded.Shield,
        isNew = true,
        detailedDescription = "Long-press any friend or conversation to Star, Remove, or Block. Long-press pending outgoing requests to cancel invites instantly without leaving the screen.",
        deepLinkAction = "open_social",
    ),

    // ----------------------------------------------------
    // Achievements
    // ----------------------------------------------------
    DiscoverFeature(
        id = "achievement_badges",
        title = "Achievements",
        shortDescription = "Unlock achievements and earn trophies as you watch and listen.",
        category = DiscoverCategory.Achievements,
        icon = Icons.Rounded.EmojiEvents,
        isNew = true,
        detailedDescription = "Track your watch milestones, genres explored, watch streaks, and audio sessions. Unlock bronze, silver, gold, and platinum badges powered by the Jellyfin AchievementBadges plugin.",
        deepLinkAction = "open_achievements",
    ),
    DiscoverFeature(
        id = "leaderboard_ranks",
        title = "Leaderboard",
        shortDescription = "Climb the server ranks and compare your total score with friends.",
        category = DiscoverCategory.Achievements,
        icon = Icons.Rounded.Leaderboard,
        isNew = true,
        detailedDescription = "Level up from Rookie to Grandmaster. View the server-wide leaderboard, earn tier badges, and showcase your achievements on your profile.",
        deepLinkAction = "open_achievements",
    ),

    // ----------------------------------------------------
    // Watching
    // ----------------------------------------------------
    DiscoverFeature(
        id = "dropdown_gestures",
        title = "Dropdown Alerts",
        shortDescription = "Glassmorphic top banners with 5-second auto-dismiss and swipe-to-dismiss.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.TouchApp,
        isNew = true,
        detailedDescription = "Incoming messages, friend requests, watch party invites, and achievement unlocks drop down elegantly from the top. Swipe up on any banner to dismiss it early while keeping notification badges intact.",
    ),
    DiscoverFeature(
        id = "playback",
        title = "Built-in Player",
        shortDescription = "Full-featured video player with direct play, transcoding, and gesture controls.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.PlayArrow,
        detailedDescription = "Vantafyn uses Media3 ExoPlayer for reliable playback with direct play, direct stream, and transcoding. Supports resume, progress reporting, and multiple playback methods.",
    ),
    DiscoverFeature(
        id = "popup_player",
        title = "Popup Player",
        shortDescription = "Keep watching while you browse other parts of Vantafyn.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.PictureInPicture,
        detailedDescription = "During video playback, tap the picture-in-picture button to minimize the player into a floating window. Continue browsing while your video plays.",
    ),
    DiscoverFeature(
        id = "bitrate",
        title = "Streaming Bitrate",
        shortDescription = "Auto or manually cap quality from 5 to 120 Mbps.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Speed,
        detailedDescription = "Open Settings \u2192 Playback Preferences \u2192 Max Streaming Bitrate. Auto lets the server decide. Manual options range from 5 Mbps (data saver) to 120 Mbps.",
        deepLinkAction = "open_playback_preferences",
    ),
    DiscoverFeature(
        id = "subtitles",
        title = "Subtitles & Audio Tracks",
        shortDescription = "Switch subtitles and audio tracks on-the-fly during playback.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Subtitles,
        detailedDescription = "Tap the subtitle or audio track icons in the player controls. Supports embedded subtitles, external SRT/ASS, multiple audio languages, and surround sound.",
    ),
    DiscoverFeature(
        id = "hdr",
        title = "4K & HDR Playback",
        shortDescription = "Direct play 4K, HDR10, HDR10+, and Dolby Vision where supported.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Hd,
        detailedDescription = "Vantafyn advertises HEVC, AV1, VP9 and other modern codecs for direct play. When transcoding is needed, HEVC is preferred to preserve HDR metadata.",
    ),
    DiscoverFeature(
        id = "watch_party",
        title = "Watch Party",
        shortDescription = "SyncPlay with friends \u2014 watch together in real-time.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Groups,
        detailedDescription = "Create a Watch Party from Settings to start a SyncPlay group. Invite friends, use Swipe to Match to find something everyone wants, or pre-select a title.",
        deepLinkAction = "open_watch_party",
    ),
    DiscoverFeature(
        id = "swipe_to_match",
        title = "Swipe to Match",
        shortDescription = "A fun voting system to find the perfect Watch Party title.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Swipe,
        detailedDescription = "In Watch Party Swipe to Match mode, candidates appear one at a time. Swipe right to vote yes, left to pass. When the group agrees, everyone watches together.",
    ),
    DiscoverFeature(
        id = "skip_segments",
        title = "Skip Intro & Recap",
        shortDescription = "Auto-detect and skip intros, recaps, and outros.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Animation,
        detailedDescription = "With Jellyfin Media Segments enabled, Vantafyn detects intro, recap, outro, and commercial segments. Choose per type: Prompt, Auto-Skip, or Do Nothing.",
        deepLinkAction = "open_playback_preferences",
    ),
    DiscoverFeature(
        id = "up_next",
        title = "Up Next & Autoplay",
        shortDescription = "Seamless episode autoplay with a configurable countdown.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.QueueMusic,
        detailedDescription = "After an episode ends, Vantafyn shows the next one with a countdown. Configure the timer (5\u201330 seconds) and display mode in Playback Preferences.",
        deepLinkAction = "open_playback_preferences",
    ),
    DiscoverFeature(
        id = "cast",
        title = "Google Cast",
        shortDescription = "Cast video and music to Chromecast with subtitle and quality controls.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Cast,
        detailedDescription = "Tap the Cast icon in the player to discover Chromecast devices. Cast movies, episodes, live TV, and music queues with full subtitle and audio track support.",
    ),
    DiscoverFeature(
        id = "live_tv",
        title = "Live TV",
        shortDescription = "Browse and stream live TV channels from your Jellyfin server.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Tv,
        detailedDescription = "If your Jellyfin server has Live TV configured, channels appear on Home and in Libraries. Browse programs and stream live channels.",
    ),
    DiscoverFeature(
        id = "passout",
        title = "Passout Protection",
        shortDescription = "Stop playback after a set time to prevent binge-watching fatigue.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.Animation,
        detailedDescription = "Open Settings \u2192 Playback Preferences \u2192 Passout Protection. Set a limit (60\u2013300 minutes) and Vantafyn stops playback when reached.",
        deepLinkAction = "open_playback_preferences",
    ),
    DiscoverFeature(
        id = "external_player",
        title = "External Player",
        shortDescription = "Hand off the stream URL to your preferred video app.",
        category = DiscoverCategory.Watching,
        icon = Icons.Rounded.OndemandVideo,
        detailedDescription = "Open Settings \u2192 Playback Preferences \u2192 Video Player. Switch to External Player to use apps like MX Player or VLC.",
        deepLinkAction = "open_playback_preferences",
    ),
    DiscoverFeature(
        id = "music_player",
        title = "Music Player",
        shortDescription = "Full-featured player with artwork, queue controls, and background playback.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.Headphones,
        detailedDescription = "Tap any track to start the music player. Enjoy full-screen artwork, lock-screen controls, notification shade controls, and seamless background playback.",
    ),
    DiscoverFeature(
        id = "lyrics",
        title = "Synced Lyrics",
        shortDescription = "Follow along with time-synced lyrics that highlight in real-time.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.Lyrics,
        detailedDescription = "While music is playing, tap Lyrics to open the lyrics overlay. Synced lyrics highlight line-by-line. Falls back to plain-text when synced data isn\u2019t available.",
    ),
    DiscoverFeature(
        id = "playlists",
        title = "Playlists",
        shortDescription = "Create, manage and play playlists from your Jellyfin server.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.QueueMusic,
        detailedDescription = "Browse playlists in the Music section. Create new playlists, add or remove tracks, and play entire playlists with queue management.",
    ),
    DiscoverFeature(
        id = "music_downloads",
        title = "Offline Music",
        shortDescription = "Download songs, albums and playlists for offline listening.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.LibraryMusic,
        detailedDescription = "Long-press any track, album, or playlist and choose Download. Downloaded music plays without a network connection. Lyrics and artwork are saved alongside.",
    ),
    DiscoverFeature(
        id = "android_auto",
        title = "Android Auto",
        shortDescription = "Browse and control music from your car\u2019s display.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.Apps,
        detailedDescription = "Vantafyn integrates with Android Auto for in-car music browsing. Browse albums, artists, playlists, and search from your car\u2019s display.",
    ),
    DiscoverFeature(
        id = "music_widget",
        title = "Home Screen Widget",
        shortDescription = "Control music playback from your Android home screen.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.ArtTrack,
        detailedDescription = "Add the Vantafyn Music Widget to your home screen. See current track artwork, title, and artist with play/pause and skip controls.",
    ),
    DiscoverFeature(
        id = "music_popup_player",
        title = "Music Popup Player",
        shortDescription = "Long-press the Music icon on any screen for a quick player popup.",
        category = DiscoverCategory.Music,
        icon = Icons.Rounded.QueueMusic,
        detailedDescription = "On any screen except Music, long-press the Music icon in the bottom navigation bar. A quick player sheet pops up showing the current track with playback controls. Tap the icon normally to go to the full Music screen.",
    ),
    DiscoverFeature(
        id = "library_browse",
        title = "Library Browsing",
        shortDescription = "Browse all your Jellyfin libraries with filters, pagination, and view modes.",
        category = DiscoverCategory.YourLibrary,
        icon = Icons.Rounded.CollectionsBookmark,
        detailedDescription = "Tap Libraries in the bottom nav. Toggle between grid and list views, switch between poster/landscape/thumbnail cards, and filter by Recently Added, A\u2013Z, Favorites, or Unwatched.",
    ),
    DiscoverFeature(
        id = "search",
        title = "Search",
        shortDescription = "Search across all your Jellyfin libraries \u2014 movies, TV, music and more.",
        category = DiscoverCategory.YourLibrary,
        icon = Icons.Rounded.Search,
        detailedDescription = "Tap Search in the bottom navigation to search across all content types. Results show movies, TV shows, music, and people with rich artwork cards.",
    ),
    DiscoverFeature(
        id = "favorites",
        title = "My List",
        shortDescription = "Your personal collection of favorited movies, shows and music.",
        category = DiscoverCategory.YourLibrary,
        icon = Icons.Rounded.Favorite,
        detailedDescription = "Tap My List in the bottom navigation to see all items you\u2019ve marked as favorites across every library.",
    ),
    DiscoverFeature(
        id = "downloads",
        title = "Downloads & Offline",
        shortDescription = "Save movies, episodes and music for offline playback.",
        category = DiscoverCategory.YourLibrary,
        icon = Icons.Rounded.Download,
        detailedDescription = "Long-press any movie, episode, or track and choose Download. Manage all downloads from the Downloads screen. WiFi-only mode available. Offline playback syncs when reconnected.",
    ),
    DiscoverFeature(
        id = "media_detail",
        title = "Media Details",
        shortDescription = "Rich detail pages with cast, related titles, seasons, and playback info.",
        category = DiscoverCategory.YourLibrary,
        icon = Icons.Rounded.Dashboard,
        detailedDescription = "Tap any item to see its full detail page: backdrop, poster, logo, year, runtime, rating, overview, genres, cast filmography, related titles, and media source info.",
    ),
    DiscoverFeature(
        id = "requests",
        title = "Content Requests",
        shortDescription = "Request movies and TV shows through Ombi integration.",
        category = DiscoverCategory.Requests,
        icon = Icons.Rounded.Send,
        detailedDescription = "If your server has Ombi configured, browse discovery rails, search for movies and TV, and submit requests. Track request status from Pending to Available.",
    ),
    DiscoverFeature(
        id = "request_discovery",
        title = "Discovery Rails",
        shortDescription = "Browse Popular, Now Playing, Upcoming, and Trending content to request.",
        category = DiscoverCategory.Requests,
        icon = Icons.Rounded.Dashboard,
        detailedDescription = "The Requests screen shows TMDB-powered browse rails: Popular Movies, Now Playing, Upcoming, Top-Rated, Trending Series, and more. Tap any item to request it.",
    ),
    DiscoverFeature(
        id = "admin_overview",
        title = "Server Overview",
        shortDescription = "See server stats, library counts, and connected sessions at a glance.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Dashboard,
        adminOnly = true,
        detailedDescription = "Open Settings \u2192 Admin to see server name, version, OS, library count, total items, movies/series/episodes/music counts, and connected sessions.",
        deepLinkAction = "open_admin",
    ),
    DiscoverFeature(
        id = "admin_sessions",
        title = "Active Sessions",
        shortDescription = "Monitor all connected sessions with live stream quality and transcode status.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Groups,
        adminOnly = true,
        detailedDescription = "View every connected session: user, device, client, now-playing info, stream quality, and whether content is being transcoded.",
        deepLinkAction = "open_admin",
    ),
    DiscoverFeature(
        id = "admin_users",
        title = "User Management",
        shortDescription = "Create users, manage policies, reset passwords, and upload profile images.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Person,
        adminOnly = true,
        detailedDescription = "Open Admin \u2192 Users to create new users, edit admin status, manage library access, reset passwords, and upload profile images.",
        deepLinkAction = "open_admin",
    ),
    DiscoverFeature(
        id = "admin_plugins",
        title = "Plugin Management",
        shortDescription = "View and toggle Jellyfin plugins from within Vantafyn.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Apps,
        adminOnly = true,
        detailedDescription = "Open Admin \u2192 Plugins to see all installed plugins with their status. Enable or disable plugins directly from your phone.",
        deepLinkAction = "open_admin",
    ),
    DiscoverFeature(
        id = "admin_tasks",
        title = "Scheduled Tasks",
        shortDescription = "View and trigger server tasks like library scans.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Animation,
        adminOnly = true,
        detailedDescription = "Open Admin \u2192 Tasks to see all scheduled server tasks. Trigger a library scan and watch its progress in real-time.",
        deepLinkAction = "open_admin",
    ),
    DiscoverFeature(
        id = "admin_messaging",
        title = "Device Messaging",
        shortDescription = "Send messages to connected session displays and broadcast to all users.",
        category = DiscoverCategory.AdminTools,
        icon = Icons.Rounded.Send,
        adminOnly = true,
        detailedDescription = "From Active Sessions, tap a session to send a display message. Messages appear on the user\u2019s Jellyfin client.",
        deepLinkAction = "open_admin",
    ),
)
