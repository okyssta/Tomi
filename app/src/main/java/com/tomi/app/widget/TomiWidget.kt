package com.tomi.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.text.Text
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.tomi.app.data.local.TomiDatabase
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TomiWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsRepository = SettingsRepository.from(context)
        val settings = settingsRepository.settings.first()
        val runtime = settingsRepository.runtime.first()
        val today = LocalDate.now().toString()
        val steps = TomiDatabase.get(context).stepCacheDao().observeDay(today).first()?.steps
        val next = runtime.nextReminderAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp),
                ) {
                    Column(GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Tomi",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = steps?.let { "%,d steps today".format(it) } ?: "Connect steps in Tomi",
                            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
                        )
                        Text(
                            text = WidgetText.nextReminder(settings.reminder.enabled, next, Instant.now()),
                            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Row {
                        Button(
                            text = "Open Tomi",
                            onClick = actionStartActivity(Intent(context, MainActivity::class.java)),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Button(
                            text = "Start break",
                            onClick = actionRunCallback<StartBreakAction>(),
                        )
                    }
                }
            }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(TomiWidget::class.java).forEach { update(context, it) }
    }
}

class StartBreakAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_ROUTE, "move/WALK")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

class TomiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: TomiWidget = TomiWidget()
}
