package ch.epfl.unison.ui;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import ch.epfl.unison.R;
import ch.epfl.unison.UnisonApp;
import ch.epfl.unison.music.MusicService;

import com.actionbarsherlock.app.SherlockFragment;

public class PlayerFragment extends SherlockFragment implements OnClickListener, Runnable {
    private static final String TAG = "MusicPlayerActivity";

    Button toggle;
    ProgressBar position;

    private UnisonApp app;
    private Handler handler;

    enum Status {
        Stopped,
        Playing,
        Paused
    }
    Status status = Status.Stopped;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.player, container, false);

        this.toggle = (Button) v.findViewById(R.id.musicToggleBtn);
        this.toggle.setOnClickListener(this);

        this.position = (ProgressBar) v.findViewById(R.id.musicPositionBar);

        this.app = (UnisonApp) this.getActivity().getApplicationContext();
        this.handler = new Handler();

        return v;
    }

    public void onClick(View v) {
        if (v == this.toggle) {
            if (this.status == Status.Stopped) {
                this.load();
            } else if (this.status == Status.Playing) {
                this.pause();
            } else { // this.status == Status.Paused)
                this.play();
            }
        }
    }

    private void load() {
        String[] proj = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE };
        // Be careful here, this could return null.
        Cursor c = this.getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        this.getActivity().startManagingCursor(c);
        c.moveToFirst();

        int id = c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID));
        String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));

        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        Log.i(TAG, "The title is -- " + title);

        Intent i = new Intent(MusicService.ACTION_LOAD);
        i.setData(uri);
        //i.putExtra("resource", uri);
        this.getActivity().startService(i);
        handler.post(this);
        //t.isAlive()

        this.status = Status.Playing;
        this.toggle.setBackgroundResource(R.drawable.btn_pause);
    }

    private void pause() {
        this.getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
        this.status = Status.Paused;
        this.toggle.setBackgroundResource(R.drawable.btn_play);
    }

    private void play() {
        this.getActivity().startService(new Intent(MusicService.ACTION_PLAY));
        this.status = Status.Playing;
        this.toggle.setBackgroundResource(R.drawable.btn_pause);
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
