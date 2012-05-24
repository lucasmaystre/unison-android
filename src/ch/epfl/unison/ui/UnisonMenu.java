package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Intent;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Abstract base class for all activities that give access to the menu.
 *
 * @author lum
 */
public abstract class UnisonMenu {

    public static final String ACTION_LOGOUT = "ch.epfl.unison.action.LOGOUT";

    public static boolean onCreateOptionsMenu(SherlockActivity activity, Menu menu) {
        MenuInflater inflater = activity.getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static boolean onCreateOptionsMenu(SherlockFragmentActivity activity, Menu menu) {
        MenuInflater inflater = activity.getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static boolean onOptionsItemSelected(Activity activity, OnRefreshListener listener, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_refresh:
            listener.onRefresh();
        break;
        case R.id.menu_item_ratings:
            activity.startActivity(new Intent(activity, RatingsActivity.class));
        break;
        case R.id.menu_item_prefs:
            activity.startActivity(new Intent(activity, PrefsActivity.class));
        break;
        case R.id.menu_item_logout:
            activity.startActivity(new Intent(activity, LoginActivity.class)
                    .putExtra("logout", true));
            // Send broadcast to all activities that can only be used when logged in.
            activity.sendBroadcast(new Intent().setAction(ACTION_LOGOUT));
        break;
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            activity.startActivity(new Intent(activity, RoomsActivity.class)
                    .setAction(RoomsActivity.ACTION_LEAVE_ROOM)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        break;
        default:
            // Should never happen.
            break;
        }
        return true;
    }

    public static interface OnRefreshListener {
        public void onRefresh();
    }
}
