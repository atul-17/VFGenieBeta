package com.vodafone.idtmlib.lib.dagger;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.gson.Gson;
import com.vodafone.idtmlib.BuildConfig;

import java.util.concurrent.Semaphore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private Application application;
    private Semaphore idtmSemaphore;

    public AppModule(Application application) {
        this.application = application;
        this.idtmSemaphore = new Semaphore(1);
    }

    @Singleton
    @Provides
    Application provideApplication() {
        return application;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return application.getApplicationContext();
    }

    @Singleton
    @Provides
    ContentResolver provideContentResolver() {
        return application.getContentResolver();
    }

    @Singleton
    @Provides
    AssetManager provideAssetManager() {
        return application.getAssets();
    }

    @Singleton
    @Provides
    PackageManager providePackageManager() {
        return application.getPackageManager();
    }

    @Singleton
    @Provides
    Resources provideResources() {
        return application.getResources();
    }

    @Singleton
    @Provides
    ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Singleton
    @Provides
    SharedPreferences provideSharedPreferences() {
        return application.getSharedPreferences(BuildConfig.LIBRARY_PACKAGE_NAME, 0);
    }

    @Singleton
    @Provides
    Gson provideGson() {
        return new Gson();
    }

    @Singleton
    @Provides
    FirebaseJobDispatcher dispatcher() {
        return new FirebaseJobDispatcher(new GooglePlayDriver(application));
    }

    @Singleton
    @Provides
    Semaphore provideIdtmSemaphore() {
        return this.idtmSemaphore;
    }
}