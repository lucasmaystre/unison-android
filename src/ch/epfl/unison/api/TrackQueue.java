package ch.epfl.unison.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import ch.epfl.unison.AppData;
import ch.epfl.unison.MusicItem;
import ch.epfl.unison.api.JsonStruct.Track;
import ch.epfl.unison.api.UnisonAPI.Error;

public class TrackQueue {

    private static final String TAG = "ch.epfl.unison.TrackQueue";

    private static final int QUEUE_SIZE = 1;
    private static final int SLEEP_INTERVAL = 2000; // in ms.

    /** Number of attempts to get an element from the queue **/
    private static final int MAX_RETRIES = 15;  // To be multiplied by SLEEP_INTERVAL.

    /** Max number of requests to get a track from the server */
    private static final int MAX_REQUESTS = 10;

    private List<MusicItem> queue;
    private int pending;

    private Context context;
    private long roomId;

    public TrackQueue(Context context, long rid) {
        this.queue = Collections.synchronizedList(new ArrayList<MusicItem>());
        this.pending = 0;
        this.context = context;
        this.roomId = rid;
    }

    /** Populate the track queue. */
    public TrackQueue init() {
        this.ensureEnoughElements();
        return this;
    }

    public void get(final Callback clbk) {
        this.ensureEnoughElements();
        try {
            MusicItem item = this.queue.remove(0);
            this.requestTrack();
            clbk.callback(item);
        } catch(IndexOutOfBoundsException e) {
            new AsyncTask<Void, Void, MusicItem>() {

                @Override
                protected MusicItem doInBackground(Void... nothing) {
                    for (int i = 0; i < MAX_RETRIES; ++i) {
                        try {
                            return TrackQueue.this.queue.remove(0);
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
                        TrackQueue.this.requestTrack();
                        clbk.callback(item);
                    } else {
                        clbk.onError();
                    }
                }
            }.execute();
        }
    }

    private synchronized void ensureEnoughElements() {
        int nbMissing = QUEUE_SIZE - (this.queue.size() + this.pending);
        for (int i = 0; i < nbMissing; ++i) {
            this.requestTrack();
        }
    }

    private void requestTrack() {
        this.requestTrack(MAX_REQUESTS);
    }

    private void requestTrack(final int trials) {
        if (trials == 0) {
            return;
        }

        UnisonAPI api = AppData.getInstance(this.context).getAPI();
        this.pending += 1;
        api.getNextTrack(this.roomId, new UnisonAPI.Handler<JsonStruct.Track>() {

            public void callback(Track track) {
                TrackQueue.this.pending -= 1;
                TrackQueue.this.queue.add(
                        new MusicItem(track.localId, track.artist, track.title));
            }

            public void onError(Error error) {
                Log.d(TAG, error.toString());
                TrackQueue.this.pending -= 1;
                TrackQueue.this.requestTrack(trials - 1);
            }
        });
    }

    public static interface Callback {
        public void callback(MusicItem item);
        public void onError();
    }
}
