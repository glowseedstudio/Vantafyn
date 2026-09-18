package dev.vantafyn.core.integrations.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object VantafynPushNotifier {
    private const val TAG = "VantafynPushNotifier"

    private const val CHANNEL_ID_GENERAL = "vantafyn_push_channel"
    private const val CHANNEL_NAME_GENERAL = "Vantafyn Push Notifications"

    private const val CHANNEL_ID_CHAT = "vantafyn_chat_channel"
    private const val CHANNEL_NAME_CHAT = "Vantafyn Chat & Messages"

    private const val CHANNEL_ID_ACHIEVEMENTS = "vantafyn_achievements_channel"
    private const val CHANNEL_NAME_ACHIEVEMENTS = "Vantafyn Achievements"

    private const val TEST_NOTIFICATION_ID = 9001

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
