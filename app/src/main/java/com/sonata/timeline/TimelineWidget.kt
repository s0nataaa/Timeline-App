package com.sonata.timeline

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import android.content.Intent
import androidx.glance.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.datastore.preferences.core.Preferences
import androidx.glance.currentState



class TimelineWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            currentState<Preferences>() // dibaca supaya widget digambar ulang tiap update
            val open = Store.read(context).sortedTimeline().filter { !it.isDone() }
            Content(open)
        }
    }
}

class TimelineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TimelineWidget()
}

private val Gray = Color(0xFF8E8E93)
private fun cp(c: Color) = ColorProvider(day = c, night = c)

private fun short(i: Item): String = when (i.type) {
    Type.IMPORTANT -> "Penting"
    Type.DAILY -> "Harian"
    Type.DEADLINE -> {
        val d = i.daysLeft()!!
        when {
            d < 0 -> "Telat ${-d} hari"
            d == 0 -> "Hari ini"
            d == 1 -> "Besok"
            else -> "$d hari"
        }
    }
}

private fun sub(i: Item): String =
    if (i.type == Type.DEADLINE) i.due!!.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id")))
    else short(i)

@androidx.compose.runtime.Composable
private fun Content(all: List<Item>) {
    Column(
        GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(Color.White)
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java)))
    ) {
        Text("TIMELINE", style = TextStyle(color = cp(Gray), fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        if (all.isEmpty()) {
            Text("Semua beres", style = TextStyle(color = cp(Gray), fontSize = 15.sp))
        }
        all.take(5).forEach { i ->
            val c = meta(i).second
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(8.dp).background(c).cornerRadius(4.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(i.title, maxLines = 1,
                        style = TextStyle(color = cp(Color.Black), fontSize = 15.sp))
                    Text(sub(i), maxLines = 1,
                        style = TextStyle(color = cp(Gray), fontSize = 12.sp))
                }
                if (i.type == Type.DEADLINE) {
                    Text(short(i), style = TextStyle(color = cp(c), fontSize = 13.sp))
                }
            }
        }
        if (all.size > 5) {
            Text("+${all.size - 5} lagi", style = TextStyle(color = cp(Gray), fontSize = 13.sp))
        }
    }
}