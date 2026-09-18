package dev.vantafyn.core.integrations.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

import android.app.PendingIntent
import android.content.Intent

object VantafynPushNotifier {
    private const val TAG = "VantafynPushNotifier"

    const val EXTRA_ACTION = "vantafyn_action"
    const val EXTRA_CONVERSATION_ID = "conversation_id"
    const val EXTRA_SENDER_ID = "sender_id"
    const val EXTRA_SENDER_NAME = "sender_name"
    const val EXTRA_BADGE_ID = "badge_id"

    const val ACTION_OPEN_CHAT = "open_chat"
    const val ACTION_OPEN_ACHIEVEMENTS = "open_achievements"

    private const val CHANNEL_ID_GENERAL = "vantafyn_push_channel"
    private const val CHANNEL_NAME_GENERAL = "Vantafyn Push Notifications"

    private const val CHANNEL_ID_CHAT = "vantafyn_chat_channel"
    private const val CHANNEL_NAME_CHAT = "Vantafyn Chat & Messages"

    private const val CHANNEL_ID_ACHIEVEMENTS = "vantafyn_achievements_channel"
    private const val CHANNEL_NAME_ACHIEVEMENTS = "Vantafyn Achievements"

    private const val TEST_NOTIFICATION_ID = 9001

    private fun getPendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    }

    private fun createGeneralPendingIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: return null
        return PendingIntent.getActivity(context, TEST_NOTIFICATION_ID, intent, getPendingIntentFlags())
    }

    private fun createChatPendingIntent(context: Context, conversationId: String, senderId: String, senderName: String): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_OPEN_CHAT)
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_SENDER_ID, senderId)
            putExtra(EXTRA_SENDER_NAME, senderName)
        } ?: return null
        val reqCode = (conversationId.ifBlank { senderId }).hashCode()
        return PendingIntent.getActivity(context, reqCode, intent, getPendingIntentFlags())
    }

    private fun createAchievementPendingIntent(context: Context, badgeId: String): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_OPEN_ACHIEVEMENTS)
            putExtra(EXTRA_BADGE_ID, badgeId)
        } ?: return null
        val reqCode = badgeId.hashCode()
        return PendingIntent.getActivity(context, reqCode, intent, getPendingIntentFlags())
    }

    fun routeIntent(intent: Intent?): Boolean {
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: return false
        Log.i(TAG, "Routing notification intent action: '$action'")
        when (action) {
            ACTION_OPEN_CHAT -> {
                val convId = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
                val senderId = intent.getStringExtra(EXTRA_SENDER_ID).orEmpty()
                val senderName = intent.getStringExtra(EXTRA_SENDER_NAME)
                if (senderId.isNotBlank() || convId.isNotBlank()) {
                    UnifiedPushPayloadDispatcher.navigateTo(PushNavigationTarget.Chat(convId, senderId, senderName))
                    return true
                }
            }
            ACTION_OPEN_ACHIEVEMENTS -> {
                val badgeId = intent.getStringExtra(EXTRA_BADGE_ID).orEmpty()
                UnifiedPushPayloadDispatcher.navigateTo(PushNavigationTarget.Achievement(badgeId))
                return true
            }
        }
        return false
    }

    fun showNotification(context: Context, title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            ensureChannel(notificationManager, CHANNEL_ID_GENERAL, CHANNEL_NAME_GENERAL, "General notifications from Vantafyn Companion")

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            createGeneralPendingIntent(context)?.let { builder.setContentIntent(it) }

            notificationManager.notify(TEST_NOTIFICATION_ID, builder.build())
            Log.i(TAG, "Displayed general push notification: title='$title'")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, unable to display push notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display push notification", e)
        }
    }

    fun showChatMessage(context: Context, senderName: String, messageText: String, conversationId: String, senderId: String) {
        try {
            Log.i(TAG, "showChatMessage called: senderName='$senderName', message='$messageText', convId='$conversationId', senderId='$senderId'")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            ensureChannel(notificationManager, CHANNEL_ID_CHAT, CHANNEL_NAME_CHAT, "Chat and direct messages from friends")

            val notificationId = (conversationId.ifBlank { senderId }).hashCode()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(senderName)
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            createChatPendingIntent(context, conversationId, senderId, senderName)?.let { builder.setContentIntent(it) }

            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Displayed chat push notification from '$senderName'")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, unable to display chat push notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display chat push notification", e)
        }
    }

    fun showAchievementUnlock(context: Context, title: String, description: String, badgeId: String) {
        try {
            Log.i(TAG, "showAchievementUnlock called: title='$title', badgeId='$badgeId'")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            ensureChannel(notificationManager, CHANNEL_ID_ACHIEVEMENTS, CHANNEL_NAME_ACHIEVEMENTS, "Achievement unlock badges and milestone announcements")

            val notificationId = badgeId.hashCode()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_ACHIEVEMENTS)
                .setSmallIcon(android.R.drawable.star_big_on)
                .setContentTitle("Achievement Unlocked: $title")
                .setContentText(description.ifBlank { "You unlocked a new badge!" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            createAchievementPendingIntent(context, badgeId)?.let { builder.setContentIntent(it) }

            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Displayed achievement unlock push notification: title='$title'")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, unable to display achievement push notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display achievement push notification", e)
        }
    }

    private fun ensureChannel(manager: NotificationManager, channelId: String, channelName: String, desc: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = desc
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
