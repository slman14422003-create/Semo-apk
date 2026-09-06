package com.tomodachi.chat

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.tomodachi.chat.notification.NotificationHelper

class TomodachiApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    /**
     * ImageLoader مخصّص يسجّل فكّ تشفير GIF/WebP المتحرّك — بدونه، Coil يعرض
     * فقط الإطار الأول الثابت من روابط ستيكرات Giphy المتحركة بدل تشغيلها
     * فعلياً، وهو ما كان سيجعل ميزة "ستيكرات من الإنترنت" تبدو معطوبة رغم
     * أنها تعمل تقنياً. نستخدم ImageDecoderDecoder على أندرويد 9+ (أسرع
     * وأخف على البطارية) ونتراجع لـ GifDecoder على ما قبلها لدعم كل الأجهزة
     * حتى minSdk 23 الحالي للمشروع.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
