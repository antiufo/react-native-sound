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
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
  public void extractData(String url, ReadableMap map, Callback callback){
      try{
          Element page = HttpUtils.getHtmlNode(HttpUtils.asUrl(url));
          
          callback.invoke(extractData(page, map));
          
      }catch(Exception ex){
          WritableMap params = Arguments.createMap();
          params.putArray("causes", serializeException(ex));
          callback.invoke(null, params);
      }
  }
  
  private Object extractData(Element root, ReadableMap map) {
      if(map.hasKey("itemSelector")) {
          
          Elements elements = root.select(map.getString("itemSelector"));
          WritableArray arr = Arguments.createArray();
          
          for (Element element : elements) {
              arr.pushMap(extractSingleItem(element, map));
          }
          return arr;
      }
      return extractSingleItem(root, map);
  }
  
  private WritableMap extractSingleItem(Element element, ReadableMap map) {
      WritableMap result = Arguments.createMap();
      ReadableMapKeySetIterator iterator = map.keySetIterator();
      while(iterator.hasNextKey()) {
          String key = iterator.nextKey();
          if(key.equals("itemSelector")) continue;
           ReadableType type = map.getType(key);
           
           String selector = null;
           String attribute = null;
           String regex = null;
           
           if(type == ReadableType.String) {
               selector = map.getString(key);
           } else if( type == ReadableType.Array) {
               ReadableArray args = map.getArray(key);
               selector = getAtOrDefault(args, 0);
               attribute = getAtOrDefault(args, 1);
               regex = getAtOrDefault(args, 2);
           } else if (type == ReadableType.Map) {
               ReadableMap submap = map.getMap(key);
               if(!submap.hasKey("itemSelector")) throw new RuntimeException("Invalid extraction specification. Expected itemSelector.");
               result.putArray(key, (WritableArray)extractData(element, submap));
               continue;
           } else throw new RuntimeException("Invalid extraction specification.");
          
          String value = HttpUtils.tryGetValue(element, selector, attribute, regex); 
          result.putString(key, value);
      }
      
      return result;
  }
  
  public String getAtOrDefault(ReadableArray arr, int index){
      if(index < arr.size()){
          return arr.getString(index);
      }
      return null;
  }
  
  private WritableArray serializeException(Throwable c){
      WritableArray arr = Arguments.createArray();
      while(c != null){
                
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            c.printStackTrace(pw);
                
            WritableMap z = Arguments.createMap();
            z.putString("message", c.getMessage());
            z.putString("type", c.getClass().getCanonicalName());
            z.putString("stackTrace", sw.toString());
            arr.pushMap(z);
            c = c.getCause();
      }
            
      return arr;
  }
  
  @ReactMethod
  public void setProxy(String host, int port) {
    if(host == null) HttpUtils.setProxy(null);
    else HttpUtils.setProxy(host, port);
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
        WritableMap params = Arguments.createMap();
        params.putArray("causes", serializeException(ex));
        sendEvent(key, "error", params);
      }
  }
  
  class PlayerListener implements ExoPlayer.Listener {
        String key;
      
        public void onPlayWhenReadyCommitted(){
            sendEvent(key, "playWhenReadyCommitted");      
        }

        public void onPlayerError(ExoPlaybackException exoPlaybackException){
            WritableMap params = Arguments.createMap();
            params.putArray("causes", serializeException(exoPlaybackException));
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
