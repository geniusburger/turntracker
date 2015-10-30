package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.Fragment;
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
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView with a GridView.
 * <p/>
 */
public class TurnFragment extends Fragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_TASK_NAME = "taskName";
    private static final String ARG_AUTO_TURN = "autoTurn";

    private Task mTask;
    private long mTaskId;
    private String mTaskName;
    private boolean mAutoTurn = false;
    private TurnFragmentInteractionListener mListener;

    private AbsListView mListView;
    private SwipeRefreshLayout mSwipeLayout;

    // Workers
    GetStatusAsyncTask mGetStatusAsyncTask;
    TakeTurnAsyncTask mTakeTurnAsyncTask;
    UndoTurnAsyncTask mUndoTurnAsyncTask;

    // Adapter
    private StatusAdapter mStatusAdapter;

    public static TurnFragment newInstance(long taskId, String taskName, boolean autoTurn) {
        TurnFragment fragment = new TurnFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_NAME, taskName);
        args.putBoolean(ARG_AUTO_TURN, autoTurn);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TurnFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTaskId = getArguments().getLong(ARG_TASK_ID);
            mTaskName = getArguments().getString(ARG_TASK_NAME);
            mAutoTurn = getArguments().getBoolean(ARG_AUTO_TURN);
        }

        mStatusAdapter = new StatusAdapter(
                getContext(),
                mTask,
                getString(R.string.user_list_label),
                getString(R.string.turn_list_label),
                false);

        setHasOptionsMenu(true);

        if(mAutoTurn && mListener != null) {
            mAutoTurn = false;
            takeTurn(mListener.getSnackBarView());
        } else {
            refreshData();
        }
    }

    @Override
    public void onResume() {
        getActivity().setTitle(mTaskName);
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_turn, container, false);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        mSwipeLayout.setOnRefreshListener(this);

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mStatusAdapter);
        View emptyView = view.findViewById(android.R.id.empty);
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
            }
        });
        mListView.setEmptyView(emptyView);
        //mUserListView.setOnItemClickListener(this);

        // show progress if the task is already running
        if(mGetStatusAsyncTask != null && mGetStatusAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            showProgress(true);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (TurnFragmentInteractionListener) activity;
            mTask = mListener.getCurrentTask();
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
                refreshData();
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(mUsers.get(position).id);
//        }
    }

    public boolean cancelAllAsyncTasks() {
        return cancelUndoTurn() || cancelTakeTurn() || cancelRefreshData();
    }

    public void refreshData() {
        cancelAllAsyncTasks();
        mGetStatusAsyncTask = new GetStatusAsyncTask(getActivity());
        mGetStatusAsyncTask.execute();
    }

    public boolean cancelRefreshData() {
        return mGetStatusAsyncTask != null && !mGetStatusAsyncTask.isCancelled() && mGetStatusAsyncTask.cancel(true);
    }

    public boolean cancelTakeTurn() {
        return mTakeTurnAsyncTask != null && !mTakeTurnAsyncTask.isCancelled() && mTakeTurnAsyncTask.cancel(true);
    }

    public boolean cancelUndoTurn() {
        return mUndoTurnAsyncTask != null && !mUndoTurnAsyncTask.isCancelled() && mUndoTurnAsyncTask.cancel(true);
    }

    private boolean checkBusy(View view) {
        if(mGetStatusAsyncTask != null && !mGetStatusAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already getting status", Snackbar.LENGTH_LONG).show();
        } else if(mTakeTurnAsyncTask != null && !mTakeTurnAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already taking turn", Snackbar.LENGTH_LONG).show();
        } else if(mUndoTurnAsyncTask != null && !mUndoTurnAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already undoing turn", Snackbar.LENGTH_LONG).show();
        } else {
            return false;
        }
        return true;
    }

    public void takeTurn(View view) {
        if(!checkBusy(view)) {
            mTakeTurnAsyncTask = new TakeTurnAsyncTask(getActivity(), view);
            mTakeTurnAsyncTask.execute();
        }
    }

    private void undoTurn(View view, long turnId) {
        if(!checkBusy(view)) {
            mUndoTurnAsyncTask = new UndoTurnAsyncTask(getActivity(), view, turnId);
            mUndoTurnAsyncTask.execute();
        }
    }

    private void showProgress(boolean show) {
        if(mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(show);
        }
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    public class TakeTurnAsyncTask extends AsyncTask<Void, Void, Long> {

        private View mView;
        private Context mContext;

        public TakeTurnAsyncTask(Context context, View view) {
            mView = view;
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Long doInBackground(Void... params) {
            return new Api(mContext).takeTurn(mTaskId, mStatusAdapter.getUsers(), mStatusAdapter.getTurns());
        }

        @Override
        protected void onPostExecute(final Long turnId) {
            mStatusAdapter.notifyDataSetChanged();
            if(turnId > 0) {
                final Snackbar bar = Snackbar.make(mView, "Turn Taken", Snackbar.LENGTH_LONG);
                bar.setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bar.dismiss();
                        undoTurn(mView, turnId);
                    }
                });
                bar.show();
            } else {
                Snackbar.make(mView, "Failed to take turn", Snackbar.LENGTH_LONG).show();
            }
            showProgress(false);
            mTakeTurnAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mTakeTurnAsyncTask = null;
            showProgress(false);
        }
    }

    public class UndoTurnAsyncTask extends AsyncTask<Void, Void, Boolean>{

        private View mView;
        private Context mContext;
        private long mTurnId;

        public UndoTurnAsyncTask(Context context, View view, long turnId) {
            mView = view;
            mContext = context;
            mTurnId = turnId;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(mContext).deleteTurn(mTurnId, mTaskId, mStatusAdapter.getUsers(), mStatusAdapter.getTurns());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mStatusAdapter.notifyDataSetChanged();
            if(success) {
                Snackbar.make(mView, "Turn Undone", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(mView, "Failed to undo turn", Snackbar.LENGTH_LONG).show();
            }
            showProgress(false);
            mUndoTurnAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mUndoTurnAsyncTask = null;
            showProgress(false);
        }
    }

    public class GetStatusAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;

        public GetStatusAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(mContext).getStatus(mTaskId, mStatusAdapter.getUsers(), mStatusAdapter.getTurns());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mStatusAdapter.notifyDataSetChanged();
            if(success) {
                setEmptyText(R.string.status_empty);
            } else {
                setEmptyText(R.string.status_failed);
            }
            showProgress(false);
            mGetStatusAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mGetStatusAsyncTask = null;
            setEmptyText(R.string.status_failed);
            showProgress(false);
        }
    }

    public void setEmptyText(int resId) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(resId);
        }
    }

    public interface TurnFragmentInteractionListener {
        View getSnackBarView();
        Task getCurrentTask();
    }

}
