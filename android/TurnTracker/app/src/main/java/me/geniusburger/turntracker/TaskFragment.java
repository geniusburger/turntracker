package me.geniusburger.turntracker;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.geniusburger.turntracker.model.Task;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTaskSelectedListener}
 * interface.
 */
public class TaskFragment extends RefreshableFragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemLongClickListener {

    private static final String ARG_AUTO_TURN_TASK_ID = "autoTurnTaskId";
    private static final String ARG_TAKE_TURN = "takeTurn";
    private long autoTurnTaskId = 0;
    private boolean takeTurn = false;

    private List<Task> mTasks;

    private OnTaskSelectedListener mListener;

    private SwipeRefreshLayout mSwipeLayout;
    private AbsListView mListView;

    // Workers
    GetTasksAsyncTask mGetTasksAsyncTask;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    public static TaskFragment newInstance(long autoTurnTaskId, boolean takeTurn) {
        TaskFragment fragment = new TaskFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_AUTO_TURN_TASK_ID, autoTurnTaskId);
        args.putBoolean(ARG_TAKE_TURN, takeTurn);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TaskFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            autoTurnTaskId = getArguments().getLong(ARG_AUTO_TURN_TASK_ID, 0);
            takeTurn = getArguments().getBoolean(ARG_TAKE_TURN, false);
        }

        mTasks = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1, mTasks);

        setHasOptionsMenu(true);

        refreshData(getContext());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.task_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshData(getContext());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        getActivity().setTitle(R.string.app_name);
        ((MainActivity)getActivity()).setFabIcon(R.drawable.ic_add_24dp);
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        mSwipeLayout.setOnRefreshListener(this);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        View emptyView = view.findViewById(android.R.id.empty);
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData(getContext());
            }
        });
        mListView.setEmptyView(emptyView);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        // show progress if the task is already running
        if(mGetTasksAsyncTask != null && mGetTasksAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            showProgress(true);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTaskSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTaskSelectedListener");
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
        cancelRefreshData();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onTaskSelected(mTasks.get(position), false);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(null != mListener) {
            mListener.onTaskLongSelected(mTasks.get(position));
        }
        return true;
    }

    public void refreshData(Context context) {
        cancelRefreshData();
        mGetTasksAsyncTask = new GetTasksAsyncTask(context);
        mGetTasksAsyncTask.execute();
    }

    public boolean cancelRefreshData() {
        if(mGetTasksAsyncTask != null && !mGetTasksAsyncTask.isCancelled()) {
            return mGetTasksAsyncTask.cancel(true);
        }
        return false;
    }

    public void clear() {
        cancelRefreshData();
        mTasks.clear();
        ((BaseAdapter)mAdapter).notifyDataSetChanged();
    }

    private void showProgress(boolean show) {
        if(mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(show);
        }
    }

    @Override
    public void onRefresh(Context context) {
        refreshData(context);
    }

    @Override
    public void onRefresh() {
        refreshData(getContext());
    }

    @Override
    public void onFabClick(View view) {
        mListener.createTask();
    }

    public class GetTasksAsyncTask extends AsyncTask<Void, Void, Task[]> {

        private Context mContext;

        public GetTasksAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Task[] doInBackground(Void... params) {
            return new Api(mContext).getTasks();
        }

        @Override
        protected void onPostExecute(Task[] tasks) {
            mTasks.clear();
            if(tasks == null) {
                setEmptyText(R.string.tasks_failed);
            } else {
                setEmptyText(R.string.tasks_empty);
                mTasks.addAll(Arrays.asList(tasks));
            }
            ((BaseAdapter)mAdapter).notifyDataSetChanged();
            showProgress(false);

            if(autoTurnTaskId > 0 && mListener != null) {
                for(Task task : mTasks) {
                    if(task.id == autoTurnTaskId) {
                        mListener.onTaskSelected(task, takeTurn);
                        autoTurnTaskId = 0;
                        takeTurn = false;
                        break;
                    }
                }
                if(autoTurnTaskId > 0) {
                    Snackbar.make(mListener.getSnackBarView(), "Can't find task ID " + autoTurnTaskId, Snackbar.LENGTH_LONG).show();
                    autoTurnTaskId = 0;
                    takeTurn = false;
                }
            }
        }

        @Override
        protected void onCancelled() {
            mGetTasksAsyncTask = null;
            setEmptyText(R.string.tasks_failed);
            showProgress(false);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(int resId) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(resId);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTaskSelectedListener {
        void onTaskSelected(Task task, boolean autoTurn);
        void onTaskLongSelected(Task task);
        View getSnackBarView();
        void createTask();
    }
}
