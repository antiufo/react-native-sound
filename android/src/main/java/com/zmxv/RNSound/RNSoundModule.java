package com.zmxv.RNSound;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.net.URL;
import java.io.StringWriter;
import java.io.PrintWriter;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.ExoPlaybackException;
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

import io.shaman.HttpUtils;

public class RNSoundModule extends ReactContextBaseJavaModule {
  Map<String, ExoPlayer> playerPool = new HashMap<>();
  ReactApplicationContext context;
  final static Object NULL = null;

  public RNSoundModule(ReactApplicationContext context) {
    super(context);
    
    if(previousPool != null){
        for(String key : previousPool.keySet()){
            ExoPlayer p = previousPool.get(key);
            p.release();
        }
        previousPool.clear();
    }
    previousPool = this.playerPool;
    this.context = context;
    moduleInstanceId = ++this.lastModuleInstanceId;
  }
  
  static Map<String, ExoPlayer> previousPool;

  @Override
  public String getName() {
    return "RNSound";
  }
  
  private static int lastModuleInstanceId;
  private int moduleInstanceId;
  
  private void sendEvent(String key, String eventName, WritableMap params){
      params.putString("playerId", key);
      params.putString("eventName", eventName);
      context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("playerEvent", params);
  }
  private void sendEvent(String key, String eventName, String value){
      WritableMap params = Arguments.createMap();
      params.putString("value", value);
      sendEvent(key, eventName, params);
  }
  private void sendEvent(String key, String eventName){
      WritableMap params = Arguments.createMap();
      sendEvent(key, eventName, params);
  }
  

  @ReactMethod
  public void prepare(final String fileName, final String key) {
    try{
        
        //Uri url = Uri.parse(fileName);
        ExoPlayer player = ExoPlayer.Factory.newInstance(1);
        this.playerPool.put(key, player);
        
        
        Uri radioUri = Uri.parse(fileName);
        //HttpUtils.setProxy("192.168.1.16", 8888);
        
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        URL url = HttpUtils.asUrl(fileName);
        DataSource dataSource = new ShamanDataSource(url);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(radioUri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);
        player.prepare(audioRenderer);
        PlayerListener listener = new PlayerListener();
        listener.key = key;
        player.addListener(listener);
        sendEvent(key, "created");
      }catch(Throwable ex){
        sendEvent(key, "error", ex.getMessage());
      }
  }
  
  class PlayerListener implements ExoPlayer.Listener {
        String key;
      
        public void onPlayWhenReadyCommitted(){
            sendEvent(key, "playWhenReadyCommitted");      
        }

        public void onPlayerError(ExoPlaybackException exoPlaybackException){
            WritableArray arr = Arguments.createArray();
            Throwable c = exoPlaybackException;
            while(c != null){
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                c.printStackTrace(pw);
                

                arr.pushString(c.getMessage()+"\n"+c.getClass().getCanonicalName()+"\n"+sw.toString());
                c = c.getCause();
            }
            
            WritableMap params = Arguments.createMap();
            params.putArray("causes", arr);
            sendEvent(key, "playerError", params);
        }

        public void onPlayerStateChanged(boolean playWhenReady, int playbackState){
            WritableMap params = Arguments.createMap();
            params.putBoolean("playWhenReady", playWhenReady);
            params.putInt("playbackState", playbackState);
            sendEvent(key, "playerStateChanged", params);
        }      
  }
  
  private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  private static final int BUFFER_SEGMENT_COUNT = 256;
  
  @ReactMethod
  public void play(final String key) {
    ExoPlayer player = this.playerPool.get(key);
    if(player != null) player.setPlayWhenReady(true);
  }

  @ReactMethod
  public void pause(final String key) {
    ExoPlayer player = this.playerPool.get(key);
    if(player != null) player.setPlayWhenReady(false);
  }

  @ReactMethod
  public void release(final String key) {
    ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.release();
      this.playerPool.remove(key);
    }
  }

  @ReactMethod
  public void setCurrentTime(final String key, final Float sec) {
    ExoPlayer player = this.playerPool.get(key);
    if (player != null) {
      player.seekTo((int)Math.round(sec * 1000));
    }
  }

  @ReactMethod
  public void getInfo(final String key, final Callback callback) {
    ExoPlayer player = this.playerPool.get(key);
    if (player == null) {
      callback.invoke("wrongId");
      return;
    }
    callback.invoke(true, player.getCurrentPosition() * .001,
        player.getBufferedPercentage(),
        player.getBufferedPosition(),
        player.getCurrentPosition(),
        player.getDuration(),
        player.getPlayWhenReady(),
        player.getPlaybackState(),
        player.isPlayWhenReadyCommitted());
  }
}
