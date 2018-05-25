package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.User;
import me.geniusburger.turntracker.utilities.UnitMapping;

public class EditTaskFragment extends RefreshableFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_EDIT = "edit";

    private Task mTask;
    private List<User> mUsers;
    private boolean mEdit = false;
    private TaskListener mListener;
    private SwipeRefreshLayout mSwipeLayout;
    private ListView mListView;
    private ListAdapter mAdapter;
    private long mMyUserId;
    private String mCreatorUserDisplayName;
    private UnitMapping mUnits;

    // Views
    EditText mNameEditText;
    EditText mPeriodEditText;
    Spinner mSpinner;

    // Workers
    GetUsersAsyncTask mGetUsersAsyncTask;
    SaveTaskAsyncTask mSaveTaskAsyncTask;

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

        mUnits = UnitMapping.getInstance(getContext());

        if (getArguments() != null) {
            mEdit = getArguments().getBoolean(ARG_EDIT, false);
        }

        setHasOptionsMenu(true);

        mUsers = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_multiple_choice,
                android.R.id.text1, mUsers);

        if(mListener != null) {
            mMyUserId = mListener.getMyUserID();
            if(mEdit) {
                mTask = mListener.getCurrentTask();
            }
            getUsers(getContext());
        }
    }

    @Override
    public void onResume() {
        getActivity().setTitle(mEdit ? R.string.edit_task_title : R.string.create_task_title);
        ((MainActivity)getActivity()).setFabIcon(R.drawable.ic_done_24dp);
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
        mSpinner = (Spinner) header.findViewById(R.id.unitSpinner);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(),
                android.R.layout.simple_spinner_item, mUnits.getLabels());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        if(mTask != null && mEdit) {
            mNameEditText.setText(mTask.name);
            mPeriodEditText.setText(String.valueOf(mTask.periodicHours));
            int matchingIndex = mUnits.getMatchingIndex(mTask.periodicHours);
            mSpinner.setSelection(matchingIndex);
            mPeriodEditText.setText(String.valueOf(mTask.periodicHours / mUnits.getMultiplier(matchingIndex)));
        } else {
            mSpinner.setSelection(mUnits.getDefaultIndex());
        }

        // show progress if the task is already running
        if(mGetUsersAsyncTask != null && mGetUsersAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            showProgress(true);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (TaskListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement TurnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.edit_task_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getUsers(getContext());
                return true;
            case R.id.action_delete:
                // TODO handle delete
                Toast.makeText(getContext(), "Delete not yet implemented", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_info:
                showInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(mTask.name);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.task_info_dialog, null);
        ((TextView)view.findViewById(R.id.textViewId)).setText(String.valueOf(mTask.id));
        ((TextView)view.findViewById(R.id.textViewCreated)).setText(String.valueOf(mTask.created));
        ((TextView)view.findViewById(R.id.textViewCreator)).setText(mCreatorUserDisplayName);
        ((TextView)view.findViewById(R.id.textViewModified)).setText(String.valueOf(mTask.modified));

        builder.setView(view);
        builder.setCancelable(true);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing, just dismiss
                    }
                });

        builder.create().show();;
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

    public void getUsers(Context context) {
        cancelAllAsyncTasks();
        mGetUsersAsyncTask = new GetUsersAsyncTask(context);
        mGetUsersAsyncTask.execute();
    }

    public boolean cancelAllAsyncTasks() {
        return cancelGetTaskUsers() || cancelSaveTask();
        // TODO cancel all tasks
    }

    public boolean cancelGetTaskUsers() {
        return mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled() && mGetUsersAsyncTask.cancel(true);
    }

    public boolean cancelSaveTask() {
        return mSaveTaskAsyncTask != null && !mSaveTaskAsyncTask.isCancelled() && mSaveTaskAsyncTask.cancel(true);
    }

    private boolean checkBusy() {
        // TODO check if any async tasks are running
        if(mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled()) {
            Toast.makeText(getContext(), "Already getting users", Toast.LENGTH_SHORT).show();
        } else if(mSaveTaskAsyncTask != null && !mSaveTaskAsyncTask.isCancelled()) {
            Toast.makeText(getContext(), "Already saving task", Toast.LENGTH_SHORT).show();
        } else {
            return false;
        }
        return true;
    }

    public void saveTask() {
        if(!checkBusy()) {
            mSaveTaskAsyncTask = new SaveTaskAsyncTask();
            mSaveTaskAsyncTask.execute();
        }
    }

    private void showProgress(boolean show) {
        if(mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(show);
        }
    }

    public class SaveTaskAsyncTask extends AsyncTask<Void, Void, Long> {

        List<Long> mSelectedUserIds = new ArrayList<>();
        Task mTaskUpdate;

        public SaveTaskAsyncTask() {

            int multiplier = mUnits.getMultiplier(mSpinner.getSelectedItemPosition());
            int count = Integer.parseInt(mPeriodEditText.getText().toString());

            mTaskUpdate = new Task(
                    mEdit ? mTask.id : 0,
                    mNameEditText.getText().toString(),
                    count * multiplier,
                    mEdit ? mTask.creatorUserID : mMyUserId);
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
            // TODO stop showing progress here somehow
            mSelectedUserIds.add(mMyUserId);
            for(int i = 0; i < mUsers.size(); i++) {
                User user = mUsers.get(i);
                // check 'i+1' because the listview header is at position 0
                if(mListView.isItemChecked(i+1) && !mSelectedUserIds.contains(user.id)) {
                    mSelectedUserIds.add(user.id);
                }
            }
        }

        @Override
        protected Long doInBackground(Void... params) {
            return new Api(getContext()).saveTask(mTaskUpdate, mSelectedUserIds);
        }

        @Override
        protected void onPostExecute(Long taskId) {
            // TODO stop showing progress here
            if(taskId > 0) {
                if(mListener != null) {
                    mListener.taskSaved(true);
                    Toast.makeText(getContext(), "Saved task", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Failed to save task", Toast.LENGTH_LONG).show();
            }
            showProgress(false);
            mSaveTaskAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
            mSaveTaskAsyncTask = null;
        }
    }

    public class GetUsersAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;

        public GetUsersAsyncTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(mContext).getTaskUsers(mEdit ? mTask.id : 0, mUsers);
        }

        @Override
        protected void onPostExecute(Boolean success) {

            ((BaseAdapter)mAdapter).notifyDataSetChanged();
            if(success) {
                for(int i = 0; i < mUsers.size(); i++) {
                    User user = mUsers.get(i);
                    // set the i+1 position to compensate for the header row, which is at position 0
                    mListView.setItemChecked(i + 1, user.selected);
                    if(user.id == mTask.creatorUserID) {
                        mCreatorUserDisplayName = user.displayName;
                    }
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
    public void onRefresh(Context context) {
        getUsers(context);
    }

    @Override
    public void onRefresh() {
        getUsers(getContext());
    }

    @Override
    public void onFabClick(View view) {
        saveTask();
    }

    public interface TaskListener {
        Task getCurrentTask();
        void taskSaved(boolean saved);
        long getMyUserID();
    }
}
