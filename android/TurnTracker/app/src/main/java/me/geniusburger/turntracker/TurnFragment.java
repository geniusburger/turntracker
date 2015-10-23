package me.geniusburger.turntracker;

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
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelRefreshData();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(mUsers.get(position).id);
//        }
    }

    public void refreshData() {
        cancelRefreshData();
        mGetUsersAsyncTask = new GetUsersAsyncTask(getActivity());
        mGetUsersAsyncTask.execute();
    }

    public boolean cancelRefreshData() {
        return mGetUsersAsyncTask != null && !mGetUsersAsyncTask.isCancelled() && mGetUsersAsyncTask.cancel(true);
    }

    private void showProgress(boolean show) {
        if(mListView != null && mProgressView != null) {
            UIUtil.showProgress(getActivity(), show, mListView, mProgressView);
        }
    }

    public class GetUsersAsyncTask extends AsyncTask<Void, Void, User[]> {

        private Context mContext;

        public GetUsersAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected User[] doInBackground(Void... params) {
            return new Api(mContext).getStatus(mTaskId);
        }

        @Override
        protected void onPostExecute(User[] users) {
            mUsers.clear();
            if(users == null) {
                setEmptyText(R.string.tasks_failed);
            } else {
                setEmptyText(R.string.tasks_empty);
                mUsers.addAll(Arrays.asList(users));
            }
            ((BaseAdapter)mAdapter).notifyDataSetChanged();
            showProgress(false);
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
