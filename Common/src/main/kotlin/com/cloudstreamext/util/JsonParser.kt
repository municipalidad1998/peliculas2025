package com.cloudstreamext.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Utility class for parsing JSON data using Gson.
 * Provides safe parsing with error handling and common patterns.
 *
 * Usage:
 * ```kotlin
 * val json = JsonParser.parse(text)
 * val title = JsonParser.getString(json, "data.title")
 * val items = JsonParser.getArray(json, "results")
 * ```
 */
object JsonParser {

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    /**
     * Parses a JSON string into a JsonElement.
     */
    fun parse(json: String): JsonElement? {
        return try {
            JsonParser.parseString(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a JSON string into a JsonObject.
     */
    fun parseObject(json: String): JsonObject? {
        return try {
            JsonParser.parseString(json).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a JSON string into a data class using Gson.
     */
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            gson.fromJson(json, object : TypeToken<T>() {}.type as Type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a JSON string into a list of a data class.
     */
    inline fun <reified T> listFromJson(json: String): List<T>? {
        return try {
            val type: Type = object : TypeToken<List<T>>() {}.type as Type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts an object to a JSON string.
     */
    fun toJson(obj: Any): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            "{}"
        }
    }

    // --- Safe Accessors ---

    /**
     * Gets a string value from a JsonObject by key path (dot notation).
     * Example: getString(json, "data.results.0.title")
     */
    fun getString(json: JsonObject, path: String, default: String = ""): String {
        return getValue(json, path)?.let {
            if (it.isJsonPrimitive) it.asString else default
        } ?: default
    }

    /**
     * Gets an integer value from a JsonObject by key path.
     */
    fun getInt(json: JsonObject, path: String, default: Int = 0): Int {
        return getValue(json, path)?.let {
            if (it.isJsonPrimitive) it.asInt else default
        } ?: default
    }

    /**
     * Gets a long value from a JsonObject by key path.
     */
    fun getLong(json: JsonObject, path: String, default: Long = 0L): Long {
        return getValue(json, path)?.let {
            if (it.isJsonPrimitive) it.asLong else default
        } ?: default
    }

    /**
     * Gets a double value from a JsonObject by key path.
     */
    fun getDouble(json: JsonObject, path: String, default: Double = 0.0): Double {
        return getValue(json, path)?.let {
            if (it.isJsonPrimitive) it.asDouble else default
        } ?: default
    }

    /**
     * Gets a boolean value from a JsonObject by key path.
     */
    fun getBool(json: JsonObject, path: String, default: Boolean = false): Boolean {
        return getValue(json, path)?.let {
            if (it.isJsonPrimitive) it.asBoolean else default
        } ?: default
    }

    /**
     * Gets a JsonArray from a JsonObject by key path.
     */
    fun getArray(json: JsonObject, path: String): JsonArray {
        return getValue(json, path)?.let {
            if (it.isJsonArray) it.asJsonArray else JsonArray()
        } ?: JsonArray()
    }

    /**
     * Gets a JsonObject from a JsonObject by key path.
     */
    fun getObject(json: JsonObject, path: String): JsonObject {
        return getValue(json, path)?.let {
            if (it.isJsonObject) it.asJsonObject else JsonObject()
        } ?: JsonObject()
    }

    /**
     * Navigates a JsonObject by a dot-separated path.
     * Returns null if any step fails.
     */
    fun getValue(json: JsonObject, path: String): JsonElement? {
        val parts = path.split(".")
        var current: JsonElement = json
        for (part in parts) {
            if (part.matches(Regex("\\d+"))) {
                // Array index
                if (!current.isJsonArray) return null
                val index = part.toInt()
                val array = current.asJsonArray
                if (index >= array.size()) return null
                current = array[index]
            } else {
                // Object key
                if (!current.isJsonObject) return null
                current = current.asJsonObject.get(part) ?: return null
            }
        }
        return current
    }

    /**
     * Checks if a path exists in a JsonObject.
     */
    fun hasPath(json: JsonObject, path: String): Boolean {
        return getValue(json, path) != null
    }

    /**
     * Gets all string values from a JsonArray.
     */
    fun toStringList(array: JsonArray): List<String> {
        return array.mapNotNull { element ->
            if (element.isJsonPrimitive) element.asString else null
        }
    }

    /**
     * Extracts all values for a given key from a JsonArray of Objects.
     */
    fun extractValues(array: JsonArray, key: String): List<String> {
        return array.mapNotNull { element ->
            if (element.isJsonObject) {
                element.asJsonObject.get(key)?.let {
                    if (it.isJsonPrimitive) it.asString else null
                }
            } else null
        }
    }

    /**
     * Extracts all integer values for a given key from a JsonArray of Objects.
     */
    fun extractInts(array: JsonArray, key: String): List<Int> {
        return array.mapNotNull { element ->
            if (element.isJsonObject) {
                element.asJsonObject.get(key)?.let {
                    if (it.isJsonPrimitive) it.asInt else null
                }
            } else null
        }
    }
}
