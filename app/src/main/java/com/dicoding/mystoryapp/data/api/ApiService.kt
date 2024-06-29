package com.dicoding.mystoryapp.data.api

import com.dicoding.mystoryapp.data.response.DetailStoryResponse
import com.dicoding.mystoryapp.data.response.FileUploadResponse
import com.dicoding.mystoryapp.data.response.StoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("stories")
    suspend fun getStories(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): StoryResponse


    @Multipart
    @POST("stories")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody,
    ) : FileUploadResponse

    @GET("stories/{id}")
    suspend fun getStoryDetail(
        @Path("id") storyId: String
    ) : DetailStoryResponse


    @GET("stories")
    suspend fun getStoriesLocation(
        @Query("location") location: Int = 1
    ): StoryResponse
}