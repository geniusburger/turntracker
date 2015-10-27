package me.geniusburger.turntracker;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;

import me.geniusburger.turntracker.model.Task;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TaskFragment.OnTaskSelectedListener, TurnFragment.TurnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_LOGIN = 1;
    private static final String FRAGMENT_TASKS = "tasks";
    private static final String FRAGMENT_TURNS = "turns";

    // UI
    TextView mUserNameTextView;
    TextView mDisplayNameTextView;

    // Fragments
    TaskFragment mTaskFragment;
    TurnFragment mTurnFragment;

    // Preferences
    private Preferences prefs;

    // Things to do
    long autoTurnTaskId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                String tag = currentFragment.getTag();
                switch (tag) {
                    case FRAGMENT_TASKS:
                        Snackbar.make(view, "Task creation is...under construction", Snackbar.LENGTH_LONG).show();
                        break;
                    case FRAGMENT_TURNS:
                        ((TurnFragment) currentFragment).takeTurn(view);
                        break;
                    default:
                        Log.e(TAG, "Unhandled FAB fragment tag " + tag);
                        Snackbar.make(view, "Not sure what to do...my bad", Snackbar.LENGTH_SHORT).show();
                        break;
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
        }

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
                getFragmentManager().popBackStack();
            } else if(null == mTaskFragment || !mTaskFragment.cancelRefreshData()) {
                super.onBackPressed();
            }
        }
    }

    public void refreshTasks(View v) {
        mTaskFragment.refreshData();
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
        // TODO need to do anything with the old fragment?
        mTurnFragment = TurnFragment.newInstance(task.id, task.name, autoTurn);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back

        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, mTurnFragment, FRAGMENT_TURNS)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public View getSnackBarView() {
        return findViewById(R.id.fab);
    }
}
