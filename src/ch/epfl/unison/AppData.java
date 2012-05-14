package ch.epfl.unison;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import ch.epfl.unison.api.UnisonAPI;

public class AppData implements OnSharedPreferenceChangeListener {

    private static AppData instance;

    private UnisonAPI api;
    private SharedPreferences prefs;

    private AppData(Context c) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public UnisonAPI getAPI() {
        if (this.api == null) {
            String email = this.prefs.getString("email", null);
            String password = this.prefs.getString("password", null);
            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                this.api = new UnisonAPI(email, password);
            } else {
                this.api = new UnisonAPI();
            }
        }
        return this.api;
    }

    public long getUid() {
        return this.prefs.getLong("uid", -1);
    }

    public static synchronized AppData getInstance(Context c) {
        if (instance == null) {
            instance = new AppData(c.getApplicationContext());
        }
        return instance;
    }

    public synchronized void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (key.equals("email") || key.equals("password") || key.equals("uid")) {
            this.api = null;
        }
    }
}
