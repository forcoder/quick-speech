package com.quickspeech.common.util

import retrofit2.Response

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Resource.Success(body)
            } else {
                Resource.Error("Empty response body")
            }
        } else {
            Resource.Error(
                message = response.errorBody()?.string() ?: "Unknown error",
                code = response.code()
            )
        }
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Network error")
    }
}
