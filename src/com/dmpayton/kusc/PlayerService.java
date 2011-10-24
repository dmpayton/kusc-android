package com.dmpayton.kusc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
	static final String STREAM_URL = "http://915.kuscstream.org:8000/kuscaudio96.mp3";
	static final String NOWPLAYING_URL = "http://kusc.webplaylist.org/cgi-bin/kusc/wonV6.json";
	private final IBinder mBinder = new LocalBinder();
	private static Timer timer = new Timer();
	
	private StreamProxy proxy;
	MediaPlayer _mp;
	AudioManager audioManager;

	String currentShow = "";
	String currentSong = "";
	private static final int NOTIFICATION_ID = 1;
	PendingIntent contentIntent;
	NotificationManager notificationManager;
	Notification notification;
	
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
		timer.scheduleAtFixedRate(new fetchTrack(), 0, 30*1000);
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
	
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "onCompletion(" + mp + ")");
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		//Log.i(TAG, "onBufferingUpdate(" + mp + ", " + percent + ")");
	}
	
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.i(TAG, "onError(" + mp + ", " + what + ", " + extra + ")");
		return true;
	}
	
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.i(TAG, "onInfo(" + mp + ", " + what + ", " + extra + ")");
		return true;
	}
	
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
	
	public void updateNotification(JSONObject data){
		String updatedShow;
		String updatedSong;

		try {
			if(notification == null || notificationManager == null || contentIntent == null){
				int icon = R.drawable.icon;
				CharSequence ticker = "Unofficial KUSC Android Player";
				notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				notification = new Notification(icon, ticker, System.currentTimeMillis());
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				Intent intent = new Intent(this, PlayerActivity.class);
				contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
			}

			try {
				JSONObject show = data.getJSONObject("show");
				updatedShow = show.getString("title") + " " + show.getString("sub_title");
				updatedSong = "";
				try {
					JSONObject track = data.getJSONObject("track");
					try{ updatedSong += track.getString("composer") + ": "; } catch (JSONException e) {}
					try{ updatedSong += " " + track.getString("title"); } catch (JSONException e) {}
					try{ updatedSong += " " + track.getString("in_key"); } catch (JSONException e) {}
					try{ updatedSong += " " + track.getString("opus"); } catch (JSONException e) {}
					try{ updatedSong += " " + track.getString("catalog"); } catch (JSONException e) {}
				} catch (JSONException e) {
					Log.e(TAG, "Error building track.");
					e.printStackTrace();
				}
			} catch (JSONException e) {
				updatedShow = "Error retrieving track info.";
				updatedSong = "";
			}
			if(currentShow != updatedShow || currentSong != updatedSong){
				currentShow = updatedShow;
				currentSong = updatedSong;
				notification.setLatestEventInfo(getApplicationContext(), (CharSequence) currentShow, (CharSequence) currentSong, contentIntent);
				notificationManager.notify(NOTIFICATION_ID, notification);
			}
		} catch (Exception e) {
			Log.e(TAG, "updateNotification Failure");
			e.printStackTrace();
		}
	}
	
	
	private class fetchTrack extends TimerTask
    { 
        public void run() 
        {
        	try {
				URL url = new URL(NOWPLAYING_URL);
				URLConnection conn = url.openConnection();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(50);

                int current = 0;
                while((current = bis.read()) != -1){
                    baf.append((byte)current);
                }

                /* Convert the Bytes read to a String. */
                String response = new String(baf.toByteArray());
	            JSONObject data = new JSONArray(response.substring(13, response.length()-1)).getJSONObject(0);
	            updateNotification(data);
			    
			} catch (MalformedURLException e) {
				Log.e(TAG, "MalformedURLException in Fetch Track");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, "IOException in Fetch Track");
				e.printStackTrace();
			} catch (JSONException e) {
				Log.e(TAG, "JSONException in Fetch Track");
				e.printStackTrace();
			}
        }
    }    
}
