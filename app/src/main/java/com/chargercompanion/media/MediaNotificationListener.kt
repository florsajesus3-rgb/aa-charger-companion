package com.chargercompanion.media

import android.service.notification.NotificationListenerService

/**
 * Enables MediaSessionManager.getActiveSessions for the media remote.
 * User must enable: Settings → Notification access → Charger Companion.
 */
class MediaNotificationListener : NotificationListenerService()
