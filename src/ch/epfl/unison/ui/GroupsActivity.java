package ch.epfl.unison.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.GroupsList;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class GroupsActivity extends SherlockActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.GroupsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000;  // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    // EPFL Polydome.
    private static final double DEFAULT_LATITUDE = 46.52147800207456;
    private static final double DEFAULT_LONGITUDE = 6.568992733955383;

    public static final String ACTION_LEAVE_GROUP = "ch.epfl.unison.action.LEAVE_GROUP";

    private ListView groupsList;
    private Menu menu;

    private boolean isForeground = false;
    private Handler handler = new Handler();
    private Runnable updater = new Runnable() {
        public void run() {
            if (isForeground) {
                onRefresh();
                handler.postDelayed(this, RELOAD_INTERVAL);
            }
        }
    };

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

        this.setContentView(R.layout.groups);

        ((Button)this.findViewById(R.id.createGroupBtn))
                .setOnClickListener(new OnCreateGroupListener());

        this.groupsList = (ListView)this.findViewById(R.id.groupsList);
        this.groupsList.setOnItemClickListener(new OnGroupSelectedListener());

        // Actions that should be taken whe activity is started.
        if (ACTION_LEAVE_GROUP.equals(this.getIntent().getAction())) {
            // We are coming back from a group - let's make sure the back-end knows.
            this.leaveGroup();
        } else if (AppData.getInstance(this).showHelpDialog()) {
            this.showHelpDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.isForeground = true;
        this.startService(new Intent(LibraryService.ACTION_UPDATE));
        this.handler.postDelayed(this.updater, INITIAL_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.isForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.logoutReceiver);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    public void onRefresh() {
        this.repaintRefresh(true);

        UnisonAPI.Handler<JsonStruct.GroupsList> handler
                = new UnisonAPI.Handler<JsonStruct.GroupsList>() {

            public void callback(GroupsList struct) {
                GroupsActivity.this.groupsList.setAdapter(new GroupsAdapter(struct));
                GroupsActivity.this.repaintRefresh(false);
            }

            public void onError(UnisonAPI.Error error) {
                Log.d(TAG, error.toString());
                if (GroupsActivity.this != null) {
                    Toast.makeText(GroupsActivity.this, R.string.error_loading_groups,
                            Toast.LENGTH_LONG).show();
                    GroupsActivity.this.repaintRefresh(false);
                }
            }
        };

        AppData data = AppData.getInstance(this);
        if (data.getLocation() != null) {
            double lat = data.getLocation().getLatitude();
            double lon = data.getLocation().getLongitude();
            data.getAPI().listGroups(lat, lon, handler);
        } else {
            data.getAPI().listGroups(handler);
        }
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (this.menu == null) {
            Log.d(TAG, "repaintRefresh: this.menu is null");
            return;
        }

        MenuItem refreshItem = this.menu.findItem(R.id.menu_item_refresh);
        if (refreshItem != null) {
            if (isRefreshing) {
                LayoutInflater inflater = (LayoutInflater)getSupportActionBar()
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View refreshView = inflater.inflate(R.layout.actionbar_indeterminate_progress, null);
                refreshItem.setActionView(refreshView);
            } else {
                refreshItem.setActionView(null);
            }
        } else {
            Log.d(TAG, "repaintRefresh: menu_item_refresh not found");
        }
    }

    private void leaveGroup() {
        // Make sure the user is not marked as present in any group.
        AppData data = AppData.getInstance(this);
        data.getAPI().leaveGroup(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

            public void callback(Success struct) {
                Log.d(TAG, "successfully left group");
            }

            public void onError(Error error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void showHelpDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Welcome");
        alert.setMessage("Want to learn how to use the application ?");

        final CheckBox cbox = new CheckBox(this);
        cbox.setText("Don't show this again");
        alert.setView(cbox);

        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                if (cbox.isChecked()) {
                    // Don't show the dialog again in the future.
                    AppData.getInstance(GroupsActivity.this).setShowHelpDialog(false);
                }
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    startActivity(new Intent(GroupsActivity.this, HelpActivity.class));
                }
            }
        };

        alert.setPositiveButton("Yes", click);
        alert.setNegativeButton("No, thanks", click);
        alert.show();
    }

    private class GroupsAdapter extends ArrayAdapter<JsonStruct.Group> {

        public static final int ROW_LAYOUT = R.layout.groups_row;

        public GroupsAdapter(JsonStruct.GroupsList list) {
            super(GroupsActivity.this, 0, list.groups);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Group group = this.getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) GroupsActivity.this.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.groupName)).setText(group.name);
            String subtitle = null;
            if (group.distance != null) {
                subtitle = String.format("%s away - %d people.",
                        Uutils.distToString(group.distance), group.nbUsers);
            } else {
                subtitle = String.format("%d people.", group.nbUsers);
            }
            ((TextView) view.findViewById(R.id.nbParticipants)).setText(subtitle);

            view.setTag(group);
            return view;
        }
    }

    private class OnCreateGroupListener implements OnClickListener {

        public void onClick(View v) {
            AlertDialog.Builder alert = new AlertDialog.Builder(GroupsActivity.this);

            alert.setTitle("New Group");
            alert.setMessage("Pick a name for the group:");

            // Set an EditText view to get user input
            final EditText input = new EditText(GroupsActivity.this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    String name = input.getText().toString();
                    AppData data = AppData.getInstance(GroupsActivity.this);

                    double lat, lon;
                    if (data.getLocation() != null) {
                        lat = data.getLocation().getLatitude();
                        lon = data.getLocation().getLongitude();
                    } else {
                        lat = DEFAULT_LATITUDE;
                        lon = DEFAULT_LONGITUDE;
                        Log.i(TAG, "location was null, using default values");
                    }
                    data.getAPI().createGroup(name, lat, lon,
                            new UnisonAPI.Handler<JsonStruct.GroupsList>() {

                        public void callback(GroupsList struct) {
                            GroupsActivity.this.groupsList.setAdapter(new GroupsAdapter(struct));
                        }

                        public void onError(Error error) {
                            Log.d(TAG, error.toString());
                            if (GroupsActivity.this != null) {
                                Toast.makeText(GroupsActivity.this, R.string.error_creating_group,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

            alert.setNegativeButton("Cancel", null);
            alert.show();
        }
    }

    private class OnGroupSelectedListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
            UnisonAPI api = AppData.getInstance(GroupsActivity.this).getAPI();
            long uid = AppData.getInstance(GroupsActivity.this).getUid();
            final JsonStruct.Group group = (JsonStruct.Group) view.getTag();

            api.joinGroup(uid, group.gid, new UnisonAPI.Handler<JsonStruct.Success>() {

                public void callback(Success struct) {
                    GroupsActivity.this.startActivity(new Intent(GroupsActivity.this, MainActivity.class)
                            .putExtra("gid", group.gid).putExtra("name", group.name));
                }

                public void onError(Error error) {
                    Log.d(TAG, error.toString());
                    if (GroupsActivity.this != null) {
                        Toast.makeText(GroupsActivity.this, R.string.error_joining_group,
                                Toast.LENGTH_LONG).show();
                    }
                }

            });
        }
    }
}
