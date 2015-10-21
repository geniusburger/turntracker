package me.geniusburger.turntracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import me.geniusburger.turntracker.model.Task;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TaskFragment.OnTaskSelectedListener {

    // UI
    TextView mUserNameTextView;
    TextView mDisplayNameTextView;

    // Fragments
    TaskFragment mTaskFragment;

    // Preferences
    private Preferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mUserNameTextView = (TextView) drawer.findViewById(R.id.userNameTextView);
        mDisplayNameTextView = (TextView) drawer.findViewById(R.id.displayNameTextView);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        prefs = new Preferences(this);
        long userId = prefs.getUserId();
        if(userId <= 0) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            mTaskFragment = TaskFragment.newInstance();
            getFragmentManager().beginTransaction().add(R.id.fragment_container, mTaskFragment).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        mUserNameTextView.setText(prefs.getUserName());
        mDisplayNameTextView.setText(prefs.getUserDisplayName());
        if(mTaskFragment == null) {
            mTaskFragment = TaskFragment.newInstance();
            getFragmentManager().beginTransaction().add(R.id.fragment_container, mTaskFragment).commit();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(null == mTaskFragment || !mTaskFragment.cancelRefreshData()) {
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
            mTaskFragment.refreshData();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onTaskSelected(Task task) {
        Toast.makeText(this, "selected task " + task, Toast.LENGTH_SHORT).show();
    }
}
