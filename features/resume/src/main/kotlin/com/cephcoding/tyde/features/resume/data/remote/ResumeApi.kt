package com.cephcoding.tyde.features.resume.data.remote

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ResumeApi {

    @Multipart
    @POST("api/resumes")
    suspend fun uploadResume(@Part file: MultipartBody.Part): ResumeDto

    @GET("api/resumes")
    suspend fun getResumes(): List<ResumeDto>
}
