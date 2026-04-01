package code.name.monkey.retromusic.alist.network

import code.name.monkey.retromusic.alist.model.*
import retrofit2.http.*

interface AlistService {
    @POST("api/auth/login")
    suspend fun login(@Body request: AlistLoginRequest): AlistResponse<AlistLoginResponse>

    @POST("api/fs/list")
    suspend fun listFiles(@Body request: AlistFsListRequest, @Header("Authorization") token: String? = null): AlistResponse<AlistFsListResponse>

    @POST("api/fs/get")
    suspend fun getFile(@Body request: AlistFsGetRequest, @Header("Authorization") token: String? = null): AlistResponse<AlistFsGetResponse>
}
