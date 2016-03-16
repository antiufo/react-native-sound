package com.zmxv.RNSound;

import java.util.HashMap;
import java.util.Map;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;

import android.media.AudioManager;
import android.net.Uri;

public class RNSoundModule extends ReactContextBaseJavaModule {
  Map<Integer, ExoPlayer> playerPool = new HashMap<>();
  ReactApplicationContext context;
  final static Object NULL = null;

  public RNSoundModule(ReactApplicationContext context) {
    super(context);
    this.context = context;
  }

  @Override
  public String getName() {
    return "RNSound";
  }

  @ReactMethod
  public void prepare(final String fileName, final Integer key, final Callback callback) {
    try{
        
        //Uri url = Uri.parse(fileName);
        ExoPlayer player = ExoPlayer.Factory.newInstance(1);
        this.playerPool.put(key, player);
        
        String url = "http://shamanbackups.blob.core.windows.net/music/12%20%20Amy%20MacDonald%20-%20Caledonia%20%5BHidden%20Track%5D.mp3";
        Uri radioUri = Uri.parse(url);
        
        
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
        DataSource dataSource = new ShamanDataSource("Meow",  null);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(radioUri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);
        player.prepare(audioRenderer);
        player.setPlayWhenReady(true);
        
        WritableMap props = Arguments.createMap();
        props.putDouble("duration", player.getDuration() * .001);
        callback.invoke(NULL, props);
      }catch(Throwable ex){
        callback.invoke(ex.getMessage());
      }
  }
  
  private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  private static final int BUFFER_SEGMENT_COUNT = 256;
  
  @ReactMethod
  public void play(final Integer key, final Callback callback) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player == null) {
      callback.invoke(false);
      return;
    }
    if (player.isPlaying()) {
      return;
    }
    player.setOnCompletionListener(new OnCompletionListener() {
      @Override
      public void onCompletion(ExoPlayer mp) {
        if (!mp.isLooping()) {
          callback.invoke(true);
        }
      }
    });
    player.setOnErrorListener(new OnErrorListener() {
      @Override
      public boolean onError(ExoPlayer mp, int what, int extra) {
        callback.invoke(false);
        return true;
      }
    });
    player.start();*/
  }

  @ReactMethod
  public void pause(final Integer key) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player != null && player.isPlaying()) {
      player.pause();
    }*/
  }

  @ReactMethod
  public void stop(final Integer key) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player != null && player.isPlaying()) {
      player.stop();
      try {
        player.prepare();
      } catch (Exception e) {
      }
    }*/
  }

  @ReactMethod
  public void release(final Integer key) {
    ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.release();
      this.playerPool.remove(key);
    }
  }

  @ReactMethod
  public void setVolume(final Integer key, final Float left, final Float right) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setVolume(left, right);
    }*/
  }

  @ReactMethod
  public void setLooping(final Integer key, final Boolean looping) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.setLooping(looping);
    }*/
  }

  @ReactMethod
  public void setCurrentTime(final Integer key, final Float sec) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.seekTo((int)Math.round(sec * 1000));
    }*/
  }

  @ReactMethod
  public void getCurrentTime(final Integer key, final Callback callback) {
    /*ExoPlayer player = this.playerPool.get(key);
    if (player == null) {
      callback.invoke(-1, false);
      return;
    }
    callback.invoke(player.getCurrentPosition() * .001, player.isPlaying());*/
  }
}
