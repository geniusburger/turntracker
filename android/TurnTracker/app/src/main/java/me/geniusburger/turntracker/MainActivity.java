package me.geniusburger.turntracker;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import me.geniusburger.turntracker.model.Task;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI
    TextView mUserNameTextView;
    TextView mDisplayNameTextView;
    ListView mTaskListView;

    // Preferences
    private Preferences prefs;

    // Adapters
    ArrayAdapter<String> adapter;
    // Workers
    GetTasksTask mGetTasksTask;

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
        mTaskListView = (ListView) findViewById(R.id.taskListView);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[0]);
        mTaskListView.setAdapter(adapter);

        prefs = new Preferences(this);
        long userId = prefs.getUserId();
        if(userId <= 0) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        mUserNameTextView.setText(prefs.getUserName());
        mDisplayNameTextView.setText(prefs.getUserDisplayName());
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public class GetTasksTask extends AsyncTask<Void, Void, Task[]> {

        private Context mContext;

        public GetTasksTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Task[] doInBackground(Void... params) {
            return new Api(mContext).getTasks();
        }

        @Override
        protected void onPostExecute(Task[] tasks) {
            if(tasks == null) {
                Toast.makeText(mContext, "Failed to get tasks", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "Found " + tasks.length + " tasks", Toast.LENGTH_LONG).show();
                String[] names = new String[tasks.length];
                for(int i = 0; i < tasks.length; i++) {
                    names[i] = tasks[i].name;
                }
                adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, names);
                mTaskListView.setAdapter(adapter);
            }
        }

        @Override
        protected void onCancelled() {
            mGetTasksTask = null;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_tasks) {
            if(mGetTasksTask != null && !mGetTasksTask.isCancelled()) {
                mGetTasksTask.cancel(true);
            }
            mGetTasksTask = new GetTasksTask(this);
            mGetTasksTask.execute();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
