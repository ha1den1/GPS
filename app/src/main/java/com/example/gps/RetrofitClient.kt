import com.example.gps.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RetrofitClient {

    companion object {
        private const val BASE_URL = "http://192.168.1.103:3000"

        fun create(): ApiService {
            val okHttpClient = getUnsafeOkHttpClient()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }

        private fun getUnsafeOkHttpClient(): OkHttpClient {
            try {

                val trustAllCertificates = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                })


                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCertificates, SecureRandom())


                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

                return OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustAllCertificates[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .addInterceptor(loggingInterceptor)
                    .build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
