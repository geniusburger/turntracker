package me.geniusburger.turntracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
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
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.nfc.TagReceiver;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView with a GridView.
 * <p/>
 */
public class StatusFragment extends RefreshableFragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemLongClickListener {

    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_TASK_NAME = "taskName";
    private static final String ARG_AUTO_TURN = "autoTurn";
    private static final String TAG = StatusFragment.class.getSimpleName();

    private Calendar mTurnDate;
    private Task mTask;
    private long mTaskId;
    private String mTaskName;
    private boolean mAutoTurn = false;
    private TurnFragmentInteractionListener mListener;
    private Snackbar bar;
    private boolean mWaitingToWrite = false;
    private boolean mOverwrite = false;
    private byte[] lastNfcId;

    private AbsListView mListView;
    private SwipeRefreshLayout mSwipeLayout;
    private AlertDialog mWaitingDialog;

    // Workers
    GetStatusAsyncTask mGetStatusAsyncTask;
    TakeTurnAsyncTask mTakeTurnAsyncTask;
    UndoTurnAsyncTask mUndoTurnAsyncTask;
    UpdateSubscriptionAsyncTask mUpdateSubscriptionAsyncTask;

    // Adapter
    private StatusAdapter mStatusAdapter;
    private NfcAdapter mNfcAdapter;

    public static StatusFragment newInstance(long taskId, String taskName, boolean autoTurn) {
        StatusFragment fragment = new StatusFragment();
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
    public StatusFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTaskId = getArguments().getLong(ARG_TASK_ID);
            mTaskName = getArguments().getString(ARG_TASK_NAME);
            mAutoTurn = getArguments().getBoolean(ARG_AUTO_TURN);
        }

        mStatusAdapter = new StatusAdapter(this, mTask);

        setHasOptionsMenu(true);

        if(mAutoTurn && mListener != null) {
            mAutoTurn = false;
            takeTurn(mListener.getSnackBarView());
        } else {
            refreshData(getContext());
        }
    }

    @Override
    public void onResume() {
        getActivity().setTitle(mTaskName);
        ((MainActivity)getActivity()).setFabIcon(R.drawable.ic_add_24dp);
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
        emptyView.setOnClickListener(v -> refreshData(v.getContext()));
        mListView.setEmptyView(emptyView);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        // show progress if the task is already running
        if(mGetStatusAsyncTask != null && mGetStatusAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            showProgress(true);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (TurnFragmentInteractionListener) context;
            mTask = mListener.getCurrentTask();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement TurnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
            case R.id.notificationImageView:
                for (Map.Entry<Integer, String> pair : mStatusAdapter.getReasons().entrySet()) {
                    menu.add(Menu.NONE, pair.getKey(), Menu.NONE, pair.getValue());
                }
                menu.add(Menu.NONE, 0, Menu.NONE, R.string.disable_notifications);
                break;
            case R.id.reminderImageView:
                menu.add(Menu.NONE, -1, Menu.NONE, R.string.enable_reminders);
                menu.add(Menu.NONE, -2, Menu.NONE, R.string.disable_reminders);
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        Task tempTask = new Task(mTask);
        switch (id) {
            case -2:
                if(mTask.reminder) {
                    tempTask.reminder = false;
                    updateSubscription(mListener.getSnackBarView(), tempTask);
                }
                return true;
            case -1:
                if(!mTask.reminder) {
                    tempTask.reminder = true;
                    updateSubscription(mListener.getSnackBarView(), tempTask);
                }
                return true;
            case 0:
                if(mTask.notification) {
                    tempTask.notification = false;
                    updateSubscription(mListener.getSnackBarView(), tempTask);
                }
                return true;
            default:
                String reason = mStatusAdapter.getReasons().get(id);
                if(reason != null) {
                    if(!mTask.notification) {

                        tempTask.reasonID = id;
                        tempTask.notification = true;
                        tempTask.methodID = 1;// default (android)
                        updateSubscription(mListener.getSnackBarView(), tempTask);
                    } else if(mTask.reasonID != id) {
                        tempTask.reasonID = id;
                        updateSubscription(mListener.getSnackBarView(), tempTask);
                    }
                } else {
                    return super.onContextItemSelected(item);
                }
                return true;
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
                refreshData(getContext());
                return true;
            case R.id.action_pick_turn_date:
                takeTurnInThePast();
                return true;
            case R.id.action_edit_task:
                if(mListener != null) {
                    mListener.editTask();
                }
                return true;
            case R.id.action_write_nfc:
                showNfcWaitingDialog(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void takeTurnInThePast() {
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
    }

    private void showNfcWaitingDialog(boolean write) {
        mWaitingToWrite = write;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNfcAdapter.enableForegroundDispatch(getActivity(), pi, null, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setIcon(R.drawable.ic_nfc_24dp);
        builder.setTitle(write ? "Write NFC Tag" : "Scan NFC Tag");
        builder.setMessage(write ? "Waiting for you to re-scan the same NFC tag..." : "Waiting for you to scan an NFC tag...");
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mWaitingToWrite = false;
                mOverwrite = false;
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mNfcAdapter != null) {
                    mNfcAdapter.disableForegroundDispatch(getActivity());
                    mNfcAdapter = null;
                }
                mWaitingDialog = null;
            }
        });
        mWaitingDialog = builder.create();
        mWaitingDialog.show();
    }

    private void showNfcOverwriteDialog(final Intent intent) {
        mOverwrite = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setIcon(R.drawable.ic_nfc_24dp);
        builder.setTitle("Overwrite NFC Tag");
        final StringBuilder sb = new StringBuilder("This NFC tag is not blank:\n");
        TagReceiver.readTag(intent, new TagReceiver.TagHandler() {
            @Override
            public void processText(String key, String value) {
                sb.append("\n").append(key).append(" - ").append(value);
            }
            @Override
            public void processOther(String ext) {
                sb.append("\n").append(ext);
            }
        });
        sb.append("\n\nDo you want to overwrite it?");
        builder.setMessage(sb.toString());
        builder.setCancelable(false);
        builder.setNegativeButton("No", null);
        builder.setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mOverwrite = true;
                lastNfcId = TagReceiver.getTag(intent).getId();
                showNfcWaitingDialog(true);
            }
        });
        builder.create().show();
    }

    private boolean isValidTech(Tag tag) {
        return tag.getTechList().length > 1;
    }

    private boolean isSameTag(Tag tag) {
        return Arrays.equals(tag.getId(), lastNfcId);
    }

    private void writeNfc(Intent intent) {

        Tag tag = TagReceiver.getTag(intent);
        Log.d(TAG, tag.getId().toString());
        Log.d(TAG, tag.getTechList().toString());
        // todo write nfc tag
        Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TAG, String.format("%s %s (%s)", key, value.toString(), value.getClass().getName()));
        }

        TagReceiver.writeNfc(getContext(), tag, "task=" + mTask.id);

        mWaitingToWrite = false;
        mOverwrite = false;
    }

    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onNewIntent action: " + action);
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = TagReceiver.getTag(intent);
            if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
                mWaitingDialog.dismiss();
            }
            if(mWaitingToWrite && mOverwrite) {
                if(isSameTag(tag)) {
                    writeNfc(intent);
                }
            } else {
                if(isValidTech(tag)) {
                    showNfcOverwriteDialog(intent);
                }
            }
        } else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = TagReceiver.getTag(intent);
            if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
                mWaitingDialog.dismiss();
            }
            if(isValidTech(tag)) {
                writeNfc(intent);
            }
        } else {
            Log.e(TAG, "Unexpected action received in onNewIntent");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        if(bar != null && bar.isShownOrQueued()) {
            bar.dismiss();
        }
        cancelAllAsyncTasks();
        if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
            mWaitingDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO handle click
    }

    public void refreshData(Context context) {
        if(bar != null && bar.isShownOrQueued()) {
            bar.dismiss();
        }
        cancelAllAsyncTasks();
        mGetStatusAsyncTask = new GetStatusAsyncTask(context);
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
        } else if(mUpdateSubscriptionAsyncTask != null && !mUpdateSubscriptionAsyncTask.isCancelled()) {
            Snackbar.make(view, "Already updating subscription", Snackbar.LENGTH_LONG).show();
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
                if(mTurnDate == null) {
                    mTurnDate = Calendar.getInstance();
                }
                mTakeTurnAsyncTask = new TakeTurnAsyncTask(getContext(), view, mTurnDate);
                mTurnDate = null;
                mTakeTurnAsyncTask.execute();
            }
        }
    }

    private void undoTurn(View view, long turnId) {
        if(!checkBusy(view)) {
            mUndoTurnAsyncTask = new UndoTurnAsyncTask(getContext(), view, turnId, "undo", "undone");
            mUndoTurnAsyncTask.execute();
        }
    }

    private void deleteTurn(View view, long turnId) {
        if(!checkBusy(view)) {
            mUndoTurnAsyncTask = new UndoTurnAsyncTask(getContext(), view, turnId, "delete", "deleted");
            mUndoTurnAsyncTask.execute();
        }
    }

    private void updateSubscription(View view, Task tempTask) {
        if(!checkBusy(view)) {
            mUpdateSubscriptionAsyncTask = new UpdateSubscriptionAsyncTask(getContext(), view, tempTask);
            mUpdateSubscriptionAsyncTask.execute();
        }
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
        takeTurn(view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Object item = parent.getAdapter().getItem(position);
        if(item instanceof Turn) {
            if(bar != null && bar.isShownOrQueued()) {
                bar.dismiss();
            }
            bar = Snackbar.make(mListener.getSnackBarView(), "Delete Turn?", Snackbar.LENGTH_LONG);
            bar.setAction("Delete", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bar.dismiss();
                    deleteTurn(mListener.getSnackBarView(), ((Turn) item).turnId);
                }
            });
            bar.show();
            return true;
        }
        return false;
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
                bar = Snackbar.make(mView, "Turn Taken", Snackbar.LENGTH_LONG);
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
            mStatusAdapter.notifyDataSetChanged();
            mTakeTurnAsyncTask = null;
            showProgress(false);
        }
    }

    public class UpdateSubscriptionAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private View mView;
        private Context mContext;
        private Task mTempTask;

        public UpdateSubscriptionAsyncTask(Context context, View view, Task tempTask) {
            mView = view;
            mContext = context;
            mTempTask = tempTask;
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new Api(mContext).setSubscription(mTempTask);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success) {
                mTask.update(mTempTask);
                mStatusAdapter.notifyDataSetChanged();
                Snackbar.make(mView, "Updated subscription", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(mView, "Failed to update subscription", Snackbar.LENGTH_LONG).show();
            }
            showProgress(false);
            mUpdateSubscriptionAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mUpdateSubscriptionAsyncTask = null;
            mStatusAdapter.notifyDataSetChanged();
            showProgress(false);
        }
    }

    public class UndoTurnAsyncTask extends AsyncTask<Void, Void, Boolean>{

        private View mView;
        private Context mContext;
        private long mTurnId;
        private String mPastTenseLabel;
        private String mPresentTenseLabel;

        public UndoTurnAsyncTask(Context context, View view, long turnId, String presentTenseLabel, String pastTenseLabel) {
            mView = view;
            mContext = context;
            mTurnId = turnId;
            mPastTenseLabel = pastTenseLabel;
            mPresentTenseLabel = presentTenseLabel;
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
                String label = mPastTenseLabel.substring(0, 1).toUpperCase() + mPastTenseLabel.toLowerCase().substring(1);
                Snackbar.make(mView, "Turn " + label, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(mView, "Failed to " + mPresentTenseLabel.toLowerCase() + " turn", Snackbar.LENGTH_LONG).show();
            }
            showProgress(false);
            mUndoTurnAsyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mUndoTurnAsyncTask = null;
            mStatusAdapter.notifyDataSetChanged();
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
            return new Api(mContext).getStatus(mTask, mStatusAdapter.getUsers(), mStatusAdapter.getTurns(), mStatusAdapter.getReasons(), mStatusAdapter.getMethods());
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
            mStatusAdapter.notifyDataSetChanged();
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
