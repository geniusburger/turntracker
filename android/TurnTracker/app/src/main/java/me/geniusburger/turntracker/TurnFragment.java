package me.geniusburger.turntracker;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

import me.geniusburger.turntracker.model.User;
import me.geniusburger.turntracker.utilities.UIUtil;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView with a GridView.
 * <p/>
 */
public class TurnFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_TASK_NAME = "taskName";

    private long mTaskId;
    private String mTaskName;
	private List<User> mUsers;

    //private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    private View mProgressView;

    // Workers
    GetUsersAsyncTask mGetUsersAsyncTask;
    TakeTurnAsyncTask mTakeTurnAsyncTask;
    UndoTurnAsyncTask mUndoTurnAsyncTask;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    public static TurnFragment newInstance(long taskId, String taskName) {
        TurnFragment fragment = new TurnFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_NAME, taskName);
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
        }
		
		mUsers = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
				android.R.id.text1, mUsers);

        setHasOptionsMenu(true);

		refreshData();
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

        mProgressView = view.findViewById(R.id.progress);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        // show progress if the task is already running
        if(mGetUsersAsyncTask != null && mGetUsersAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mListView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
        }

        return view;
    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }


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
        //mListener = null;
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
        mGetUsersAsyncTask = new GetUsersAsyncTask(getActivity());
        mGetUsersAsyncTask.execute();
    }

    public boolean cancelRefreshData() {
        return mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled() && mGetUsersAsyncTask.cancel(true);
    }

    public boolean cancelTakeTurn() {
        return mTakeTurnAsyncTask != null && !mTakeTurnAsyncTask.isCancelled() && mTakeTurnAsyncTask.cancel(true);
    }

    public boolean cancelUndoTurn() {
        return mUndoTurnAsyncTask != null && !mUndoTurnAsyncTask.isCancelled() && mUndoTurnAsyncTask.cancel(true);
    }

    private boolean checkBusy(View view) {
        if(mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already trying to get status", Snackbar.LENGTH_LONG).show();
        } else if(mTakeTurnAsyncTask != null && !mTakeTurnAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already trying to take turn", Snackbar.LENGTH_LONG).show();
        } else if(mUndoTurnAsyncTask != null && !mUndoTurnAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already trying to undo turn", Snackbar.LENGTH_LONG).show();
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
        if(mListView != null && mProgressView != null) {
            UIUtil.showProgress(getActivity(), show, mListView, mProgressView);
        }
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
            return new Api(mContext).takeTurn(mTaskId, mUsers);
        }

        @Override
        protected void onPostExecute(final Long turnId) {
            ((BaseAdapter)mAdapter).notifyDataSetChanged();
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
            return new Api(mContext).deleteTurn(mTurnId, mTaskId, mUsers);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ((BaseAdapter)mAdapter).notifyDataSetChanged();
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

    public class GetUsersAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;

        public GetUsersAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(mContext).getStatus(mTaskId, mUsers);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            ((BaseAdapter)mAdapter).notifyDataSetChanged();
            if(success) {
                setEmptyText(R.string.tasks_empty);
            } else {
                setEmptyText(R.string.tasks_failed);
            }
            showProgress(false);
            mGetUsersAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mGetUsersAsyncTask = null;
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
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(String id);
//    }

}
