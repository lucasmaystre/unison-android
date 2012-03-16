package ch.epfl.hello;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class MusicList extends Activity implements RatingBar.OnRatingBarChangeListener {

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
            ((RatingBar) view).setOnRatingBarChangeListener(MusicList.this);
            view.setTag(cursor.getInt(columnIndex));
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.musiclist_basic);

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

        adapter = new SimpleCursorAdapter(this, R.layout.row, c, FROM, TO);
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
