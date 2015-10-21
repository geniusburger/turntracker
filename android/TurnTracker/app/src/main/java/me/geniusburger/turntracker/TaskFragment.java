package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import me.geniusburger.turntracker.utilities.UIUtil;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTaskSelectedListener}
 * interface.
 */
public class TaskFragment extends Fragment implements AbsListView.OnItemClickListener {

    private List<Task> mTasks;

    private OnTaskSelectedListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    private View mProgressView;

    // Workers
    GetTasksAsyncTask mGetTasksAsyncTask;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    public static TaskFragment newInstance() {
        TaskFragment fragment = new TaskFragment();
        Bundle args = new Bundle();
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

        mTasks = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1, mTasks);
        refreshData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        mProgressView = view.findViewById(R.id.progress);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        // show progress if the task is already running
        if(mGetTasksAsyncTask != null && mGetTasksAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mListView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
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
            mListener.onTaskSelected(mTasks.get(position));
        }
    }

    public void refreshData() {
        cancelRefreshData();
        mGetTasksAsyncTask = new GetTasksAsyncTask(getActivity());
        mGetTasksAsyncTask.execute();
    }

    public boolean cancelRefreshData() {
        if(mGetTasksAsyncTask != null) {
            return mGetTasksAsyncTask.cancel(true);
        }
        return false;
    }

    private void showProgress(boolean show) {
        if(mListView != null && mProgressView != null) {
            UIUtil.showProgress(getActivity(), show, mListView, mProgressView);
        }
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
            if(tasks == null) {
                setEmptyText(R.string.tasks_failed);
            } else {
                setEmptyText(R.string.tasks_empty);
                mTasks.clear();
                mTasks.addAll(Arrays.asList(tasks));
                ((BaseAdapter)mAdapter).notifyDataSetChanged();
            }
            showProgress(false);
        }

        @Override
        protected void onCancelled() {
            mGetTasksAsyncTask = null;
            showProgress(false);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(int resId)
    {
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
        void onTaskSelected(Task task);
    }
}
