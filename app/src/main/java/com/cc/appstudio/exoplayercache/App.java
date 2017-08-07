package com.cc.appstudio.exoplayercache;

import android.app.Application;
import android.content.Context;

import com.cc.appstudio.exoplayercache.utilities.FileUtils;
import com.danikula.videocache.HttpProxyCacheServer;

/**
 * Created by afali on 8/7/17.
 */

public class App extends Application {

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .cacheDirectory(FileUtils.getVideoCacheDir(this))
                .build();
    }
}
