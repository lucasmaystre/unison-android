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
    private UnisonLocationListener gpsListener;
    private Location gpsLocation;
    private UnisonLocationListener networkListener;
    private Location networkLocation;

    private AppData(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }

    private AppData setupLocation() {
        if (this.locationMgr == null) {
            this.locationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        // try to set up the network location listener.
        if (this.networkListener == null
                && this.locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            this.networkListener = new UnisonLocationListener(LocationManager.NETWORK_PROVIDER);
            this.locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL, 1f, this.networkListener);
            this.networkLocation = this.locationMgr.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER);
        }
        // try to set up the GPS location listener.
        if (this.gpsListener == null
                && this.locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            this.gpsListener = new UnisonLocationListener(LocationManager.GPS_PROVIDER);
            this.locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL, 1f, this.gpsListener);
            this.gpsLocation = this.locationMgr.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);
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

    public boolean showHelpDialog() {
        return this.prefs.getBoolean("helpdialog", true);
    }

    public void setShowHelpDialog(boolean value) {
        this.prefs.edit().putBoolean("helpdialog", value).commit();
    }

    public Location getLocation() {
        // Prefer GPS locations over network locations.
        return this.gpsLocation != null ? this.gpsLocation : this.networkLocation;
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

        private String provider;

        public UnisonLocationListener(String provider) {
            this.provider = provider;
        }

        public void onLocationChanged(Location location) {
            if (LocationManager.GPS_PROVIDER.equals(this.provider)) {
                AppData.this.gpsLocation = location;
            } else if (LocationManager.NETWORK_PROVIDER.equals(this.provider)) {
                AppData.this.networkLocation = location;
            } else {
                throw new RuntimeException("unsupported location provider");
            };
            Log.i(TAG, String.format("Got location (%s): lat=%f, lon=%f",
                    provider, location.getLatitude(), location.getLongitude()));
        }

        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}
