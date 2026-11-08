package com.sonata.timeline

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class Type { DAILY, DEADLINE, IMPORTANT }

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: Type,
    val due: LocalDate? = null,
    val note: String = "",
    val done: Boolean = false,
    val lastDone: LocalDate? = null
)

fun Item.isDone() = if (type == Type.DAILY) lastDone == LocalDate.now() else done

fun Item.daysLeft() = due?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

// belum selesai dulu, lalu deadline > penting > harian, lalu due date terdekat
fun List<Item>.sortedTimeline() = sortedWith(
    compareBy<Item>({ it.isDone() }, { it.type.ordinal }, { it.due ?: LocalDate.MAX })
)