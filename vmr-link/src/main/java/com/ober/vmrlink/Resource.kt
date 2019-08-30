package com.ober.vmrlink

interface Resource<out T> {
    val data: T?

    companion object {
        fun <T> success(data: T?, source: Source): Resource<T> {
            return Success(data, source)
        }

        fun <T> error(message: String, data: T?): Resource<T> {
            return Error(data, message)
        }

        fun <T> loading(data: T?, source: Source): Resource<T> {
            return Loading(data, source)
        }
    }
}

interface SuccessResource<out T> : Resource<T> {
    val source: Source
}

data class Success<out T>(override val data: T?, override val source: Source) : SuccessResource<T>

data class Loading<out T>(override val data: T?, override val source: Source) : SuccessResource<T>

data class Error<out T>(override val data: T?, val message: String) : Resource<T>

enum class Source {
    NETWORK,
    DATABASE,
    NO_DATA
}