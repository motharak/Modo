package com.belta.audio.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.belta.audio.R
import com.belta.audio.core.domain.model.PlaybackState
import com.belta.audio.ui.MainActivity

class BeltaGlassWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                // Broadcast to media controller
                val mediaIntent = Intent("com.belta.audio.ACTION_TOGGLE_PLAYBACK")
                context.sendBroadcast(mediaIntent)
            }
            ACTION_NEXT -> {
                val mediaIntent = Intent("com.belta.audio.ACTION_NEXT_TRACK")
                context.sendBroadcast(mediaIntent)
            }
            ACTION_PREV -> {
                val mediaIntent = Intent("com.belta.audio.ACTION_PREV_TRACK")
                context.sendBroadcast(mediaIntent)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.belta.audio.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.belta.audio.WIDGET_NEXT"
        const val ACTION_PREV = "com.belta.audio.WIDGET_PREV"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, state: PlaybackState?) {
            val views = RemoteViews(context.packageName, R.layout.widget_frosted_glass)

            val track = state?.currentTrack
            if (track != null) {
                views.setTextViewText(R.id.widget_track_title, track.title)
                views.setTextViewText(R.id.widget_track_artist, track.artist)
            } else {
                views.setTextViewText(R.id.widget_track_title, "Belta Audio")
                views.setTextViewText(R.id.widget_track_artist, "Ready to Play")
            }

            val isPlaying = state?.isPlaying ?: false
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            // Click root opens MainActivity
            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPending = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_glass_root, launchPending)

            // Control pending intents
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, getActionPendingIntent(context, ACTION_PLAY_PAUSE, 101))
            views.setOnClickPendingIntent(R.id.widget_btn_next, getActionPendingIntent(context, ACTION_NEXT, 102))
            views.setOnClickPendingIntent(R.id.widget_btn_prev, getActionPendingIntent(context, ACTION_PREV, 103))

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun getActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, BeltaGlassWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
