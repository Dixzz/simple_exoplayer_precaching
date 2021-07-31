package com.example.myapplication;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

public class MyApp extends Application {
    public SimpleCache simpleCache;


    @Override
    public void onCreate() {
        super.onCreate();
        final long exoPlayerCacheSize = 180 * 1024 * 1024; // ~200MB
        if (simpleCache == null) {
            simpleCache = new SimpleCache(getCacheDir(), new LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize), new ExoDatabaseProvider(this));
        }
        Fresco.initialize(this);
    }
}
