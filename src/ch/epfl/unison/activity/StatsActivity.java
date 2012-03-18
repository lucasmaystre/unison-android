package ch.epfl.unison.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import ch.epfl.unison.R;
import ch.epfl.unison.widget.RefreshBar;
import ch.epfl.unison.widget.RefreshBar.OnRefreshListener;

public class StatsActivity extends MenuActivity implements OnClickListener,
        OnRefreshListener, Runnable {
    private static final String TAG = "ch.epfl.unison.StatsActivity";

    private List<HashMap<String, String>> data;

    private ListView usersList;
    private SimpleAdapter adapter;
    private RefreshBar refreshBar;

    private Handler handler;

    private final SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
        public boolean setViewValue(View view, Object data,
                String textRepresentation) {
            if (view.getId() != R.id.liking)
                return false;
            int r = Integer.valueOf(textRepresentation);
            ((ProgressBar) view).setMax(100);
            ((ProgressBar) view).setProgress(r);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.stats);

        this.data = new ArrayList<HashMap<String, String>>();
        this.data.add(new HashMap<String, String>() {{ put("u", "bengiuliano");  put("r", "67"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "lum"); put("r", "92"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "melody"); put("r", "13"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "william"); put("r", "100"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "gilbert95"); put("r", "36"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "da_pro_xxx"); put("r", "78"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "kikoulol"); put("r", "85"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "blabla"); put("r", "94"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "user29384"); put("r", "69"); }});
        this.data.add(new HashMap<String, String>() {{ put("u", "user9832"); put("r", "51"); }});

        this.usersList = (ListView)this.findViewById(R.id.usersList);

        this.refreshBar = (RefreshBar) this.findViewById(R.id.refreshBar);
        this.refreshBar.setOnRefreshListener(this);

        this.handler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] from = {"u", "r"};
        int[] to = {R.id.username, R.id.liking};
        this.adapter = new SimpleAdapter(this, this.data, R.layout.stats_row, from, to);
        this.adapter.setViewBinder(this.viewBinder);
        this.usersList.setAdapter(adapter);

        this.refreshBar.setState(RefreshBar.REFRESHING);
        this.handler.postDelayed(this, 1000);
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        this.refreshBar.setState(RefreshBar.READY);
    }

    public void run() {
        if (this.refreshBar.getState() == RefreshBar.REFRESHING) {
            this.refreshBar.setState(RefreshBar.READY);
        } else {
            this.refreshBar.setState(RefreshBar.REFRESHING);
            this.handler.postDelayed(this, 1000);
        }
    }

    public void onRefresh() {
        this.handler.post(this);
    }
}
