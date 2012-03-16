package ch.epfl.hello;

import android.app.Application;
import android.media.MediaPlayer;

public class UnisonApp extends Application {

    /**
     * This provides a kind of safe way to get some information related to the media player
     * from the MusicService (that owns the MediaPlayer object).
     */
    public MediaPlayer mediaPlayer;

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
}
