package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.MusicItem;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.TrackQueue;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.music.MusicService;
import ch.epfl.unison.music.MusicService.MusicServiceBinder;

import com.actionbarsherlock.app.SherlockFragment;

public class PlayerFragment extends SherlockFragment implements OnClickListener,
        MainActivity.OnRoomInfoListener {

    private static final String TAG = "ch.epfl.unison.PlayerFragment";
    private static final int CLICK_INTERVAL = 5 * 1000;  // In milliseconds.

    private MainActivity activity;

    private Button djBtn;
    private Button ratingBtn;
    private Button toggleBtn;
    private Button nextBtn;
    private Button prevBtn;

    private View buttons;
    private TextView artistTxt;
    private TextView titleTxt;
    private ImageView coverImg;

    private boolean isDJ;

    private TrackQueue trackQueue;

    private MusicItem currentTrack;
    private List<MusicItem> history;
    private int histPointer;

    enum Status {
        Stopped,
        Playing,
        Paused
    }
    Status status = Status.Stopped;

    private BroadcastReceiver completedReceiver = new TrackCompletedReceiver();
    private MusicServiceBinder musicService;
    private boolean isBound;
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            musicService = (MusicServiceBinder) service;
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.player, container, false);

        this.toggleBtn = (Button) v.findViewById(R.id.musicToggleBtn);
        this.toggleBtn.setOnClickListener(this);
        this.nextBtn = (Button) v.findViewById(R.id.musicNextBtn);
        this.nextBtn.setOnClickListener(this);
        this.prevBtn = (Button) v.findViewById(R.id.musicPrevBtn);
        this.prevBtn.setOnClickListener(this);
        this.djBtn = (Button) v.findViewById(R.id.djToggleBtn);
        this.djBtn.setOnClickListener(this);
        this.ratingBtn = (Button) v.findViewById(R.id.ratingBtn);
        this.ratingBtn.setOnClickListener(new OnRatingClickListener());

        this.buttons = v.findViewById(R.id.musicButtons);

        this.artistTxt = (TextView) v.findViewById(R.id.musicArtist);
        this.titleTxt = (TextView) v.findViewById(R.id.musicTitle);
        this.coverImg = (ImageView) v.findViewById(R.id.musicCover);

        this.history = new ArrayList<MusicItem>();
        this.histPointer = 0;

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
        this.activity.registerRoomInfoListener(this);
        this.activity.registerReceiver(this.completedReceiver,
                new IntentFilter(MusicService.ACTION_COMPLETED));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity.unregisterRoomInfoListener(this);
        this.activity.unregisterReceiver(this.completedReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!this.isDJ) {
            // Just to make sure, when the activity is recreated.
            this.buttons.setVisibility(View.INVISIBLE);
            this.djBtn.setText("Become the DJ");
        }
        this.activity.bindService(new Intent(this.activity, MusicService.class),
                this.connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.isBound) {
            this.activity.unbindService(this.connection);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().startService(new Intent(MusicService.ACTION_STOP));
    }

    public void onRoomInfo(JsonStruct.Room roomInfo) {
        // Check that we're consistent with respect to the DJ position.
        Long uid = AppData.getInstance(this.activity).getUid();
        if (!this.isDJ && roomInfo.master != null && uid.equals(roomInfo.master.uid)) {
            this.setDJ(true);
        } else if (this.isDJ && (roomInfo.master == null || !uid.equals(roomInfo.master.uid))) {
            this.setDJ(false);
        }

        // Update track information.
        if (roomInfo.track != null) {
            this.artistTxt.setText(roomInfo.track.artist);
            this.titleTxt.setText(roomInfo.track.title);
            this.currentTrack = new MusicItem(
                    -1, roomInfo.track.artist, roomInfo.track.title);
            if (roomInfo.track.image != null) {
                Uutils.setBitmapFromURL(this.coverImg, roomInfo.track.image);
            } else {
                this.coverImg.setImageResource(R.drawable.cover);
            }
        } else {
            this.currentTrack = null;
            this.coverImg.setImageResource(R.drawable.cover);
        }
    }

    public void onClick(View v) {
        if (v == this.toggleBtn) {
            if (this.status == Status.Stopped) {
                this.next();
            } else  { // Paused or Playing.
                this.toggle();
            }
        } else if (v == this.nextBtn) {
            this.next();
        } else if (v == this.prevBtn) {
            this.prev();
        } else if (v == this.djBtn) {
            Log.d(TAG, "Clicked DJ button");
            this.setDJ(!this.isDJ);
        }
    }

    private void prev() {
        int curPos = this.isBound ? this.musicService.getCurrentPosition() : 0;
        if (curPos < CLICK_INTERVAL && this.histPointer < this.history.size() - 1) {
            // We play the *previous* track.
            this.histPointer += 1;
            this.play(this.history.get(this.histPointer));
        } else if (this.histPointer < this.history.size()) {
            // We just restart the current track.
            this.play(this.history.get(this.histPointer));
        }
    }

    private void next() {
        if (!this.history.isEmpty() && this.histPointer == 0
                && (this.status == Status.Playing || this.status == Status.Paused)) {
            // We're skipping a song that is heard for the first time. Notify the server.
            this.notifySkip();
        }

        if (this.histPointer > 0) {
            this.histPointer -= 1;
            this.play(this.history.get(this.histPointer));
        } else {
            // We need a new track.
            this.trackQueue.get(new TrackQueue.Callback() {

                public void callback(MusicItem item) {
                    history.add(0, item);
                    play(item);
                }

                public void onError() {
                    Toast.makeText(getActivity(), "Error when getting track", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void toggle() {
        if (this.status == Status.Playing) {
            this.getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
            this.status = Status.Paused;
            this.toggleBtn.setBackgroundResource(R.drawable.btn_play);
        } else if (this.status == Status.Paused) {
            this.getActivity().startService(new Intent(MusicService.ACTION_PLAY));
            this.status = Status.Playing;
            this.toggleBtn.setBackgroundResource(R.drawable.btn_pause);
        }
    }

    private void play(MusicItem item) {
        Log.i(TAG, String.format("playing %s - %s", item.artist, item.title));
        // Send the song to the music player service.
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.localId);
        this.activity.startService(new Intent(MusicService.ACTION_LOAD).setData(uri));
        this.currentTrack = item;
        this.status = Status.Playing;

        // Update the interface.
        this.toggleBtn.setBackgroundResource(R.drawable.btn_pause);
        this.coverImg.setImageResource(R.drawable.cover);
        this.artistTxt.setText(item.artist);
        this.titleTxt.setText(item.title);

        // Notify the server.
        UnisonAPI api = AppData.getInstance(this.activity).getAPI();
        api.setCurrentTrack(this.activity.getRoomId(), item.artist, item.title,
                new UnisonAPI.Handler<JsonStruct.Success>() {

            public void callback(JsonStruct.Success struct) {
                // Automatically refresh the content (in particular, to get the cover art).
                activity.onRefresh();
            }

            public void onError(Error error) {
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void notifySkip() {
        UnisonAPI api = AppData.getInstance(this.activity).getAPI();
        api.skipTrack(this.activity.getRoomId(), new UnisonAPI.Handler<JsonStruct.Success>() {
            public void callback(JsonStruct.Success struct) {}
            public void onError(Error error) {
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setDJ(boolean isDJ) {
        final long rid = ((MainActivity) this.getActivity()).getRoomId();
        AppData data = AppData.getInstance(getActivity());

        if (isDJ) {
            data.getAPI().becomeMaster(rid, data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

                public void callback(Success structure) {
                    djBtn.setText("Leave DJ seat");
                    toggleBtn.setBackgroundResource(R.drawable.btn_play);
                    buttons.setVisibility(View.VISIBLE);

                    trackQueue = new TrackQueue(getActivity(), rid).start();
                }

                public void onError(Error error) {
                    Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
                    PlayerFragment.this.setDJ(false);
                }
            });

        } else {
            if (this.trackQueue != null) {
                this.trackQueue.stop();
            }
            data.getAPI().resignMaster(rid, data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

                public void callback(Success structure) {
                    djBtn.setText("Become the DJ");
                    buttons.setVisibility(View.INVISIBLE);

                    getActivity().startService(new Intent(MusicService.ACTION_STOP));
                    status = Status.Stopped;
                }

                public void onError(Error error) {
                    Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
        this.isDJ = isDJ;
    }

    private class OnRatingClickListener implements OnClickListener {

        public void onClick(View v) {
            if (currentTrack == null) {
                return;
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle("Rate this song");
            alert.setMessage("How do you like this song ?");

            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rating_dialog, null);
            final RatingBar bar = (RatingBar) layout.findViewById(R.id.ratingBar);

            alert.setView(layout);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    if (currentTrack != null) {
                        final int newRating = Math.max((int)bar.getRating(), 1);

                        UnisonAPI api = AppData.getInstance(getActivity()).getAPI();
                        Log.d(TAG, String.format("artist: %s, title: %s, rating: %d",
                                currentTrack.artist, currentTrack.title, newRating));
                        api.instantRate(activity.getRoomId(), currentTrack.artist, currentTrack.title,
                                newRating, new UnisonAPI.Handler<JsonStruct.Success>(){
                            public void callback(JsonStruct.Success struct) {}
                            public void onError(Error error) {
                                Toast.makeText(getActivity(), error.toString(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });

            alert.setNegativeButton("Cancel", null);
            alert.show();
        }
    }

    private class TrackCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            PlayerFragment.this.status = Status.Stopped;
            Log.i(TAG, "track has completed, send the next one.");
            PlayerFragment.this.next();
        }

    }
}
