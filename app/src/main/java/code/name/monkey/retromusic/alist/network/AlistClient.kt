package code.name.monkey.retromusic.alist.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AlistClient {
    fun create(baseUrl: String, token: String? = null): AlistService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "EffinMusic-Alist")
                
                if (!token.isNullOrEmpty()) {
                    request.addHeader("Authorization", token)
                }
                
                chain.proceed(request.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlistService::class.java)
    }
}
