package expo.modules.printers.commons

/**
 * Safe data extraction utilities for handling potentially null or incorrect data types
 * from JavaScript/TypeScript side in printer modules.
 */

/**
 * Safely extracts a String value from a Map, throwing IllegalArgumentException with descriptive message if missing or invalid.
 */
fun Map<String, Any>.safeGetString(key: String, fieldName: String? = null): String {
    val displayName = fieldName ?: key
    return this[key] as? String ?: throw IllegalArgumentException("Missing or invalid $displayName")
}

/**
 * Safely extracts a String value from a Map with a default value if null or missing.
 */
fun Map<String, Any>.safeGetStringOrDefault(key: String, defaultValue: String = ""): String {
    return this[key] as? String ?: defaultValue
}

/**
 * Safely extracts an Int value from a Map, handling both Number and String types.
 */
fun Map<String, Any>.safeGetInt(key: String, fieldName: String? = null): Int {
    val displayName = fieldName ?: key
    return when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() 
            ?: throw IllegalArgumentException("Invalid $displayName format: $value")
        else -> throw IllegalArgumentException("Missing or invalid $displayName")
    }
}

/**
 * Safely extracts an Int value from a Map with a default value.
 */
fun Map<String, Any>.safeGetIntOrDefault(key: String, defaultValue: Int): Int {
    return when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: defaultValue
        null -> defaultValue
        else -> defaultValue
    }
}

/**
 * Safely extracts a nested Map from a Map.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.safeGetMap(key: String, fieldName: String? = null): Map<String, Any> {
    val displayName = fieldName ?: key
    return this[key] as? Map<String, Any> 
        ?: throw IllegalArgumentException("Missing or invalid $displayName")
}

/**
 * Safely parses PrinterConnectionType from string with error handling.
 */
fun String.toPrinterConnectionType(): PrinterConnectionType {
    return try {
        PrinterConnectionType.valueOf(this)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid connectionType: $this", e)
    }
} 