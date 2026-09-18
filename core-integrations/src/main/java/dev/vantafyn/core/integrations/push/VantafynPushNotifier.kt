package dev.vantafyn.core.integrations.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object VantafynPushNotifier {
    private const val TAG = "VantafynPushNotifier"
    private const val CHANNEL_ID = "vantafyn_push_channel"
    private const val CHANNEL_NAME = "Vantafyn Push Notifications"
    private const val TEST_NOTIFICATION_ID = 9001

    fun showNotification(context: Context, title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications delivered through UnifiedPush"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(TEST_NOTIFICATION_ID, builder.build())
            Log.i(TAG, "Displayed push notification: title='$title'")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, unable to display push notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display push notification", e)
        }
    }
}
