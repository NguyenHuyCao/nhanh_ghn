package com.app84soft.check_in.util;

import com.app84soft.check_in.dto.config.ConnectionConfig;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RetrofitCommunication {

    public static <T> T buildSetting(Class<T> classType, ConnectionConfig connect) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(connect.getApiUrl())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(buildCommunication(connect))
                .build();

        return retrofit.create(classType);
    }

    private static OkHttpClient buildCommunication(ConnectionConfig connect) {
        final int maxIdle = 256;
        final long keepAliveDuration = 60;

        Dispatcher dispatcher = new Dispatcher(Executors.newVirtualThreadPerTaskExecutor());
        dispatcher.setMaxRequests(connect.getMaxRequest());
        dispatcher.setMaxRequestsPerHost(connect.getMaxRequestPerHost());
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .addInterceptor(logger)
                .connectTimeout(connect.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(connect.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(connect.getConnectTimeout(), TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdle, keepAliveDuration, TimeUnit.SECONDS))
                .build();
    }
}
