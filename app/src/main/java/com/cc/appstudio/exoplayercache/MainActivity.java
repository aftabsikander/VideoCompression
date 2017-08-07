package com.cc.appstudio.exoplayercache;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.widget.SeekBar;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements CacheListener {

    private static final String LOG_TAG = "VideoFragment";

    /*
    http://beta.aap.csquareonline.com/backend/web/uploads/videos/2017/08/aap20170802150016.mp4
    http://beta.aap.csquareonline.com/backend/web/uploads/videos/2017/08/aap20170802150101.mp4
    http://beta.aap.csquareonline.com/backend/web/uploads/videos/2017/08/aap20170802150210.mp4
    http://beta.aap.csquareonline.com/backend/web/uploads/videos/2017/08/aap20170802150054.mp4
     */
    String url = "http://beta.aap.csquareonline.com/backend/web/uploads/videos/2017/08/aap20170802150210.mp4";

    @BindView(R.id.cacheStatusImageView)
    AppCompatImageView cacheStatusImageView;
    @BindView(R.id.simpleExoPlayerView)
    SimpleExoPlayerView simpleExoPlayerView;
    @BindView(R.id.progressBar)
    SeekBar progressBar;

    private SimpleExoPlayer simpleExoPlayer;

    private final VideoProgressUpdater updater = new VideoProgressUpdater();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        checkCachedState();
        simpleExoPlayer = setupPlayer();
        simpleExoPlayer.setPlayWhenReady(true);

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekVideo();
            }
        });

    }

    private void checkCachedState() {
        HttpProxyCacheServer proxy = App.getProxy(this);
        boolean fullyCached = proxy.isCached(url);
        setCachedState(fullyCached);
        if (fullyCached) {
            progressBar.setSecondaryProgress(100);
        }
    }

    private SimpleExoPlayer setupPlayer() {
        simpleExoPlayerView.setUseController(false);
        HttpProxyCacheServer proxy = App.getProxy(this);
        proxy.registerCacheListener(this, url);
        String proxyUrl = proxy.getProxyUrl(url);
        Log.d(LOG_TAG, "Use proxy url " + proxyUrl + " instead of original url " + url);

        SimpleExoPlayer exoPlayer = newSimpleExoPlayer();
        simpleExoPlayerView.setPlayer(exoPlayer);

        MediaSource videoSource = newVideoSource(proxyUrl);
        exoPlayer.prepare(videoSource);

        return exoPlayer;
    }

    private SimpleExoPlayer newSimpleExoPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        return ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
    }

    private MediaSource newVideoSource(String url) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        String userAgent = Util.getUserAgent(this, "AndroidVideoCache sample");
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent, bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        return new ExtractorMediaSource(Uri.parse(url), dataSourceFactory, extractorsFactory, null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        updater.start();
        simpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        updater.stop();
        simpleExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        simpleExoPlayer.release();
        App.getProxy(this).unregisterCacheListener(this);
    }


    private void updateVideoProgress() {
        long videoProgress = simpleExoPlayer.getCurrentPosition() * 100 / simpleExoPlayer.getDuration();
        progressBar.setProgress((int) videoProgress);
    }

    //@SeekBarTouchStop(R.id.progressBar)
    void seekVideo() {
        long videoPosition = simpleExoPlayer.getDuration() * progressBar.getProgress() / 100;
        simpleExoPlayer.seekTo(videoPosition);
    }

    private void setCachedState(boolean cached) {
        int statusIconId = cached ? R.drawable.ic_cloud_done_black_24dp :
                R.drawable.ic_cloud_download_black_24dp;
        cacheStatusImageView.setImageResource(statusIconId);
    }

    @Override
    public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
        progressBar.setSecondaryProgress(percentsAvailable);
        setCachedState(percentsAvailable == 100);
        Log.d(LOG_TAG, String.format("onCacheAvailable. percents: %d, file: %s, url: %s",
                percentsAvailable, cacheFile, url));
    }

    private final class VideoProgressUpdater extends Handler {

        public void start() {
            sendEmptyMessage(0);
        }

        public void stop() {
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message msg) {
            updateVideoProgress();
            sendEmptyMessageDelayed(0, 500);
        }
    }

}
