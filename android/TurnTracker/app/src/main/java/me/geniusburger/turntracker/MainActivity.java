package me.geniusburger.turntracker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import me.geniusburger.turntracker.gcm.RegistrationIntentService;
import me.geniusburger.turntracker.model.Task;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TaskFragment.OnTaskSelectedListener, TurnFragment.TurnFragmentInteractionListener, EditTaskFragment.TaskListener {

    public static final String EXTRA_TASK_ID = "taskId";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_CODE_LOGIN = 1;
    private static final String FRAGMENT_TASKS = "tasks";
    private static final String FRAGMENT_TURNS = "turns";
    private static final String FRAGMENT_EDIT = "edit";

    // UI
    TextView mUserNameTextView;
    TextView mDisplayNameTextView;
    FloatingActionButton fab;

    // Fragments
    TaskFragment mTaskFragment;
    TurnFragment mTurnFragment;

    // Preferences
    private Preferences prefs;

    // Things
    long autoTurnTaskId = 0;
    Task mCurrentTask;
    boolean autoRefresh = false;
    long fabResourceId = 0;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment instanceof RefreshableFragment) {
                    ((RefreshableFragment)currentFragment).onFabClick(view);
                } else {
                    Log.e(TAG, "Unhandled FAB fragment tag " + currentFragment.getTag());
                    Snackbar.make(view, "Not sure what to do...my bad", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerLayout = navigationView.inflateHeaderView(R.layout.nav_header_main);

        mUserNameTextView = (TextView) headerLayout.findViewById(R.id.userNameTextView);
        mDisplayNameTextView = (TextView) headerLayout.findViewById(R.id.displayNameTextView);

        autoTurnTaskId = 0;
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            handleNfc();
        } else {
            //autoTurnTaskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
            // TODO setup a way of viewing a task instead of just taking a turn
        }

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if(autoRefresh) {
                    if(mTaskFragment != null) {
                        mTaskFragment.onRefresh();
                    }
                    // TODO figure out why this is throwing an exception
//                    if(mTurnFragment != null) {
//                        mTurnFragment.onRefresh();
//                    }
                    autoRefresh = false;
                }
            }
        });

        prefs = new Preferences(this);
        long userId = prefs.getUserId();
        if(userId <= 0) {
            startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_CODE_LOGIN);
        } else {
            mTaskFragment = TaskFragment.newInstance(autoTurnTaskId);
            autoTurnTaskId = 0;
            getFragmentManager().beginTransaction().add(R.id.fragment_container, mTaskFragment, FRAGMENT_TASKS).commit();
        }

//        String packageName = MainActivity.class.getPackage().getName();
//        NdefRecord taskRecord = NdefRecord.createUri("http://geniusburger.me/task/123");
//        NdefRecord aarRecord = NdefRecord.createApplicationRecord(packageName);
//        NdefMessage msg = new NdefMessage( new NdefRecord[] { taskRecord, aarRecord });
//        Log.d(TAG, "nfc length " + msg.getByteArrayLength());

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean sentToken = prefs.getAndroidTokenSentToServer();
                if (sentToken) {
                    Toast.makeText(MainActivity.this, "Registered for GMC", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to register for GCM", Toast.LENGTH_SHORT).show();
                }
            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void handleNfc() {

        //Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        Log.d(TAG, "msgs: " + rawMsgs.length);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        NdefRecord[] records = msg.getRecords();
        Log.d(TAG, "records: " + records.length);
        for(NdefRecord record : records) {
            if(record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                byte[] payload = record.getPayload();
                String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

                // Get the Language Code
                int languageCodeLength = payload[0] & 0063;

                try {
                    String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
                    if(!"en".equals(languageCode)) {
                        Log.d(TAG, "unsupported language code " + languageCode);
                        continue;
                    }
                    // e.g. "en"

                    // Get the Text
                    String queryString = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding).trim();
                    if(!queryString.startsWith("?")) {
                        queryString = "?" + queryString;
                    }
                    Uri uri = Uri.parse(queryString);
                    for(String key : uri.getQueryParameterNames()) {
                        Log.d(TAG, key + " => " + uri.getQueryParameter(key));
                        if("task".equals(key)) {
                            autoTurnTaskId = Long.parseLong(uri.getQueryParameter(key));
                        }
                    }
                    Log.d(TAG, languageCode + ": " + queryString);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } else if(record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                Log.d(TAG, "ext: " + new String(record.getPayload()));
            } else {
                Log.d(TAG, "unhandled record");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CODE_LOGIN:
                if (mTaskFragment == null) {
                    mTaskFragment = TaskFragment.newInstance(autoTurnTaskId);
                    autoTurnTaskId = 0;
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, mTaskFragment, FRAGMENT_TASKS).commit();
                } else {
                    mTaskFragment.refreshData();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Preferences.ANDROID_REGISTRATION_COMPLETE));
        mUserNameTextView.setText(prefs.getUserName());
        mDisplayNameTextView.setText(prefs.getUserDisplayName());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getFragmentManager().getBackStackEntryCount() > 0){
                EditTaskFragment editFragment = (EditTaskFragment)getFragmentManager().findFragmentByTag(FRAGMENT_EDIT);
                if (editFragment != null && editFragment.isVisible()) {
                    // coming back from edit, reset fab to add icon
                    // todo setFabIcon(R.drawable.ic_add_24dp);
                }
                getFragmentManager().popBackStack();
            } else if(null == mTaskFragment || !mTaskFragment.cancelRefreshData()) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_tasks) {
            for(int fragments = getFragmentManager().getBackStackEntryCount(); fragments > 0; fragments--) {
                getFragmentManager().popBackStack();
            }
            mTaskFragment.refreshData();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            for(int fragments = getFragmentManager().getBackStackEntryCount(); fragments > 0; fragments--) {
                getFragmentManager().popBackStack();
            }
            mTaskFragment.clear();
            prefs.clearUser();
            startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_CODE_LOGIN);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onTaskSelected(Task task, boolean autoTurn) {
        mCurrentTask = task;

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back

        mTurnFragment = TurnFragment.newInstance(task.id, task.name, autoTurn);
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, TurnFragment.newInstance(task.id, task.name, autoTurn), FRAGMENT_TURNS)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onTaskLongSelected(Task task) {
        mCurrentTask = task;
        editTask(true);
    }

    /**
     * Fade/shrink the FAB out, change the icon, and fade/grow the FAB back in
     * @param resId The resource ID to set the FAB image to
     */
    public void setFabIcon(final int resId) {
        if(resId == 0) {
            fab.hide();
        } else if( fabResourceId != resId) {
            fab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    fab.setImageResource(resId);
                    fab.show();
                }
            });
        }
        fabResourceId = resId;
    }

    @Override
    public View getSnackBarView() {
        return fab;
    }

    @Override
    public Task getCurrentTask() {
        return mCurrentTask;
    }

    @Override
    public void createTask() {
        mCurrentTask = null;
        editTask(false);
    }

    @Override
    public void editTask() {
        editTask(true);
    }

    public void editTask(boolean edit) {
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, EditTaskFragment.newInstance(edit), FRAGMENT_EDIT)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void taskSaved(boolean saved) {
        if(saved) {
            autoRefresh = true;
        }
        onBackPressed();
    }

    @Override
    public long getMyUserID() {
        return prefs.getUserId();
    }
}
