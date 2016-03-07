package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import me.geniusburger.turntracker.model.Task;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView with a GridView.
 * <p/>
 */
public class TurnFragment extends RefreshableFragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_TASK_NAME = "taskName";
    private static final String ARG_AUTO_TURN = "autoTurn";

    private Calendar mTurnDate;
    private Task mTask;
    private long mTaskId;
    private String mTaskName;
    private boolean mAutoTurn = false;
    private TurnFragmentInteractionListener mListener;
    private Snackbar bar;

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

        mStatusAdapter = new StatusAdapter(getContext(), mTask);

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
        Animation entry = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 1f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f);
        entry.setDuration(300);
        mListView.setLayoutAnimation(new LayoutAnimationController(entry, 0.1f));
        View emptyView = view.findViewById(android.R.id.empty);
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
            }
        });
        mListView.setEmptyView(emptyView);
        mListView.setOnItemClickListener(this);

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
        menu.clear();
        inflater.inflate(R.menu.turn_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(bar != null && bar.isShownOrQueued()) {
            bar.dismiss();
        }
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshData();
                return true;
            case R.id.action_pick_turn_date:
                mTurnDate = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mTurnDate.set(Calendar.YEAR, year);
                        mTurnDate.set(Calendar.MONTH, monthOfYear);
                        mTurnDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        TimePickerDialog timePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                mTurnDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                mTurnDate.set(Calendar.MINUTE, minute);
                                takeTurn();
                            }
                        }, mTurnDate.get(Calendar.HOUR_OF_DAY), mTurnDate.get(Calendar.MINUTE), false);
                        timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mTurnDate = null;
                            }
                        });
                        timePicker.show();
                    }
                }, mTurnDate.get(Calendar.YEAR), mTurnDate.get(Calendar.MONTH), mTurnDate.get(Calendar.DAY_OF_MONTH));
                datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mTurnDate = null;
                    }
                });
                datePicker.show();
                return true;
            case R.id.action_edit_task:
                if(mListener != null) {
                    mListener.editTask();
                }
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
        if(bar != null && bar.isShownOrQueued()) {
            bar.dismiss();
        }
        cancelAllAsyncTasks();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO handle click
    }

    public void refreshData() {
        if(bar != null && bar.isShownOrQueued()) {
            bar.dismiss();
        }
        cancelAllAsyncTasks();
        mGetStatusAsyncTask = new GetStatusAsyncTask(getContext());
        mGetStatusAsyncTask.execute();
    }

    public boolean cancelAllAsyncTasks() {
        return cancelUndoTurn() || cancelTakeTurn() || cancelRefreshData();
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

    public void takeTurn() {
        if(mListener != null) {
            takeTurn(mListener.getSnackBarView());
        } else {
            Toast.makeText(getContext(), "Couldn't access snackbar", Toast.LENGTH_LONG).show();
        }
    }

    public void takeTurn(View view) {
        if(!checkBusy(view)) {
            if(mTurnDate != null && Calendar.getInstance().compareTo(mTurnDate) < 0) {
                Toast.makeText(getContext(), "Can't take a turn in the future", Toast.LENGTH_LONG).show();
                mTurnDate = null;
            } else {
                mTakeTurnAsyncTask = new TakeTurnAsyncTask(getActivity(), view, mTurnDate);
                mTurnDate = null;
                mTakeTurnAsyncTask.execute();
            }
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

    @Override
    public void onFabClick(View view) {
        takeTurn(view);
    }

    public class TakeTurnAsyncTask extends AsyncTask<Void, Void, Long> {

        private View mView;
        private Context mContext;
        private Calendar mDate;

        public TakeTurnAsyncTask(Context context, View view, Calendar date) {
            mView = view;
            mContext = context;
            mDate = date;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Long doInBackground(Void... params) {
            return new Api(mContext).takeTurn(mTaskId, mDate, mStatusAdapter.getUsers(), mStatusAdapter.getTurns());
        }

        @Override
        protected void onPostExecute(final Long turnId) {
            mStatusAdapter.notifyDataSetChanged();
            if(turnId > 0) {
                bar = Snackbar.make(mView, "Turn Taken", Snackbar.LENGTH_INDEFINITE);
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
            return new Api(mContext).getStatus(mTask, mStatusAdapter.getUsers(), mStatusAdapter.getTurns());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success) {
                setEmptyText(R.string.status_empty);
            } else {
                setEmptyText(R.string.status_failed);
                mStatusAdapter.clearUsers();
                mStatusAdapter.clearTurns();
            }
            mStatusAdapter.notifyDataSetChanged();
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
        void editTask();
    }

}
