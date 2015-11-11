package me.geniusburger.turntracker;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.User;

public class EditTaskFragment extends RefreshableFragment implements SwipeRefreshLayout.OnRefreshListener {

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
    GetUsersAsyncTask mGetUsersAsyncTask;

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

        // show progress if the task is already running
        if(mGetUsersAsyncTask != null && mGetUsersAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            showProgress(true);
        }

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
        inflater.inflate(R.menu.edit_task_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getUsers();
                return true;
            case R.id.action_delete:
                // TODO handle delete
                Toast.makeText(getContext(), "Delete not yet implemented", Toast.LENGTH_SHORT).show();
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
        mGetUsersAsyncTask = new GetUsersAsyncTask();
        mGetUsersAsyncTask.execute();
    }

    public boolean cancelAllAsyncTasks() {
        return cancelGetTaskUsers();
        // TODO cancel all tasks
    }

    public boolean cancelGetTaskUsers() {
        return mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled() && mGetUsersAsyncTask.cancel(true);
    }

    private boolean checkBusy() {
        // TODO check if any async tasks are running
        if(mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled()) {
            Toast.makeText(getContext(), "Already getting users", Toast.LENGTH_LONG).show();
        } else {
            return false;
        }
        return true;
    }

    public void saveTask() {
//        if(!checkBusy(view)) {
//            mTakeTurnAsyncTask = new TakeTurnAsyncTask(getActivity(), view, mTurnDate);
//            mTakeTurnAsyncTask.execute();
//        }
        mListener.taskSaved(false);
        // TODO save task by creating and starting an async task
    }

    private void showProgress(boolean show) {
        if(mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(show);
        }
    }

    public class GetUsersAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(getContext()).getTaskUsers(mTask.id, mUsers);
        }

        @Override
        protected void onPostExecute(Boolean success) {

            ((BaseAdapter)mAdapter).notifyDataSetChanged();
            if(success) {
                for(int i = 0; i < mUsers.size(); i++) {
                    // set the i+1 position to compensate for the header row, which is at position 0
                    mListView.setItemChecked(i + 1, mUsers.get(i).selected);
                }
            } else {
                Toast.makeText(getContext(), "Failed to get users", Toast.LENGTH_LONG).show();
            }
            showProgress(false);
            mGetUsersAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
            mGetUsersAsyncTask = null;
        }
    }

    @Override
    public void onRefresh() {
        getUsers();
    }

    public interface TaskListener {
        Task getCurrentTask();
        void taskSaved(boolean saved);
    }
}
