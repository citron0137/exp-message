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

            // 빈 payload 체크
            if (payload.isBlank()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "empty payload")
                )
            }

            var version: String? = null
            var versionCount = 0
            var sortKeys: List<String> = emptyList()
            var sortKeysCount = 0
            val pairs = mutableListOf<Pair<String, String>>()
            val seenKeys = mutableSetOf<String>()

            payload.split("&").forEach { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@forEach

                val key = part.substring(0, idx)
                val value = part.substring(idx + 1)
                if (key.isBlank()) return@forEach

                when (key) {
                    "v" -> {
                        versionCount++
                        if (versionCount > 1) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate version key(v)"
                                )
                            )
                        }
                        version = value
                    }
                    "sk" -> {
                        sortKeysCount++
                        if (sortKeysCount > 1) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate sort keys key(sk)"
                                )
                            )
                        }
                        sortKeys = value
                            .split(",")
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toList()
                        
                        // sk에 중복된 키가 있는지 체크 (Set으로 효율적으로)
                        val sortKeysSet = mutableSetOf<String>()
                        val duplicateInSk = mutableListOf<String>()
                        for (sortKey in sortKeys) {
                            if (!sortKeysSet.add(sortKey)) {
                                duplicateInSk.add(sortKey)
                            }
                        }
                        if (duplicateInSk.isNotEmpty()) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate keys in sort keys(sk)",
                                    "duplicateKeys" to duplicateInSk
                                )
                            )
                        }
                    }
                    else -> {
                        // 일반 키 중복 체크
                        if (seenKeys.contains(key)) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate key in cursor payload",
                                    "duplicateKey" to key
                                )
                            )
                        }
                        seenKeys.add(key)
                        pairs.add(key to value)
                    }
                }
            }

            val appliedVersion = version?.takeIf { it.isNotBlank() }
                ?: throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "missing version(v)")
                )

            // sk가 비어있는지 체크
            if (sortKeys.isEmpty()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "missing or empty sort keys(sk)")
                )
            }

            // sk에 정의된 키가 pairs에 모두 있는지 확인 (누락 키 체크)
            // seenKeys를 재사용하여 불필요한 Set 생성 방지
            val missingKeys = sortKeys.filter { it !in seenKeys }
            if (missingKeys.isNotEmpty()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf(
                        "cursor" to cursor,
                        "payload" to payload,
                        "reason" to "missing required keys in cursor payload",
                        "missingKeys" to missingKeys,
                        "sortKeys" to sortKeys,
                        "availableKeys" to seenKeys.toList()
                    )
                )
            }

            // sk에 정의된 key는 먼저, 그 외 key는 뒤로(원래 순서 유지)
            // 정렬 대신 두 리스트로 분리 후 합치기 (O(p log p) → O(p))
            val order = sortKeys
                .withIndex()
                .associate { (idx, key) -> key to idx }
            
            val sortedPairs = mutableListOf<Pair<String, String>>()
            val otherPairs = mutableListOf<Pair<String, String>>()
            
            for (pair in pairs) {
                if (pair.first in order) {
                    sortedPairs.add(pair)
                } else {
                    otherPairs.add(pair)
                }
            }
            
            // sortKeys 순서대로 정렬 (보통 2-3개이므로 O(s log s)는 무시 가능)
            sortedPairs.sortBy { order[it.first] }
            
            val cursors = sortedPairs + otherPairs

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
