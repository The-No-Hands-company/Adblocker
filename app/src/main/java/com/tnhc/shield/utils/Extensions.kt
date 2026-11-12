package com.tnhc.shield.utils

import java.text.NumberFormat

fun Long.formatCount(): String = when {
    this >= 1_000_000 -> "%.1fM".format(this / 1_000_000.0)
    this >= 1_000 -> "%.1fK".format(this / 1_000.0)
    else -> NumberFormat.getNumberInstance().format(this)
}

fun Int.formatCount(): String = this.toLong().formatCount()
