package hr.foi.air.honnomachi.util

sealed class Result<out T> {
    data class Success<out T>(
        val data: T,
    ) : Result<T>()

    data class Error(
        val exception: Exception,
    ) : Result<Nothing>()

    val Succeeded: Boolean get() = this is Success
    val Failed: Boolean get() = this is Error

    fun getOrNull(): T? = if (this is Success) data else null
}
