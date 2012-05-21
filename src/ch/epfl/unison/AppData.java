package ch.epfl.unison;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import ch.epfl.unison.api.UnisonAPI;

public class AppData implements OnSharedPreferenceChangeListener {

    private static final String TAG = "ch.epfl.unison.AppData";
    private static final int LOCATION_INTERVAL = 20 * 60 * 1000;  // In ms.

    private static AppData instance;

    private UnisonAPI api;
    private SharedPreferences prefs;
    private Location location;

    private AppData(Context c) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
        this.prefs.registerOnSharedPreferenceChangeListener(this);

        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        String provider = LocationManager.NETWORK_PROVIDER;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            provider = LocationManager.GPS_PROVIDER;  // Upgrade to GPS ;-)

        this.location = lm.getLastKnownLocation(provider);
        if (location != null) {
            Log.i(TAG, String.format("Got location: lat=%f, lon=%f",
                    this.location.getLatitude(), this.location.getLongitude()));
        }
        lm.requestLocationUpdates(provider, LOCATION_INTERVAL, 1f, new UnisonLocationListener());
    }

    public UnisonAPI getAPI() {
        if (this.api == null) {
            String email = this.prefs.getString("email", null);
            String password = this.prefs.getString("password", null);
            if (email != null && password != null) {
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

    public Location getLocation() {
        return this.location;
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

    public class UnisonLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            AppData.this.location = location;
            Log.i(TAG, String.format("Got location: lat=%f, lon=%f",
                    location.getLatitude(), location.getLongitude()));
        }

        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}
