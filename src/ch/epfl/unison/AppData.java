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

    private Context context;
    private UnisonAPI api;
    private SharedPreferences prefs;

    private LocationManager locationMgr;
    private String locationProvider;
    private UnisonLocationListener locationListener;
    private Location location;

    private AppData(Context context) {
        this.context = context;
        this.locationListener = new UnisonLocationListener();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }

    private AppData setupLocation() {
        boolean updateListener = false;
        if (this.locationMgr == null) {
            this.locationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            this.locationProvider = LocationManager.NETWORK_PROVIDER;
            updateListener = true;
        }
        if (!this.locationProvider.equals(LocationManager.GPS_PROVIDER)
                && this.locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Always try to upgrade to GPS.
            this.locationProvider = LocationManager.GPS_PROVIDER;
            updateListener = true;
        }
        if (this.location == null) {
            this.location = this.locationMgr.getLastKnownLocation(this.locationProvider);
        }
        if (updateListener) {
            this.locationMgr.removeUpdates(this.locationListener);
            this.locationMgr.requestLocationUpdates(
                    this.locationProvider, LOCATION_INTERVAL, 1f, this.locationListener);
        }
        return this;
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
        return instance.setupLocation();
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
