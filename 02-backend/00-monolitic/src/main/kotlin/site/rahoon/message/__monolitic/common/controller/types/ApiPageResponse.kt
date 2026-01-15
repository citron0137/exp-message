package site.rahoon.message.__monolitic.common.controller.types

/**
 * 커서 기반 페이징 목록 응답 템플릿
 *
 * - `data`: 기존과 동일하게 리스트(배열)
 * - `pageInfo.nextCursor`: 다음 페이지 커서 (`null`이면 마지막 페이지)
 * - `pageInfo.limit`: 실제 적용된 limit (기본값/최대값 clamp 결과)
 *
 * 공통 필드(`success`, `data`, `error`)는 `ApiResponse`를 그대로 따릅니다.
 */
class ApiPageResponse<T>(
    data: List<T>,
    val pageInfo: PageInfo
) : ApiResponse<List<T>>(
    success = true,
    data = data,
    error = null
) {
    data class PageInfo(
        val nextCursor: String?,
        val limit: Int
    )

    companion object {
        fun <T> success(
            data: List<T>,
            nextCursor: String?,
            limit: Int
        ): ApiPageResponse<T> {
            return ApiPageResponse(
                data = data,
                pageInfo = PageInfo(nextCursor = nextCursor, limit = limit)
            )
        }
    }
}

