package ch.epfl.unison.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockFragment;

public class ListenerFragment extends SherlockFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.listener, container, false);
        return v;
    }
}
