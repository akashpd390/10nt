package com.tenants.app.di

import android.content.Context
import com.tenants.app.BuildConfig
import com.tenants.app.data.local.TokenManager
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.service.ServerWakeupForegroundService
import com.tenants.app.utils.InteractiveIntentSmsSender
import com.tenants.app.utils.SmsSender
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

val appModule = module {
    // Local secure storage
    single { TokenManager(get()) }

    // SMS Sender (Default: InteractiveIntentSmsSender; switch to DirectSimSmsSender when ready)
    single<SmsSender> { InteractiveIntentSmsSender() }

    // Networking
    single {
        val tokenManager: TokenManager = get()
        val context: Context = get()

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            tokenManager.getAccessToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                chain.proceed(requestBuilder.build())
            } catch (e: Exception) {
                // If socket timeout occurs while internet is active, assume Render server cold-start
                if (e is SocketTimeoutException || e is IOException) {
                    try {
                        ServerWakeupForegroundService.start(context)
                    } catch (serviceException: Exception) {
                        // Suppress background service start exceptions if OS restricts
                    }
                }
                throw e
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS) // Generous 60s for Render initial handshake
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    single<TenantsApiService> {
        val okHttpClient: OkHttpClient = get()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TenantsApiService::class.java)
    }

    // ViewModels
    viewModel { com.tenants.app.presentation.dashboard.DashboardViewModel(get()) }
    viewModel { com.tenants.app.presentation.rooms.RoomsViewModel(get()) }
    viewModel { com.tenants.app.presentation.tenants.TenantsViewModel(get()) }
    viewModel { com.tenants.app.presentation.payments.PaymentViewModel(get()) }
}
