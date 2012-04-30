package ch.epfl.unison.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

public class RatingsActivity extends SherlockActivity implements RatingBar.OnRatingBarChangeListener {

    private SimpleCursorAdapter adapter;
    private ListView listMusicList;

    private static final String[] FROM = {
        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media._ID };
    private static final int[] TO = { R.id.textMusicList, R.id.artistMusicList, R.id.ratingBarMusicList };

    private final ViewBinder viewBinder = new ViewBinder() {
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (view.getId() != R.id.ratingBarMusicList)
                return false;
            int n = (int)(6 * Math.random());
            ((RatingBar) view).setRating(n);
            ((RatingBar) view).setOnRatingBarChangeListener(RatingsActivity.this);
            view.setTag(cursor.getInt(columnIndex));
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.ratings);
        this.setTitle(R.string.activity_title_ratings);

        this.listMusicList = (ListView)this.findViewById(R.id.listMusicList);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onResume() {
        super.onResume();

        String[] proj = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION };
        Cursor c = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        this.startManagingCursor(c);

        adapter = new SimpleCursorAdapter(this, R.layout.ratings_row, c, FROM, TO);
        adapter.setViewBinder(viewBinder);
        this.listMusicList.setAdapter(adapter);

        //c.moveToFirst();
        //for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
        //  String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
        //  this.textMusicList.append(title + "\n");
        //}
    }

    public void onRatingChanged(RatingBar ratingBar, float rating,
            boolean fromUser) {
        int id = (Integer)ratingBar.getTag();
        Log.w("bla", "the id is " + id);

    }
}
