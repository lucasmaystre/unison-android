package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryHelper;
import ch.epfl.unison.MusicItem;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;

public class RatingsActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.RatingsActivity";

    private ListView musicList;
    private CheckBox unratedCheck;

    private Map<String, Integer> ratings;
    private List<MusicItem> items;

    private boolean unratedOnly;

    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity should finish on logout.
        this.registerReceiver(this.logoutReceiver,
                new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        this.setContentView(R.layout.ratings);
        this.setTitle(R.string.activity_title_ratings);

        this.musicList = (ListView) this.findViewById(R.id.listMusicList);
        this.musicList.setOnItemClickListener(new OnChangeRatingListener());

        this.unratedCheck = (CheckBox) this.findViewById(R.id.unratedCheckBox);
        this.unratedCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                unratedOnly = isChecked;
                refreshList();
            }
        });

        this.initItems();
        this.initRatings(new Runnable() {
            public void run() {
                refreshList();
                unratedCheck.setEnabled(true);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.logoutReceiver);
    }

    private void initRatings(final Runnable clbk) {
        AppData data = AppData.getInstance(this);
        data.getAPI().getRatings(data.getUid(), new UnisonAPI.Handler<JsonStruct.TracksList>() {

            public void callback(JsonStruct.TracksList struct) {
                ratings = new HashMap<String, Integer>();
                for (JsonStruct.Track t : struct.tracks) {
                    ratings.put(t.artist + t.title, t.rating);
                }
                clbk.run();
            }

            public void onError(Error error) {
                Toast.makeText(RatingsActivity.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initItems() {
        LibraryHelper helper = new LibraryHelper(this);
        this.items = new ArrayList<MusicItem>(helper.getEntries());
        Collections.sort(this.items);
        helper.close();
    }

    private void refreshList() {
        this.refreshList(0);
    }

    private void refreshList(int position) {
        if (this.unratedOnly) {
            List<MusicItem> filtered = new ArrayList<MusicItem>();
            for (MusicItem item : this.items) {
                if (!this.ratings.containsKey(item.artist + item.title)) {
                    filtered.add(item);
                }
            }
            this.musicList.setAdapter(new RatingsAdapter(filtered));
        } else {
            this.musicList.setAdapter(new RatingsAdapter(this.items));
        }
        this.musicList.setSelection(position);
    }

    private class RatingsAdapter extends ArrayAdapter<MusicItem> {

        public static final int ROW_LAYOUT = R.layout.ratings_row;

        private RatingsActivity that;

        public RatingsAdapter(List<MusicItem> ratings) {
            super(RatingsActivity.this, 0, ratings);
            that = RatingsActivity.this;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) that.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            String artist = this.getItem(position).artist;
            String title = this.getItem(position).title;
            Integer rating = that.ratings.get(artist + title);

            ((TextView) view.findViewById(R.id.artistMusicList)).setText(artist);
            ((TextView) view.findViewById(R.id.textMusicList)).setText(title);
            ((RatingBar) view.findViewById(R.id.ratingBarMusicList))
                    .setRating(rating != null ? rating : 0);

            view.setTag(this.getItem(position));
            return view;
        }
    }

    private class OnChangeRatingListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, final int position, long id)  {
            final MusicItem item = (MusicItem) view.getTag();
            final int oldRating = ratings.get(item.artist + item.title) != null
                    ? ratings.get(item.artist + item.title) : 0;
            AlertDialog.Builder alert = new AlertDialog.Builder(RatingsActivity.this);

            alert.setTitle(item.title);
            alert.setMessage("How do you like this song ?");

            LayoutInflater inflater = (LayoutInflater) RatingsActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rating_dialog, null);
            final RatingBar bar = (RatingBar) layout.findViewById(R.id.ratingBar);
            bar.setRating(oldRating);

            alert.setView(layout);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    final int newRating = Math.max((int)bar.getRating(), 1);
                    if (newRating != oldRating) {
                        AppData data = AppData.getInstance(RatingsActivity.this);
                        data.getAPI().rate(data.getUid(), item.artist, item.title, newRating,
                                new UnisonAPI.Handler<JsonStruct.Success>() {

                            public void callback(JsonStruct.Success struct) {
                                ratings.put(item.artist + item.title, newRating);
                                refreshList(position);
                            }

                            public void onError(Error error) {
                                Toast.makeText(RatingsActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });

            alert.setNegativeButton("Cancel", null);
            alert.show();
        }
    }
}
