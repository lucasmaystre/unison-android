package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;

import com.actionbarsherlock.app.SherlockFragment;

public class StatsFragment extends SherlockFragment implements MainActivity.OnRoomInfoListener {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.StatsActivity";

    private ListView usersList;
    private TextView trackTitle;

    private MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stats, container, false);

        this.usersList = (ListView)v.findViewById(R.id.usersList);
        this.trackTitle = (TextView)v.findViewById(R.id.trackTitle);

        return v;
    }

    public void onRoomInfo(JsonStruct.Room roomInfo) {
        if (roomInfo.track != null && roomInfo.track.title != null) {
            this.trackTitle.setText(roomInfo.track.title);
        }
        this.usersList.setAdapter(new StatsAdapter(roomInfo));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
        this.activity.registerRoomInfoListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity.unregisterRoomInfoListener(this);
    }

    private class StatsAdapter extends ArrayAdapter<JsonStruct.User> {

        public static final int ROW_LAYOUT = R.layout.stats_row;

        public StatsAdapter(JsonStruct.Room room) {
            super(StatsFragment.this.getActivity(), 0, room.users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) StatsFragment.this.getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.username)).setText(this.getItem(position).nickname);
            int score = getItem(position).score != null ? getItem(position).score : 0;
            ((ProgressBar) view.findViewById(R.id.liking)).setProgress(score);

            return view;
        }
    }
}
