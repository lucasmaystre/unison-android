package ch.epfl.hello;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Music player service. Inspired by the Android SDK's sample application,
 * RandomMusicPlayer. We're taking some shortcuts with respect to the sample
 * application, for example, we don't handle remote controls, etc.
 * 
 * @author lum
 */
public class MusicService extends Service
        implements OnCompletionListener, OnPreparedListener, OnErrorListener {
	
	private static final String TAG = "MusicService";
	private static final int NOTIFICATION_ID = 1;
	private static final float DUCK_VOLUME = 0.1f;
	
	public static final String ACTION_PLAY = "ch.epfl.unison.action.PLAY";
	public static final String ACTION_PAUSE = "ch.epfl.unison.action.PAUSE";
	public static final String ACTION_STOP = "ch.epfl.unison.action.STOP";
	public static final String ACTION_TOGGLE_PLAYBACK = "ch.epfl.unison.action.TOGGLE_PLAYBACK";
	public static final String ACTION_LOAD = "ch.epfl.unison.action.LOAD";
	
	private AudioFocusHelper focusHelper;
	private MediaPlayer mediaPlayer;
	private Notification notification;
	
	private UnisonApp app;

	// State variables.
	
	enum State {
		Stopped,   // Media player is stopped.
		Preparing, // Media player is preparing
		Playing,   // Currently playing.
		Paused,    // Paused by user.
	}
	private State state = State.Stopped;
	
	enum AudioFocus {
		NoFocusNoDuck,  // We don't have the focus and can't duck.
		NoFocusCanDuck, // We don't have the focus but can duck.
		Focused,        // We have the focus. Yay!
	}
	private AudioFocus focus = AudioFocus.NoFocusNoDuck;
	
	@Override
	public void onCreate() {
	    this.focusHelper = new AudioFocusHelper(getApplicationContext(), this);
	    this.app = (UnisonApp) this.getApplicationContext();
	}
	
	@Override
	public void onDestroy() {
		// Service is being killed, so make sure we release our resources.
		this.state = State.Stopped;
		this.relaxResources(true);
		this.giveUpAudioFocus();
	}
	
	/**
	 * Called when we receive an intent.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) this.toggle();
        else if (action.equals(ACTION_LOAD)) this.load(intent);
        else if (action.equals(ACTION_PLAY)) this.play();
        else if (action.equals(ACTION_PAUSE)) this.pause();
        else if (action.equals(ACTION_STOP)) this.stop();
        // Don't restart if killed.
		return START_NOT_STICKY;
	}
	
	private void toggle() {
		Log.i(TAG, "blablablablabl");
		if (this.state == State.Paused) {
			this.play();
		} else if (this.state == State.Playing) {
			this.pause();
		}
	}
	
	private void load(Intent intent) {
		Log.i(TAG, "doodlelidoo");
		this.state = State.Stopped;
		this.relaxResources(false);
		this.tryToGetAudioFocus();
		
		try {
		    Uri uri = intent.getData();
		    this.createMediaPlayerIfNeeded();
		    
		    this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		    this.mediaPlayer.setDataSource(this.getApplicationContext(), uri);
		
		    this.state = State.Preparing;
		    // Calls OnPreparedListener when ready.
		    this.mediaPlayer.prepareAsync();
		    
		    this.setUpAsForeground("Unison"); // TODO Change notification text.
		} catch (IOException ioe) {
			Log.e(TAG, "Couldn't load resource.");
		}
	}
	
	private void play() {
		if (this.state == State.Paused) {
		    this.tryToGetAudioFocus();
			this.setUpAsForeground("Unison"); // TODO Change notification text
			this.state = State.Playing;
		    this.configAndStartMediaPlayer();
		}
	}
	
	private void pause() {
		if (this.state == State.Playing) {
			this.state = State.Paused;
			this.mediaPlayer.pause();
			this.relaxResources(false); // Keep audio focus.
		}
	}
	
	private void stop() {
		if (this.state == State.Playing || this.state == State.Paused) {
		    this.relaxResources(true);
		    this.state = State.Stopped;
		    this.giveUpAudioFocus();
		
		    // Service is no longer necessary.
		    this.stopSelf();
		}
	}
	
	/**
	 * Makes sure the media player exists and has been reset. Creates one if it
	 * doesn't exist.
	 */
	private void createMediaPlayerIfNeeded() {
		if (this.mediaPlayer != null) {
			// The MediaPlayer object is already set up. We just reset it.
			this.mediaPlayer.reset();
			return;
		}
		this.mediaPlayer = new MediaPlayer();
		// This means that the screen can go off, but the CPU has to stay running.
		this.mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		// Various events we need to handle.
		this.mediaPlayer.setOnPreparedListener(this);
		this.mediaPlayer.setOnCompletionListener(this);
		this.mediaPlayer.setOnErrorListener(this);
		
		this.app.setMediaPlayer(this.mediaPlayer);
	}
	
    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it.
     */
    void configAndStartMediaPlayer() {
        if (this.focus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause.
            if (this.mediaPlayer.isPlaying()) {
            	this.mediaPlayer.pause(); // Note: the status remains Playing.
            }
        } else if (this.focus == AudioFocus.NoFocusCanDuck) {
        	this.mediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
        	if (!this.mediaPlayer.isPlaying()) {
        		this.mediaPlayer.start();
        	}
        } else { // this.focus == AudioFocus.Focused
            this.mediaPlayer.setVolume(1.0f, 1.0f);
            if (!this.mediaPlayer.isPlaying()) {
            	this.mediaPlayer.start();
            }
        }
    }
	
    private void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        this.notification = new Notification();
        this.notification.tickerText = text;
        this.notification.icon = R.drawable.ic_media_play;
        this.notification.flags |= Notification.FLAG_ONGOING_EVENT;
        this.notification.setLatestEventInfo(getApplicationContext(), "Unison",
                text, pi);
        this.startForeground(NOTIFICATION_ID, this.notification);
    }
    
    /**
     * Releases resources used by the service for playback.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should
     *     also be released or not.
     */
    void relaxResources(boolean releaseMediaPlayer) {
        this.stopForeground(true);

        if (releaseMediaPlayer && this.mediaPlayer != null) {
            this.mediaPlayer.reset();
            this.mediaPlayer.release();
            this.mediaPlayer = null;
            this.app.setMediaPlayer(null);
        }
    }
    
    void tryToGetAudioFocus() {
        if (this.focus != AudioFocus.Focused && this.focusHelper != null
                && this.focusHelper.requestFocus()) {
            this.focus = AudioFocus.Focused;
        }
    }
    
    void giveUpAudioFocus() {
    	if (this.focus == AudioFocus.Focused && this.focusHelper != null
                && this.focusHelper.abandonFocus()) {
    		this.focus = AudioFocus.NoFocusNoDuck;
    	}   
    }
    
    public void onGainedAudioFocus() {
    	this.focus = AudioFocus.Focused;
    	if (this.state == State.Playing) {
    		this.configAndStartMediaPlayer();
    	}
    }
    
    public void onLostAudioFocus(boolean canDuck) {
    	this.focus = canDuck? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
    	if (this.mediaPlayer != null && this.mediaPlayer.isPlaying()) {
    		this.configAndStartMediaPlayer();
    	}
    }

	public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + String.valueOf(what)
        		+ ", extra=" + String.valueOf(extra));

        this.state = State.Stopped;
        this.relaxResources(true);
        this.giveUpAudioFocus();
        return true; // true indicates we handled the error
	}

	public void onPrepared(MediaPlayer mp) {
		this.state = State.Playing;
		this.configAndStartMediaPlayer();
	}

	public void onCompletion(MediaPlayer mp) {
		// TODO do something to get the next song?
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
