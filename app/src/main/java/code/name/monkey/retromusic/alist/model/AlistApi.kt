package code.name.monkey.retromusic.alist.model

import com.google.gson.annotations.SerializedName

data class AlistResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class AlistLoginRequest(
    val username: String,
    val password: String
)

data class AlistLoginResponse(
    val token: String
)

data class AlistFsListRequest(
    val path: String,
    val password: String = "",
    val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 0,
    val refresh: Boolean = false
)

data class AlistFsListResponse(
    val content: List<AlistFile>?,
    val provider: String?,
    val total: Int,
    val readme: String?,
    val write: Boolean
)

data class AlistFile(
    val name: String,
    val size: Long,
    @SerializedName("is_dir") val isDir: Boolean,
    val modified: String,
    val sign: String?,
    val thumb: String?,
    val type: Int
)

data class AlistFsGetRequest(
    val path: String,
    val password: String = ""
)

data class AlistFsGetResponse(
    val name: String,
    val size: Long,
    @SerializedName("is_dir") val isDir: Boolean,
    val modified: String,
    val sign: String?,
    val thumb: String?,
    val type: Int,
    @SerializedName("raw_url") val rawUrl: String?
)
