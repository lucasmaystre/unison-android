package ch.epfl.hello;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Hello extends Activity {
    private static final String TAG = "Lucas";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String[] proj = { MediaStore.Audio.Media.TITLE };
        // TODO Be careful here, this could return null.
        Cursor c = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        this.startManagingCursor(c);
        c.moveToFirst();
        for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
            String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            Log.w(TAG, "The title is " + title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater  = this.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.itemMusicList:
            this.startActivity(new Intent(this, MusicList.class));
        break;
        case R.id.itemRooms:
            this.startActivity(new Intent(this, RoomsActivity.class));
        break;
        case R.id.itemPlayer:
            this.startActivity(new Intent(this, MainActivity.class));
        break;
        case R.id.itemSettings:
            this.startActivity(new Intent(this, PrefsActivity.class));
        break;
        }
        return true;
    }
}
