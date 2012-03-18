package ch.epfl.unison.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ch.epfl.unison.R;

/**
 * Abstract base class for all activities that give access to the menu.
 *
 * @author lum
 */
public abstract class MenuActivity extends Activity {

    /** Index of the "quit room" menu item. */
    public static final int ITEM_CLOSE = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater  = this.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_close:
            this.startActivity(new Intent(this, RoomsActivity.class));
        break;
        case R.id.menu_item_ratings:
            this.startActivity(new Intent(this, RatingsActivity.class));
        break;
        case R.id.menu_item_prefs:
            this.startActivity(new Intent(this, PrefsActivity.class));
        break;
        }
        return true;
    }
}
