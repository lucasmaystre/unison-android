package ch.epfl.unison;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import ch.epfl.unison.api.UnisonAPI;

public class UnisonApp extends Application
        implements OnSharedPreferenceChangeListener {

    /**
     * This provides a kind of safe way to get some information related to the media player
     * from the MusicService (that owns the MediaPlayer object).
     */
    public MediaPlayer mediaPlayer;

    private UnisonAPI api;
    private SharedPreferences prefs;

    private Long currentRoomId;

    @Override
    public void onCreate() {
        super.onCreate();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public UnisonAPI getAPI() {
        if (this.api == null) {
            String email = this.prefs.getString("email", null);
            String password = this.prefs.getString("password", null);
            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                this.api = new UnisonAPI(email, password);
            } else {
                this.api = new UnisonAPI();
            }
        }
        return this.api;
    }

    public void setCurrentRoom(Long rid) {
        this.currentRoomId = rid;
    }

    public Long getCurrentRoom() {
        return this.currentRoomId;
    }

    public long getUid() {
        return this.prefs.getLong("uid", -1);
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public boolean mediaPlayerIsPlaying() {
        return this.mediaPlayer != null
                ? this.mediaPlayer.isPlaying()
                : false;
    }

    public int mediaPlayerGetDuration() {
        return this.mediaPlayer != null
                ? this.mediaPlayer.getDuration()
                : 0;
    }

    public int mediaPlayerGetCurrentPosition() {
        return this.mediaPlayer != null
                ? this.mediaPlayer.getCurrentPosition()
                : 0;
    }

    public synchronized void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (key.equals("email") || key.equals("password") || key.equals("uid")) {
            this.api = null;
        }
    }
}
