package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.User;

public class EditTaskFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_EDIT = "edit";

    private Task mTask;
    private List<User> mUsers;
    private boolean mEdit = false;
    private TaskListener mListener;
    private SwipeRefreshLayout mSwipeLayout;
    private ListView mListView;
    private ListAdapter mAdapter;

    // Views
    EditText mNameEditText;
    EditText mPeriodEditText;

    // Workers
    // async task

    public static EditTaskFragment newInstance(boolean edit) {
        EditTaskFragment fragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_EDIT, edit);
        fragment.setArguments(args);
        return fragment;
    }

    public EditTaskFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mEdit = getArguments().getBoolean(ARG_EDIT);
        }

        setHasOptionsMenu(true);

        if(mEdit) {
            if(mListener != null) {
                mTask = mListener.getCurrentTask();
            }
        }

        mUsers = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_multiple_choice,
                android.R.id.text1, mUsers);

        getUsers();
    }

    @Override
    public void onResume() {
        getActivity().setTitle(mEdit ? R.string.edit_task_title : R.string.create_task_title);
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_task, container, false);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        mSwipeLayout.setOnRefreshListener(this);

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        View header = inflater.inflate(R.layout.fragment_edit_task_header, mListView, false);
        mListView.addHeaderView(header, null, false);
        mListView.setHeaderDividersEnabled(true);
        mNameEditText = (EditText) header.findViewById(R.id.nameEditText);
        mPeriodEditText = (EditText) header.findViewById(R.id.periodEditText);

        if(mTask != null && mEdit) {
            mNameEditText.setText(mTask.name);
            mPeriodEditText.setText( String.valueOf(mTask.periodicHours));
        }

        // TODO show progress if already running
        // show progress if the task is already running
//        if(mGetStatusAsyncTask != null && mGetStatusAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
//            showProgress(true);
//        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (TaskListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement TurnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.turn_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // TODO handle refresh
                return true;
            case R.id.action_delete:
                // TODO handle delete
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelAllAsyncTasks();
    }

    public void getUsers() {
        cancelAllAsyncTasks();
//        mGetStatusAsyncTask = new GetStatusAsyncTask(getActivity());
//        mGetStatusAsyncTask.execute();
        // TODO get users in an async task
        for(int i = 0; i < 20; i++) {
            mUsers.add(new User(i, "username" + i, "Dummy User " + i));
        }
        ((BaseAdapter)mAdapter).notifyDataSetChanged();
    }

    public boolean cancelAllAsyncTasks() {
        //return cancelUndoTurn() || cancelTakeTurn() || cancelRefreshData();
        // TODO cancel all tasks
        return true;
    }

    // TODO cancel task
//    public boolean cancelRefreshData() {
//        return mGetStatusAsyncTask != null && !mGetStatusAsyncTask.isCancelled() && mGetStatusAsyncTask.cancel(true);
//    }

    private boolean checkBusy(View view) {
        // TODO check if any async tasks are running
//        if(mGetStatusAsyncTask != null && !mGetStatusAsyncTask.isCancelled()) {
//            Snackbar.make(view, "Already getting status", Snackbar.LENGTH_LONG).show();
//        } else if(mTakeTurnAsyncTask != null && !mTakeTurnAsyncTask.isCancelled()) {
//            Snackbar.make(view, "Already taking turn", Snackbar.LENGTH_LONG).show();
//        } else if(mUndoTurnAsyncTask != null && !mUndoTurnAsyncTask.isCancelled()) {
//            Snackbar.make(view, "Already undoing turn", Snackbar.LENGTH_LONG).show();
//        } else {
//            return false;
//        }
//        return true;
        return false;
    }

    public void saveTask() {
//        if(!checkBusy(view)) {
//            mTakeTurnAsyncTask = new TakeTurnAsyncTask(getActivity(), view, mTurnDate);
//            mTakeTurnAsyncTask.execute();
//        }
        // TODO save task by creating and starting an async task
    }

    private void showProgress(boolean show) {
        if(mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(show);

            if(show) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                    }
                }, 3000);
            }
        }
    }

    @Override
    public void onRefresh() {
        showProgress(true);
    }

    public interface TaskListener {
        Task getCurrentTask();
        void taskSaved(boolean saved);
    }
}
