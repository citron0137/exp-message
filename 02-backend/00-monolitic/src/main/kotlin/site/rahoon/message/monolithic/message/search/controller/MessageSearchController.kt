package site.rahoon.message.monolithic.message.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.common.controller.CommonApiResponse
import site.rahoon.message.monolithic.message.search.application.MessageSearchService

/**
 * Message Search Controller.
 *
 * Provides REST API endpoints for searching chat messages stored in Elasticsearch.
 *
 * ## Architecture:
 * ```
 * Client -> MessageSearchController -> MessageSearchService -> Elasticsearch
 *        -> MessageController       -> MessageDomainService  -> MySQL
 * ```
 *
 * ## Why a dedicated controller?
 * - Message CRUD is handled by MessageController (MySQL as source of truth)
 * - Message search is handled by this controller (Elasticsearch as search engine)
 *
 * Keeping them separate makes it clear which storage is used for which purpose.
 *
 * ## Elasticsearch vs MySQL:
 * - MySQL: Source of truth for message data (ACID, reliable)
 * - Elasticsearch: Optimized for full-text search (fast, scalable)
 */
@Tag(name = "Message Search", description = "Full-text search for chat messages using Elasticsearch")
@RestController
@RequestMapping("/messages/search")
class MessageSearchController(
    private val messageSearchService: MessageSearchService
) {

    /**
     * Search messages across all chat rooms by keyword.
     *
     * GET /messages/search?q={keyword}
     *
     * ## Notes:
     * - This is Level 1 (MVP) implementation.
     * - No pagination is implemented yet.
     * - Korean tokenization will be improved in Level 2 (Nori analyzer).
     *
     * @param keyword Search keyword (required)
     * @param limit Maximum number of results to return (optional)
     * @return Search results
     */
    @Operation(
        summary = "Search messages by keyword",
        description = """
            Performs full-text search across all chat messages.
            
            **How it works:**
            1. The keyword is analyzed (lowercased, tokenized)
            2. Elasticsearch finds documents containing matching tokens
            3. Results are returned ordered by relevance score
            
            **Example:** Searching for "hello" will find messages containing 
            "hello", "Hello", "HELLO", etc.
            
            **Limitations (Level 1):**
            - Korean text may not be tokenized properly (use Nori in Level 2)
            - No cursor-based pagination yet
        """
    )
    @GetMapping
    fun search(
        @RequestParam("q") @NotBlank keyword: String,
        @RequestParam("limit", required = false) @Min(1) @Max(100) limit: Int?
    ): CommonApiResponse<List<MessageSearchResponse>> {
        val results = messageSearchService.searchMessages(keyword)
        val applied =
            if (limit == null) {
                results
            } else {
                results.take(limit)
            }

        return CommonApiResponse.success(applied.map { MessageSearchResponse.from(it) })
    }

    /**
     * Search messages within a specific chat room.
     *
     * GET /messages/search/rooms/{chatRoomId}?q={keyword}
     *
     * @param chatRoomId The chat room identifier
     * @param keyword Search keyword (required)
     * @param limit Maximum number of results to return (optional)
     * @return Search results filtered by room
     */
    @Operation(
        summary = "Search messages in a specific chat room",
        description = """
            Performs full-text search within a specific chat room.
            
            Combines two conditions:
            1. chatRoomId must match exactly (filter)
            2. content must contain the keyword (full-text search)
            
            Use this when you want to search within a conversation.
        """
    )
    @GetMapping("/rooms/{chatRoomId}")
    fun searchInRoom(
        @PathVariable chatRoomId: String,
        @RequestParam("q") @NotBlank keyword: String,
        @RequestParam("limit", required = false) @Min(1) @Max(100) limit: Int?
    ): CommonApiResponse<List<MessageSearchResponse>> {
        val results = messageSearchService.searchMessagesInRoom(keyword, chatRoomId)
        val applied =
            if (limit == null) {
                results
            } else {
                results.take(limit)
            }

        return CommonApiResponse.success(applied.map { MessageSearchResponse.from(it) })
    }
}
