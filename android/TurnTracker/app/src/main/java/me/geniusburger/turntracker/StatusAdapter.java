package me.geniusburger.turntracker;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geniusburger.turntracker.gcm.NotificationReceiver;
import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;
import me.geniusburger.turntracker.utilities.UnitMapping;

public class StatusAdapter extends BaseAdapter {

    private static final String TAG = StatusAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUB_HEADER = 1;
    private static final int TYPE_USER_ITEM = 2;
    private static final int TYPE_TURN_ITEM = 3;
    private static final int TYPE_COUNT = 4;

    private List<User> mUsers = new ArrayList<>();
    private List<Turn> mTurns = new ArrayList<>();
    private Map<Integer, String> mReasons = new HashMap();
    private Map<Integer, String> mMethods = new HashMap<>();
    private Task mTask;
    private Fragment mFragment;
    private Context mContext;
    private UnitMapping mUnits;

    private LayoutInflater mInflater;

    public StatusAdapter(Fragment fragment, Task task) {
        mFragment = fragment;
        mContext = fragment.getContext();
        mUnits = UnitMapping.getInstance(mContext);
        mTask = task;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Map<Integer, String> getReasons() {
        return mReasons;
    }

    public Map<Integer, String> getMethods() {
        return mMethods;
    }

    public List<User> getUsers() {
        return mUsers;
    }

    public void clearUsers() {
        mUsers.clear();
    }

    public List<Turn> getTurns() {
        return mTurns;
    }

    public void clearTurns() {
        mTurns.clear();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return TYPE_HEADER;
        } else if(position == 1 || position == mUsers.size() + 2) {
            return TYPE_SUB_HEADER;
        } else if(position < mUsers.size() + 2) {
            return TYPE_USER_ITEM;
        } else {
            return TYPE_TURN_ITEM;
        }
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getCount() {
        int count = mUsers.size() + mTurns.size();
        return count > 0 ? count + 3 : 0;
    }

    @Override
    public Object getItem(int position) {
        if(position == 0) {
            return mTask;
        } else if(position == 1) {
            return mContext.getString(R.string.user_order_list_label);
        } else if(position < mUsers.size() + 2) {
            return mUsers.get(position - 2);
        } else if(position == mUsers.size() + 2) {
            return mContext.getString(R.string.turn_list_label);
        } else if(position < mUsers.size() + mTurns.size() + 3) {
            return mTurns.get(position - mUsers.size() - 3);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            switch (rowType) {
                case TYPE_HEADER:
                    convertView = mInflater.inflate(R.layout.status_list_header, parent, false);
                    holder = new HeaderViewHolder(convertView);
                    break;
                case TYPE_SUB_HEADER:
                    convertView = mInflater.inflate(R.layout.status_list_sub_header, parent, false);
                    holder = new SubHeaderViewHolder(convertView);
                    break;
                case TYPE_USER_ITEM:
                    convertView = mInflater.inflate(R.layout.status_list_user_item, parent, false);
                    holder = new UserItemViewHolder(convertView);
                    break;
                case TYPE_TURN_ITEM:
                    convertView = mInflater.inflate(R.layout.status_list_turn_item, parent, false);
                    holder = new TurnItemViewHolder(convertView);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsuported row type " + rowType);
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Object item = getItem(position);
        if(item != null) {
            holder.update(this, item);
        }

        return convertView;
    }

    public static abstract class ViewHolder {
        public abstract void update(StatusAdapter adapter, Object data);
    }

    public class HeaderViewHolder extends ViewHolder implements View.OnClickListener {
        public TextView periodTextView;
        public TextView creatorTextView;
        public ImageView notificationImageView;
        public ImageView reminderImageView;
        public Chronometer elapsedChrono;
        public Chronometer exceededChrono;

        public HeaderViewHolder(View convertView) {
            periodTextView = (TextView) convertView.findViewById(R.id.periodTextView);
            creatorTextView = (TextView) convertView.findViewById(R.id.creatorTextView);
            notificationImageView = (ImageView) convertView.findViewById(R.id.notificationImageView);
            reminderImageView = (ImageView) convertView.findViewById(R.id.reminderImageView);
            elapsedChrono = (Chronometer) convertView.findViewById(R.id.elapsedChronometer);
            exceededChrono = (Chronometer) convertView.findViewById(R.id.exceededChronometer);
        }

        public void update(StatusAdapter adapter, Object data) {
            Task task = (Task)data;
            if(task.periodicHours == 0) {
                periodTextView.setText("-");
            } else {
                periodTextView.setText(mUnits.getMatchingText(task.periodicHours));
            }
            notificationImageView.setImageResource(task.notification
                    ? R.drawable.ic_notifications_24dp
                    : R.drawable.ic_notifications_none_24dp);
            notificationImageView.setOnClickListener(this);
            mFragment.registerForContextMenu(notificationImageView);
            reminderImageView.setImageResource(task.reminder
                    ? R.drawable.ic_alarm_on_24dp
                    : R.drawable.ic_alarm_off_24dp);
            reminderImageView.setOnClickListener(this);
            if(task.notification) {
                mFragment.registerForContextMenu(reminderImageView);
                reminderImageView.setImageAlpha(255);
            } else {
                mFragment.unregisterForContextMenu(reminderImageView);
                reminderImageView.setImageAlpha(70);
            }
            String name = null;
            for(User user : adapter.mUsers) {
                if(user.id == task.creatorUserID) {
                    name = user.displayName;
                }
            }

            creatorTextView.setText(name);

            if(mTurns.isEmpty()) {
                exceededChrono.setVisibility(View.INVISIBLE);
                elapsedChrono.setVisibility(View.INVISIBLE);
            } else {
                long msSinceLastTurn = new Date().getTime() - mTurns.get(0).date.getTime();
                if(mTask.periodicHours <= 0 || (msSinceLastTurn / 3600000) < mTask.periodicHours) {
                    exceededChrono.setVisibility(View.GONE);
                    elapsedChrono.setVisibility(View.VISIBLE);
                    elapsedChrono.setBase(SystemClock.elapsedRealtime() - msSinceLastTurn);
                    elapsedChrono.start();
                } else {
                    elapsedChrono.stop();
                    elapsedChrono.setBase(SystemClock.elapsedRealtime() - (3600000 * mTask.periodicHours));
                    exceededChrono.setBase(SystemClock.elapsedRealtime() - (msSinceLastTurn - (3600000 * mTask.periodicHours)));
                    exceededChrono.setVisibility(View.VISIBLE);
                    elapsedChrono.setVisibility(View.GONE);
                    exceededChrono.start();
                    exceededChrono.setOnClickListener(this);
                }
            }
        }

        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.notificationImageView) {
                if (mTask.notification) {
                    Toast.makeText(mContext, mReasons.get(mTask.reasonID), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.disable_notifications, Toast.LENGTH_SHORT).show();
                }
            } else if(v.getId() == R.id.exceededChronometer) {
                showSnoozeDialog();
            } else {
                Toast.makeText(mContext, mTask.reminder ? R.string.enable_reminders : R.string.disable_reminders, Toast.LENGTH_SHORT).show();
            }
        }

        private void showSnoozeDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setIcon(R.drawable.ic_notifications_paused_24dp);
            builder.setTitle("Snooze " + new Preferences(mContext).getNotificationSnoozeLabel() + "?");
            //builder.setMessage("Snooze " + new Preferences(mContext).getNotificationSnoozeLabel());
            builder.setPositiveButton("Snooze", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Bundle extras = new Bundle();
                    Preferences prefs = new Preferences(mContext);
                    long myId = prefs.getUserId();
                    String message = (mUsers.get(0).id == myId
                        ? "It's your turn for "
                        : "Don't forget about ") + mTask.name;
                    extras.putString(NotificationReceiver.EXTRA_MESSAGE, message);
                    extras.putLong(MainActivity.EXTRA_TASK_ID, mTask.id);
                    extras.putLong(MainActivity.EXTRA_USER_ID, myId);
                    NotificationReceiver.snooze(mContext, extras, mTask.id, prefs.getNotificationSnoozeMilliseconds(), prefs.getNotificationSnoozeLabel());
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
        }
    }

    public class SubHeaderViewHolder extends ViewHolder {
        public TextView subHeaderTextView;

        public SubHeaderViewHolder(View convertView) {
            subHeaderTextView = (TextView) convertView.findViewById(R.id.subHeaderTextView);
        }

        public void update(StatusAdapter adapter, Object data) {
            subHeaderTextView.setText((String)data);
        }
    }

    public class UserItemViewHolder extends ViewHolder {
        public View root;
        public TextView nameTextView;
        public TextView turnsTextView;

        public UserItemViewHolder(View convertView) {
            root = convertView;
            nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            turnsTextView = (TextView) convertView.findViewById(R.id.turnsTextView);
        }

        public void update(StatusAdapter adapter, Object data) {
            User user = (User)data;
            nameTextView.setText(user.displayName);
            turnsTextView.setText(user.consecutiveTurns > 0 ? String.format("x%d", user.consecutiveTurns) : null);
            if(adapter.mUsers.get(0) == user) {
                root.setBackgroundColor(adapter.mContext.getColor(R.color.colorAccent));
                nameTextView.setTextColor(adapter.mContext.getColor(R.color.colorAccentText));
            } else {
                root.setBackground(null);
                //root.setBackgroundColor(adapter.mContext.getColor(android.R.color.transparent));
                nameTextView.setTextColor(adapter.mContext.getColor(R.color.colorText));
            }
        }
    }

    public class TurnItemViewHolder extends ViewHolder implements View.OnClickListener {
        public TextView nameTextView;
        public TextView dateTextView;
        public ImageView preDateIndicator;
        public Turn turn;

        public TurnItemViewHolder(View convertView) {
            nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
            preDateIndicator = (ImageView) convertView.findViewById(R.id.preDateIndicator);
            preDateIndicator.setOnClickListener(this);
        }

        public void update(StatusAdapter adapter, Object data) {
            turn = (Turn)data;
            nameTextView.setText(turn.name);
            dateTextView.setText(turn.getDateString());
            preDateIndicator.setVisibility(turn.preDated ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onClick(View v) {
            if(turn != null) {
                Toast.makeText(mContext, "Added " + turn.getAddedString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
