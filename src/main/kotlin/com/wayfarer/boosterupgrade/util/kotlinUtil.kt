package com.wayfarer.boosterupgrade.util

inline fun <T> T.chainIf(condition: Boolean, block: T.() -> T) =
    if (condition) block() else this

inline fun <T> T.chainIfNot(condition: Boolean, block: T.() -> T) =
    chainIf(!condition, block)

inline fun <T, V> T.chainIfNotNull(v: V?, block: T.(V) -> T) =
    if (v != null) block(v) else this
