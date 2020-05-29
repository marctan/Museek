package com.marcqtan.marcqtan.samplemusic.service;

import com.marcqtan.marcqtan.samplemusic.service.TrackService;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Marc Q. Tan on 04/05/2020.
 */
public class SCApi {
    private static final String BASE_URL = "https://api.soundcloud.com/";
    private TrackService service;

    private OkHttpClient.Builder setupHttpClient() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            HttpUrl originalHttpUrl = original.url();
            HttpUrl url = originalHttpUrl.newBuilder()
                    .addQueryParameter("client_id", "AIBMBzom4aIwS64tzA3uvg")
                    .build();

            Request request = original.newBuilder()
                    .url(url).build();
            return chain.proceed(request);
        });

        return httpClient;
    }

    private Retrofit setupRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                //.client(setupHttpClient().build())
                .build();
    }

    public SCApi() {
        service = setupRetrofit().create(TrackService.class);
    }

    public TrackService getService() {
        return service;
    }
}
