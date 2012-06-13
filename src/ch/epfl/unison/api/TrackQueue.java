package ch.epfl.unison.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import ch.epfl.unison.AppData;
import ch.epfl.unison.MusicItem;
import ch.epfl.unison.api.JsonStruct.TracksList;
import ch.epfl.unison.api.UnisonAPI.Error;

public class TrackQueue {

    private static final String TAG = "ch.epfl.unison.TrackQueue";

    private static final int SLEEP_INTERVAL = 2000; // in ms.
    private static final int POLL_INTERVAL = 30000; // in ms.

    /** Number of attempts to get an element from the queue **/
    private static final int MAX_RETRIES = 15;  // To be multiplied by SLEEP_INTERVAL.

    /** Max number of requests to get a track from the server */
    private static final int MAX_REQUESTS = 10;

    private Set<MusicItem> playlist;  // Is a set, but insertion order is important!
    private String playlistId;
    private int nextPtr;

    private boolean isActive;
    private boolean isPending;
    private Handler handler;

    private Context context;
    private long roomId;

    public TrackQueue(Context context, long rid) {
        // LinkedHashSet returns insert-order iterators.
        this.playlist = Collections.synchronizedSet(
                new LinkedHashSet<MusicItem>());
        this.isPending = false;
        this.isActive = false;
        this.handler = new Handler();
        this.context = context;
        this.roomId = rid;
    }

    /** Populate the track queue, and start polling for changes. */
    public TrackQueue start() {
        this.isActive = true;
        this.ensureEnoughElements();
        this.handler.postDelayed(new Poller(), POLL_INTERVAL);
        return this;
    }

    public void stop() {
       this.isActive = false;
       this.playlist.clear();
       this.nextPtr = 0;
    }

    public void get(final Callback clbk) {
        if (!this.isActive) {
            throw new RuntimeException("track queue is inactive");
        }

        this.ensureEnoughElements();
        new AsyncTask<Void, Void, MusicItem>() {

            @Override
            protected MusicItem doInBackground(Void... nothing) {
                for (int i = 0; i < MAX_RETRIES; ++i) {
                    try {
                        MusicItem next = new LinkedList<MusicItem>(playlist).get(nextPtr);
                        nextPtr += 1;
                        return next;
                    } catch (IndexOutOfBoundsException e) {}
                    try {
                        Thread.sleep(SLEEP_INTERVAL);
                    } catch (InterruptedException e) {}
                }
                // We declare defeat.
                return null;
            }

            @Override
            protected void onPostExecute(MusicItem item) {
                if (item != null) {
                    ensureEnoughElements();
                    clbk.callback(item);
                } else {
                    clbk.onError();
                }
            }
        }.execute();
    }

    private synchronized void ensureEnoughElements() {
        if (!this.isPending && this.playlist.size() - this.nextPtr < 1) {
            this.requestTracks();
        }
    }

    private void requestTracks() {
        this.requestTracks(MAX_REQUESTS);
    }

    private void requestTracks(final int trials) {
        if (trials == 0) {
            this.isPending = false;
            return;
        }

        UnisonAPI api = AppData.getInstance(this.context).getAPI();
        this.isPending = true;
        api.getNextTracks(this.roomId, new UnisonAPI.Handler<JsonStruct.TracksList>() {

            public void callback(JsonStruct.TracksList chunk) {
                isPending = false;
                if (chunk.tracks != null && chunk.playlistId != null) {
                    if (!chunk.playlistId.equals(playlistId)) {
                        // The playlist has changed - reset it.
                        playlistId = chunk.playlistId;
                        nextPtr = 0;
                        playlist.clear();
                    }
                    for (JsonStruct.Track track : chunk.tracks) {
                        playlist.add(new MusicItem(track.localId, track.artist, track.title));
                    }
                }
            }

            public void onError(UnisonAPI.Error error) {
                Log.d(TAG, error.toString());
                requestTracks(trials - 1);
            }
        });
    }

    public static interface Callback {
        public void callback(MusicItem item);
        public void onError();
    }

    public class Poller implements Runnable {

        public void run() {
            if (!isActive) { return; }

            UnisonAPI api = AppData.getInstance(context).getAPI();
            api.getPlaylistId(roomId, new UnisonAPI.Handler<JsonStruct.TracksList>() {

                public void callback(TracksList struct) {
                    if (struct.playlistId != null
                            && !struct.playlistId.equals(playlistId)) {
                        requestTracks();
                    }
                    handler.postDelayed(Poller.this, POLL_INTERVAL);
                }

                public void onError(Error error) {
                    handler.postDelayed(Poller.this, POLL_INTERVAL);
                }
            });
        }
    }
}
