package com.lagradost.cloudstream3

/**
 * Stub HomePageList for CloudStream 3 home page.
 */
data class HomePageList(
    val name: String,
    val list: List<HomePageListResponse>,
    var isHorizontalImages: Boolean = false
)

/**
 * Stub HomePageListResponse.
 */
data class HomePageListResponse(
    val name: String,
    val list: List<SearchResponse>,
    val isHorizontalImages: Boolean = false
)

/**
 * Stub HomePageRequest.
 */
data class HomePageRequest(
    val name: String,
    val data: String = ""
)
