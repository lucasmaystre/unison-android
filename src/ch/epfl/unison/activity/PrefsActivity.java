package ch.epfl.unison.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import ch.epfl.unison.R;

public class PrefsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.prefs);
    }

}
