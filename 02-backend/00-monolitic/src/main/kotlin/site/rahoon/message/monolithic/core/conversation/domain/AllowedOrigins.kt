package site.rahoon.message.monolithic.core.conversation.domain

data class AllowedOrigins(
    val values: List<String>,
) {
    /**
     * Returns true when the requested origin is allowed by this policy.
     */
    fun allows(origin: Origin): Boolean = values.any { it == WILDCARD || it == origin.value }

    companion object {
        private const val WILDCARD = "*"

        /**
         * Creates allowed origins with normalized and distinct values.
         */
        fun of(values: List<String>): AllowedOrigins =
            AllowedOrigins(
                values =
                    values
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct(),
            )
    }
}
