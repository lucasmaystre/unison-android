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
import ch.epfl.unison.api.JsonStruct.RoomsList;
import ch.epfl.unison.api.JsonStruct.Success;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class RoomsActivity extends SherlockActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.RoomsActivity";
    private static final int RELOAD_INTERVAL = 120 * 1000;  // in ms.

    public static final String ACTION_LEAVE_ROOM = "ch.epfl.unison.action.LEAVE_ROOM";

    private ListView roomsList;
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

        this.setContentView(R.layout.rooms);
        //this.setTitle(R.string.activity_title_rooms);

        ((Button)this.findViewById(R.id.createRoomBtn))
                .setOnClickListener(new OnCreateRoomListener());

        this.roomsList = (ListView)this.findViewById(R.id.roomsList);
        this.roomsList.setOnItemClickListener(new OnRoomSelectedListener());

        // Actions that should be taken whe activity is started.
        if (ACTION_LEAVE_ROOM.equals(this.getIntent().getAction())) {
            // We are coming back from a room - let's make sure the back-end knows.
            this.leaveRoom();
        } else if (AppData.getInstance(this).showHelpDialog()) {
            this.showHelpDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.isForeground = true;
        this.startService(new Intent(LibraryService.ACTION_UPDATE));
        this.handler.post(this.updater);
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

        UnisonAPI.Handler<JsonStruct.RoomsList> handler
                = new UnisonAPI.Handler<JsonStruct.RoomsList>() {

            public void callback(RoomsList struct) {
                RoomsActivity.this.roomsList.setAdapter(new RoomsAdapter(struct));
                RoomsActivity.this.repaintRefresh(false);
            }

            public void onError(UnisonAPI.Error error) {
                Toast.makeText(RoomsActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                RoomsActivity.this.repaintRefresh(false);
            }
        };

        AppData data = AppData.getInstance(this);
        if (data.getLocation() != null) {
            double lat = data.getLocation().getLatitude();
            double lon = data.getLocation().getLongitude();
            data.getAPI().listRooms(lat, lon, handler);
        } else {
            data.getAPI().listRooms(handler);
        }
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (this.menu == null) {
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
        }
    }

    private void leaveRoom() {
        // Make sure the user is not marked as present in any room.
        AppData data = AppData.getInstance(this);
        data.getAPI().leaveRoom(data.getUid(), new UnisonAPI.Handler<JsonStruct.Success>() {

            public void callback(Success struct) {
                Log.d(TAG, "successfully left room");
            }

            public void onError(Error error) {
                Toast.makeText(RoomsActivity.this, error.toString(), Toast.LENGTH_LONG).show();
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
                    AppData.getInstance(RoomsActivity.this).setShowHelpDialog(false);
                }
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    startActivity(new Intent(RoomsActivity.this, HelpActivity.class));
                }
            }
        };

        alert.setPositiveButton("Yes", click);
        alert.setNegativeButton("No, thanks", click);
        alert.show();
    }

    private class RoomsAdapter extends ArrayAdapter<JsonStruct.Room> {

        public static final int ROW_LAYOUT = R.layout.rooms_row;

        public RoomsAdapter(JsonStruct.RoomsList list) {
            super(RoomsActivity.this, 0, list.rooms);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            JsonStruct.Room room = this.getItem(position);
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) RoomsActivity.this.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.roomName)).setText(room.name);
            String subtitle = null;
            if (room.distance != null) {
                subtitle = String.format("%s away - %d people.",
                        Uutils.distToString(room.distance), room.nbUsers);
            } else {
                subtitle = String.format("%d people.", room.nbUsers);
            }
            ((TextView) view.findViewById(R.id.nbParticipants)).setText(subtitle);

            view.setTag(room);
            return view;
        }
    }

    private class OnCreateRoomListener implements OnClickListener {

        public void onClick(View v) {
            AlertDialog.Builder alert = new AlertDialog.Builder(RoomsActivity.this);

            alert.setTitle("New Room");
            alert.setMessage("Pick a name for the room:");

            // Set an EditText view to get user input
            final EditText input = new EditText(RoomsActivity.this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    String name = input.getText().toString();

                    AppData data = AppData.getInstance(RoomsActivity.this);
                    double lat = data.getLocation().getLatitude();
                    double lon = data.getLocation().getLongitude();
                    data.getAPI().createRoom(name, lat, lon,
                            new UnisonAPI.Handler<JsonStruct.RoomsList>() {

                        public void callback(RoomsList struct) {
                            RoomsActivity.this.roomsList.setAdapter(new RoomsAdapter(struct));
                        }

                        public void onError(Error error) {
                            Toast.makeText(RoomsActivity.this, error.toString(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

            alert.setNegativeButton("Cancel", null);
            alert.show();
        }
    }

    private class OnRoomSelectedListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
            UnisonAPI api = AppData.getInstance(RoomsActivity.this).getAPI();
            long uid = AppData.getInstance(RoomsActivity.this).getUid();
            final JsonStruct.Room room = (JsonStruct.Room) view.getTag();

            api.joinRoom(uid, room.rid, new UnisonAPI.Handler<JsonStruct.Success>() {

                public void callback(Success struct) {
                    RoomsActivity.this.startActivity(new Intent(RoomsActivity.this, MainActivity.class)
                            .putExtra("rid", room.rid).putExtra("name", room.name));
                }

                public void onError(Error error) {
                    Toast.makeText(RoomsActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                }

            });
        }
    }
}
