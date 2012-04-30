package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockFragment;

public class StatsFragment extends SherlockFragment {
    private static final String TAG = "ch.epfl.unison.StatsActivity";

    private List<HashMap<String, String>> data;

    private ListView usersList;
    private SimpleAdapter adapter;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stats, container, false);

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

        this.usersList = (ListView)v.findViewById(R.id.usersList);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] from = {"u", "r"};
        int[] to = {R.id.username, R.id.liking};
        this.adapter = new SimpleAdapter(this.getActivity(), this.data, R.layout.stats_row, from, to);
        this.adapter.setViewBinder(this.viewBinder);
        this.usersList.setAdapter(adapter);
    }
}
