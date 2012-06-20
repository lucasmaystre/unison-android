package ch.epfl.unison.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity implements UnisonMenu.OnRefreshListener {

    private static final String TAG = "ch.epfl.unison.MainActivity";
    private static final int RELOAD_INTERVAL = 30 * 1000;  // in ms.
    private static final int INITIAL_DELAY = 500; // in ms.

    private TabsAdapter tabsAdapter;
    private ViewPager viewPager;
    private Menu menu;

    private boolean isForeground = false;
    private Handler handler = new Handler();
    private Runnable updater = new Runnable() {
        public void run() {
            if (isForeground) {
                onRefresh();
                handler.postDelayed(this, RELOAD_INTERVAL);
            }
        }
    };

    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private Set<OnGroupInfoListener> listeners = new HashSet<OnGroupInfoListener>();

    private long groupId;

    public long getGroupId() {
        return this.groupId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handleExtras(this.getIntent().getExtras());

        // This activity should finish on logout.
        this.registerReceiver(this.logoutReceiver,
                new IntentFilter(UnisonMenu.ACTION_LOGOUT));

        // Set up the tabs & stuff.
        this.viewPager = new ViewPager(this);
        this.viewPager.setId(R.id.realtabcontent); // TODO change
        this.setContentView(this.viewPager);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        this.tabsAdapter = new TabsAdapter(this, this.viewPager);
        this.tabsAdapter.addTab(bar.newTab().setText(R.string.fragment_title_player),
                PlayerFragment.class, null);
        this.tabsAdapter.addTab(bar.newTab().setText(R.string.fragment_title_stats),
                StatsFragment.class, null);
    }

    private void handleExtras(Bundle extras) {
        if (extras == null || !extras.containsKey("gid")) {
            // Should never happen. If it does, redirect the user to the groups list.
            this.startActivity(new Intent(this, GroupsActivity.class));
            this.finish();
        } else {
            this.groupId = extras.getLong("gid");
            Log.i(TAG, "joined group " + this.groupId);
            if (extras.containsKey("name")) {
                this.setTitle(extras.getString("name"));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.isForeground = true;
        this.handler.postDelayed(updater, INITIAL_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.isForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.logoutReceiver);
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (this.menu == null) {
            return;
        }

        MenuItem refreshItem = this.menu.findItem(R.id.menu_item_refresh);
        if (refreshItem != null) {
            if (isRefreshing) {
                LayoutInflater inflater = (LayoutInflater)getSupportActionBar()
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View refreshView = inflater.inflate(R.layout.actionbar_indeterminate_progress, null);
                refreshItem.setActionView(refreshView);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    public void onRefresh() {
        this.repaintRefresh(true);
        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.getGroupInfo(this.groupId, new UnisonAPI.Handler<JsonStruct.Group>() {

            public void callback(JsonStruct.Group struct) {
                MainActivity.this.onGroupInfo(struct);
                MainActivity.this.dispatchGroupInfo(struct);
                MainActivity.this.repaintRefresh(false);
            }

            public void onError(UnisonAPI.Error error) {
                Log.d(TAG, error.toString());
                if (MainActivity.this != null) {
                    Toast.makeText(MainActivity.this, R.string.error_loading_info,
                            Toast.LENGTH_LONG).show();
                    MainActivity.this.repaintRefresh(false);
                }
            }

        });
    }

    private void onGroupInfo(JsonStruct.Group group) {
        this.setTitle(group.name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.startActivity(new Intent(this, GroupsActivity.class)
                    .setAction(GroupsActivity.ACTION_LEAVE_GROUP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void dispatchGroupInfo(JsonStruct.Group groupInfo) {
        for (OnGroupInfoListener listener : this.listeners) {
            listener.onGroupInfo(groupInfo);
        }
    }

    public void registerGroupInfoListener(OnGroupInfoListener listener) {
        this.listeners.add(listener);
    }

    public void unregisterGroupInfoListener(OnGroupInfoListener listener) {
        this.listeners.remove(listener);
    }

    public static interface OnGroupInfoListener {
        public void onGroupInfo(JsonStruct.Group groupInfo);
    }


    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = activity.getSupportActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        public void onPageScrollStateChanged(int state) {}

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
            Object tag = tab.getTag();
            for (int i=0; i<mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
        public void onTabReselected(Tab tab, FragmentTransaction ft) {}
    }
}
