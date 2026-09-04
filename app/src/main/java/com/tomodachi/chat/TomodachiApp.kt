package com.tomodachi.chat

import android.app.Application
import com.tomodachi.chat.notification.NotificationHelper

class TomodachiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
