package com.winnyking.bookstackcompanion.data.api

import com.winnyking.bookstackcompanion.data.api.model.BookDto
import com.winnyking.bookstackcompanion.data.api.model.ChapterDto
import com.winnyking.bookstackcompanion.data.api.model.PageDto
import com.winnyking.bookstackcompanion.data.api.model.PagedResponse
import com.winnyking.bookstackcompanion.data.api.model.SearchResultDto
import com.winnyking.bookstackcompanion.data.api.model.ShelfDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BookStackApi {

    @GET("api/books")
    suspend fun getBooks(
        @Query("count") count: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "name"
    ): PagedResponse<BookDto>

    @GET("api/books/{id}")
    suspend fun getBookDetail(@Path("id") id: Long): BookDto

    @GET("api/shelves")
    suspend fun getShelves(
        @Query("count") count: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "name"
    ): PagedResponse<ShelfDto>

    @GET("api/shelves/{id}")
    suspend fun getShelfDetail(@Path("id") id: Long): ShelfDto

    @GET("api/chapters/{id}")
    suspend fun getChapterDetail(@Path("id") id: Long): ChapterDto

    @GET("api/pages/{id}")
    suspend fun getPageDetail(@Path("id") id: Long): PageDto

    @GET("api/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("count") count: Int = 50
    ): PagedResponse<SearchResultDto>

}
