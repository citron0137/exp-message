package site.rahoon.message.__monolitic.common.global

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * LocalDateTime <-> epoch millis 변환 확장 함수
 */
fun LocalDateTime.toEpochMilliLong(zoneId: ZoneId): Long {
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
}

