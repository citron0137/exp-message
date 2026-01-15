package site.rahoon.message.__monolitic.common.application

import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * cursor 디코딩 결과(공통)
 *
 * - version: 커서 포맷 버전 (예: "1")
 * - cursors: 실제 커서 payload
 */
open class CommonPageCursor(
    val version: String,
    val cursors: List<Pair<String, String>>,
    private var encoded: String? = null,
    private var cursorMap: Map<String,String>? = null,
){

    companion object {
        /**
         * cursors 순서에 따라 sk가 생성됩니다.
         */
        fun encode(
            version: String,
            cursors: List<Pair<String, String>>,
        ): String {
            return CommonPageCursor(version, cursors).encode()
        }

        fun decode(cursor: String): CommonPageCursor {
            val payload = try {
                val decoded = Base64.getUrlDecoder().decode(cursor)
                String(decoded, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "reason" to "base64url decode failed"),
                    cause = e
                )
            }

            var version: String? = null
            var sortKeys: List<String> = emptyList()
            val pairs = mutableListOf<Pair<String, String>>()

            payload.split("&").forEach { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@forEach

                val key = part.substring(0, idx)
                val value = part.substring(idx + 1)
                if (key.isBlank()) return@forEach

                when (key) {
                    "v" -> version = value
                    "sk" -> sortKeys = value
                        .split(",")
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toList()
                    else -> pairs.add(key to value)
                }
            }

            val appliedVersion = version?.takeIf { it.isNotBlank() }
                ?: throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "missing version(v)")
                )

            // sk에 정의된 key는 먼저, 그 외 key는 뒤로(원래 순서 유지)
            val order = sortKeys
                .withIndex()
                .associate { (idx, key) -> key to idx }

            val cursors = pairs
                .asSequence()
                .mapIndexed { originalIndex, (key, value) ->
                    Triple(
                        order[key] ?: Int.MAX_VALUE,
                        originalIndex,
                        key to value
                    )
                }
                .sortedWith(compareBy<Triple<Int, Int, Pair<String, String>>> { it.first }.thenBy { it.second })
                .map { it.third }
                .toList()

            return CommonPageCursor(version = appliedVersion, cursors = cursors)
        }
    }

    fun encode(): String {
        if(this.encoded != null) return this.encoded!!
        val sortKeys = cursors.map { it.first }
        val payload = buildString {
            append("v=")
            append(version)
            append("&sk=")
            append(sortKeys.joinToString(",") { it })
            cursors.forEach { (key, value) ->
                append("&")
                append(key)
                append("=")
                append(value)
            }
        }

        this.encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return this.encoded!!
    }

    private fun getCursorMap() : Map<String,String>{
        if(this.cursorMap != null) return this.cursorMap!!
        this.cursorMap = cursors.toMap()
        return this.cursorMap!!
    }


    private fun invalid(reason: String, extra: Map<String, Any> = emptyMap()): Nothing {
        throw DomainException(
            error = CommonError.INVALID_PAGE_CURSOR,
            details = mapOf("cursor" to encode(), "reason" to reason) + extra
        )
    }

    fun requireVersion(expected: String): CommonPageCursor {
        if (version != expected) {
            invalid(
                reason = "unsupported version",
                extra = mapOf("version" to version, "expected" to expected)
            )
        }
        return this
    }

    fun requireKeysInOrder(expectedKeys: List<String>): CommonPageCursor{
        val actualKeys = cursors.map { it.first }
        if (actualKeys != expectedKeys) {
            invalid(
                reason = "invalid keys order",
                extra = mapOf("expectedKeys" to expectedKeys, "actualKeys" to actualKeys)
            )
        }
        return this
    }

    fun getAsString(key: String): String {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value
    }

    fun getAsLong(key: String): Long {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value.toLongOrNull()
            ?: invalid(reason = "invalid long value", extra = mapOf("key" to key, "value" to value))
    }

    fun getAsDouble(key: String): Double {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value.toDoubleOrNull()
            ?: invalid(reason = "invalid double value", extra = mapOf("key" to key, "value" to value))
    }
}
