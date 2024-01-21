package com.blr19c.falowp.bot.bili.plugins.bili.api

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.properties.ReadOnlyProperty

internal fun timestamp(sec: Long) = OffsetDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneOffset.systemDefault())

/**
 * 2017-07-01 00:00:00
 */
private const val DYNAMIC_START = 1498838400L

internal fun dynamictime(id: Long): Long = (id shr 32) + DYNAMIC_START

internal inline fun <reified T : Any, reified R> reflect() = ReadOnlyProperty<T, R> { thisRef, property ->
    thisRef::class.java.getDeclaredField(property.name).apply { isAccessible = true }.get(thisRef) as R
}
