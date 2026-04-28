package site.rahoon.message.monolithic.core.shared.error

interface CoreError {
    val boundedContext: BoundedContext
    val code: String
    val userMessage: String
    val developerMessage: String
    val category: ErrorCategory
}
