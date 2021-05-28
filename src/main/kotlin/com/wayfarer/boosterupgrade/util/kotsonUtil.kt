package com.wayfarer.boosterupgrade.util

import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonElement

fun JsonElement.getOrNull(propertyName: String): JsonElement? {
    if (!this.isJsonObject) return null
    val obj = this.obj
    if (!obj.has(propertyName)) return null

    return obj[propertyName].let { if (it.isJsonNull) null else it }
}
