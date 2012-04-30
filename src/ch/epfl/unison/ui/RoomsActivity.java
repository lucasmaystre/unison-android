package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class RoomsActivity extends SherlockActivity implements OnClickListener,
        OnItemClickListener, Runnable, UnisonMenu.OnRefreshListener {

    private List<HashMap<String, String>> data;
    private ListView roomsList;
    private SimpleAdapter adapter;

    private Handler handler;
    private boolean isRefreshing;

    private final SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
        public boolean setViewValue(View view, Object data,
                String textRepresentation) {
            if (view.getId() != R.id.nbParticipants)
                return false;
            ((TextView) view).setText(textRepresentation + " people in this room.");
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        this.setContentView(R.layout.rooms);
        this.setTitle(R.string.activity_title_rooms);

        Button b = (Button)this.findViewById(R.id.createRoomBtn);
        b.setOnClickListener(this);

        this.data = new ArrayList<HashMap<String, String>>();
        this.data.add(new HashMap<String, String>() {{ put("n", "Rock party");  put("p", "14"); }});
        this.data.add(new HashMap<String, String>() {{ put("n", "BC 246"); put("p", "2"); }});
        this.data.add(new HashMap<String, String>() {{ put("n", "Joel's birthday"); put("p", "7"); }});
        this.data.add(new HashMap<String, String>() {{ put("n", "Maelys' home"); put("p", "10"); }});
        this.data.add(new HashMap<String, String>() {{ put("n", "blabla"); put("p", "0"); }});

        this.roomsList = (ListView)this.findViewById(R.id.roomsList);
        this.roomsList.setOnItemClickListener(this);

        this.handler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] from = {"n", "p"};
        int[] to = {R.id.roomName, R.id.nbParticipants};
        this.adapter = new SimpleAdapter(this, this.data, R.layout.rooms_row, from, to);
        this.adapter.setViewBinder(this.viewBinder);
        this.roomsList.setAdapter(adapter);

        this.isRefreshing = false;
        this.handler.post(this);
    }

    public void onClick(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("New Room");
        alert.setMessage("Pick a name for the room:");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              // Do something with value!
              String name = input.getText().toString();
              Log.w("bla", name);
            }
          });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            // Cancelled.
          }
        });

        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.startActivity(new Intent(this, MainActivity.class));
    }

    public void run() {
        if (this.isRefreshing) {
            this.isRefreshing = false;
        } else {
            this.isRefreshing = true;
            this.handler.postDelayed(this, 1000);
        }
        this.setSupportProgressBarIndeterminateVisibility(this.isRefreshing);
    }

    public void onRefresh() {
        this.handler.post(this);
    }
}
