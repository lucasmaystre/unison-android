package ch.epfl.hello;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class MusicPlayerActivity extends Activity implements OnClickListener, Runnable {
	private static final String TAG = "MusicPlayerActivity";
	
	Button play;
	Button pause;
	ProgressBar position;
	
	private UnisonApp app;
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    this.setContentView(R.layout.musicplayer);
	    
	    play = (Button) this.findViewById(R.id.musicToggleBtn);
	    play.setOnClickListener(this);
	    
	    pause = (Button) this.findViewById(R.id.musicNextBtn);
	    pause.setOnClickListener(this);
	    
	    this.position = (ProgressBar) this.findViewById(R.id.musicPositionBar);
	    
	    this.app = (UnisonApp) this.getApplicationContext();
	    this.handler = new Handler();
	}

	public void onClick(View v) {
		if (v == play) {
			this.play();
		} else if (v == pause) {
			this.toggle();
		}
	}
	
	public void play() {
        String[] proj = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE };
        // Be careful here, this could return null.
        Cursor c = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        this.startManagingCursor(c);
        c.moveToFirst();
        
        int id = c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID));
        String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
        
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        Log.i(TAG, "The title is -- " + title);
        
        Intent i = new Intent(MusicService.ACTION_LOAD);
        i.setData(uri);
        //i.putExtra("resource", uri);
        this.startService(i);
        handler.post(this);
        //t.isAlive()
	}
	
	public void toggle() {
		this.startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
	}

	public void run() {
		Log.w(TAG, "kasldjlaskjdlaksjdlskjd");
		if (this.app.mediaPlayerIsPlaying()) {
			this.position.setMax(this.app.mediaPlayerGetDuration());
			this.position.setProgress(this.app.mediaPlayerGetCurrentPosition());
			this.handler.postDelayed(this, 400);
		} else {
		    this.handler.postDelayed(this, 5000);
		}
	}
}
