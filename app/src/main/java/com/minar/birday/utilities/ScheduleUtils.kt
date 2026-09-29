package com.minar.birday.utilities

import java.time.Duration
import java.time.ZonedDateTime


// A few seconds past the chosen minute, to stay clear of midnight
private const val CHECK_SECOND = 15

/**
 * How long until the next daily check, at the given hour and minute. The next day is a day on
 * the clock, not 24 hours: when daylight saving time begins or ends a day lasts 23 or 25 of them,
 * and 24 hours after a midnight check would land on the 23 o'clock that comes twice (#365)
 */
fun delayToNextCheck(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Duration {
    var next = now.toLocalDate().atTime(hour, minute, CHECK_SECOND).atZone(now.zone)
    if (!next.isAfter(now)) next = now.toLocalDate().plusDays(1)
        .atTime(hour, minute, CHECK_SECOND).atZone(now.zone)
    return Duration.between(now, next)
}
