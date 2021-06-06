package com.wayfarer.boosterupgrade.util

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonToken.*

fun JsonReader.nextToken() = nextToken(peek())

fun JsonReader.nextToken(jsonToken: JsonToken) {
    when (jsonToken) {
        BEGIN_ARRAY -> beginArray()
        END_ARRAY -> endArray()
        BEGIN_OBJECT -> beginObject()
        END_OBJECT -> endObject()
        NAME -> nextName()
        STRING -> nextString()
        NUMBER -> nextString()
        BOOLEAN -> nextBoolean()
        NULL -> nextNull()
        END_DOCUMENT -> error("Reached end before finding next token")
    }
}

fun JsonReader.untilThisArrayEnd() {
    while (peek() != END_ARRAY) {
        when (peek()) {
            BEGIN_ARRAY -> {
                beginArray()
                untilThisArrayEnd()
            }
            BEGIN_OBJECT -> {
                beginObject()
                untilThisObjectEnd()
            }
            else -> nextToken()
        }
    }
    endArray()
}

fun JsonReader.untilThisObjectEnd() {
    while (peek() != END_OBJECT) {
        when (peek()) {
            BEGIN_ARRAY -> {
                beginArray()
                untilThisArrayEnd()
            }
            BEGIN_OBJECT -> {
                beginObject()
                untilThisObjectEnd()
            }
            else -> nextToken()
        }
    }
    endObject()
}

fun JsonReader.untilToken(jsonToken: JsonToken) {
    while (peek() != jsonToken) {
        nextToken()
    }
    nextToken(jsonToken)
}

fun JsonReader.untilProperty(propertyName: String) {
    while (hasNext()) {
        when (peek()) {
            BEGIN_ARRAY -> {
                beginArray()
                untilThisArrayEnd()
            }
            BEGIN_OBJECT -> {
                beginObject()
                untilThisObjectEnd()
            }
            NAME -> {
                val name = nextName()
                if (name == propertyName) return
            }
            else -> nextToken()
        }
    }
    error("\"$propertyName\" not found")
}
