package ch.epfl.unison.activity;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import ch.epfl.unison.R;

public class MainActivity extends TabActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Reusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, PlayerActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("music").setIndicator(
                this.getString(R.string.activity_title_player),
                res.getDrawable(R.drawable.ic_tab_music)).setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        intent = new Intent().setClass(this, StatsActivity.class);
        spec = tabHost.newTabSpec("stats").setIndicator(
                this.getString(R.string.activity_title_stats),
                res.getDrawable(R.drawable.ic_tab_stats)).setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
}
