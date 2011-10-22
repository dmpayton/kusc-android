package com.dmpayton.kusc;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.dmpayton.kusc.StreamProxy;

public class PlayerService extends Service implements OnBufferingUpdateListener, OnCompletionListener, 
	OnErrorListener, OnInfoListener, OnPreparedListener {
	static final String TAG = "KUSCService";
	static final String STREAM_URL = "http://915.kuscstream.org:8000/kuscaudio128.mp3";
	//static final String STREAM_URL = "http://www.dmpayton.com/jackyl-lumberjack.mp3";
	private final IBinder mBinder = new LocalBinder();
	
	private StreamProxy proxy;
	MediaPlayer _mp;
	AudioManager audioManager;
	
	public class LocalBinder extends Binder {
		PlayerService getService() {
			// Return this instance of LocalService so clients can call public methods
			return PlayerService.this;
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "Service closing");
		killMediaPlayer();
		super.onDestroy();
	}
	
	public void initMediaPlayer() {
		if(_mp != null){
			killMediaPlayer();
		}
		
		String playUrl = STREAM_URL;
		int sdkVersion = 0;
		try {
			sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException ignored) {
		}
		
		if(sdkVersion < 8) {
			if(proxy == null) {
				proxy = new StreamProxy();
				proxy.init();
				proxy.start();
			}
			playUrl = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), STREAM_URL);
		}
		
		Log.i(TAG, "Streaming from " + playUrl);
		
		_mp = new MediaPlayer();
        try {
        	_mp.setDataSource(playUrl);
    		_mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    		_mp.setOnBufferingUpdateListener(this);
    		_mp.setOnErrorListener(this);
    		_mp.setOnInfoListener(this);
    		_mp.setOnPreparedListener(this);
			_mp.prepare();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "SOMETHING BAD HAPPENED");
		}
	}
	
	public void killMediaPlayer() {
		if(_mp != null) {
			if(_mp.isPlaying()){
				_mp.stop();
			}
			_mp.release();
			_mp = null;
		}
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "onCompletion(" + mp + ")");
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.i(TAG, "onBufferingUpdate(" + mp + ", " + percent + ")");
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.i(TAG, "onError(" + mp + ", " + what + ", " + extra + ")");
		return true;
	}
	
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.i(TAG, "onInfo(" + mp + ", " + what + ", " + extra + ")");
		return true;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.i(TAG, "onPrepared(" + mp + ")");
	}
	
	
	public void pauseMP() {
		if(_mp != null && _mp.isPlaying()){
			_mp.pause();
		}
	}

	public void startMP() {
		if(_mp != null && !_mp.isPlaying()){
			_mp.start();
		}
	}
	
	public boolean isPlaying() {
		if(_mp != null){
			return _mp.isPlaying();
		}
		return false;
	}
}
