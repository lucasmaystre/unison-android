package ch.epfl.unison.music;

import android.content.Context;
import android.media.AudioManager;

/**
 * Small helper class that deals with audio focus. Inspired by the Android SDK's
 * sample application, RandomMusicPlayer.
 *
 * @author lum
 *
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

    AudioManager audioManager;
    MusicService musicService;

    public AudioFocusHelper(Context context, MusicService musicService) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.musicService = musicService;
    }

    public boolean requestFocus() {
        return this.audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public boolean abandonFocus() {
        return this.audioManager.abandonAudioFocus(this) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void onAudioFocusChange(int focusChange) {
        if (this.musicService == null) {
            return;
        }

        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            musicService.onGainedAudioFocus();
            break;
        case AudioManager.AUDIOFOCUS_LOSS:
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            this.musicService.onLostAudioFocus(false);
            break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            this.musicService.onLostAudioFocus(true);
            break;
        default:  // Should never happen.
            break;
        }

    }
}
