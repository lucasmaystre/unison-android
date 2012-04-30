package ch.epfl.unison.ui;

import android.os.Bundle;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PrefsActivity extends SherlockPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.activity_title_prefs);
        this.addPreferencesFromResource(R.xml.prefs);
    }

}
