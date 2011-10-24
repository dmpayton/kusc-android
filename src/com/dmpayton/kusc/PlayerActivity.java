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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.dmpayton.kusc.PlayerService.LocalBinder;


public class PlayerActivity extends Activity implements Runnable, ServiceConnection, OnSeekBarChangeListener {
	
	/* TODO:
	 * Monitor phone state to pause/resume for calls
	 * Ongoing notification
	 * About information
	 */
	
    private static final String TAG = "KUSCActivity";
    private static String DONATE_URL = "http://kuscinteractive.org/donate";
    boolean isBound = false;
    boolean isLoading = false;

    private AudioManager audioManager;
    private ImageButton buttonMediaControl;
    private PlayerService mService;
    private SeekBar volumeSlider;
    private Handler handler = new Handler();
    private Timer timer = new Timer();
	
    
    /*
     * Activity
     */
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        buttonMediaControl = (ImageButton) findViewById(R.id.buttonMediaControl);
        
        // Volume stuff
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        
        volumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
        volumeSlider.setMax(maxVol);
        volumeSlider.setProgress(curVol);
        volumeSlider.setOnSeekBarChangeListener(this);
        
        handler.postDelayed(this, 1000);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Intent intent = new Intent(this, PlayerService.class);
    	bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
    
	public void run() {
		if(isBound) {
    		TextView show = (TextView) findViewById(R.id.show);
    		TextView song = (TextView) findViewById(R.id.song);
        	if(isBound) {
        		show.setText(mService.currentShow);
        		song.setText(mService.currentSong);
        	} else {
        		show.setText((String) "");
        		song.setText((String) "");
        	}
			handler.postDelayed(this, 5000);
		}
	}
    
    /*
     * Menu
     */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuDonate) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL));
			startActivity(browserIntent);
			return true;
		/* } else if (item.getItemId() == R.id.menuAbout) {
			return true;*/
		} else if (item.getItemId() == R.id.menuQuit) {
			doQuit();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }

    
    /*
     * ServiceConnection
     */
    
  	public void onServiceConnected(ComponentName className, IBinder service) {
   		LocalBinder binder = (LocalBinder) service;
   		mService = binder.getService();
   		isBound = true;
   	}
    	
   	public void onServiceDisconnected(ComponentName arg0) {
   		isBound = false;
   	}
   	
   	
   	/*
   	 * OnSeekBarChangeListener
   	 */
   	
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
	
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		// TODO Auto-generated method stub
		if(isBound){
			mService.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
		}
	}
   	
    /*
     * User Actions
     */
   	
    public void togglePlayPause(View view) {
    	if(isBound) {
    		if(mService.isPlaying()){
    			mService.pauseMP();
    			buttonMediaControl.setImageResource(R.drawable.button_play);
    		} else {
    			// No MediaPlayer set, display a loading screen while it loads
    			if(mService._mp == null) {
        			//ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", true, false);
        			mService.initMediaPlayer();
        			mService.startMP();
        			//dialog.dismiss();
        		} else {
        			mService.startMP();
        		}
    			
    			buttonMediaControl.setImageResource(R.drawable.button_pause);
    		}
    	}
    }
    
    public void doQuit() {
    	Log.i(TAG, "Quitting");
    	if(isBound) {
    		unbindService(this);
    		isBound = false;
    	}
    	PlayerActivity.this.finish();
    }
}