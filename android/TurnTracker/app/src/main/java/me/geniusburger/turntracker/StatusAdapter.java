package me.geniusburger.turntracker;

import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

public class StatusAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUB_HEADER = 1;
    private static final int TYPE_USER_ITEM = 2;
    private static final int TYPE_TURN_ITEM = 3;
    private static final int TYPE_COUNT = 4;

    private List<User> mUsers = new ArrayList<>();
    private List<Turn> mTurns = new ArrayList<>();
    private Task mTask;
    private Context mContext;

    private LayoutInflater mInflater;

    public StatusAdapter(Context context, Task task) {
        mContext = context;
        mTask = task;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        ViewHolder holder = null;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            switch (rowType) {
                case TYPE_HEADER:
                    convertView = mInflater.inflate(R.layout.status_list_header, null);
                    holder = new HeaderViewHolder(convertView);
                    break;
                case TYPE_SUB_HEADER:
                    convertView = mInflater.inflate(R.layout.status_list_sub_header, null);
                    holder = new SubHeaderViewHolder(convertView);
                    break;
                case TYPE_USER_ITEM:
                    convertView = mInflater.inflate(R.layout.status_list_user_item, null);
                    holder = new UserItemViewHolder(convertView);
                    break;
                case TYPE_TURN_ITEM:
                    convertView = mInflater.inflate(R.layout.status_list_turn_item, null);
                    holder = new TurnItemViewHolder(convertView);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.update(this, getItem(position));

        return convertView;
    }

    public static abstract class ViewHolder {
        public abstract void update(StatusAdapter adapter, Object data);
    }

    public class HeaderViewHolder extends ViewHolder {
        public TextView periodTextView;
        public TextView creatorTextView;
        public ImageView notificationImageView;
        public Chronometer elapsedChrono;
        public Chronometer exceededChrono;

        public HeaderViewHolder(View convertView) {
            periodTextView = (TextView) convertView.findViewById(R.id.periodTextView);
            creatorTextView = (TextView) convertView.findViewById(R.id.creatorTextView);
            notificationImageView = (ImageView) convertView.findViewById(R.id.notificationImageView);
            elapsedChrono = (Chronometer) convertView.findViewById(R.id.elapsedChronometer);
            exceededChrono = (Chronometer) convertView.findViewById(R.id.exceededChronometer);
        }

        public void update(StatusAdapter adapter, Object data) {
            Task task = (Task)data;
            if(task.periodicHours == 0) {
                periodTextView.setText("Unspecified");
            } else if(task.periodicHours % 24 == 0) {
                periodTextView.setText(task.periodicHours / 24 + " days");
            } else {
                periodTextView.setText(task.periodicHours + " hours");
            }
            notificationImageView.setImageResource(task.notification ? R.drawable.ic_notifications_24dp : R.drawable.ic_notifications_none_24dp);
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
                    exceededChrono.setVisibility(View.INVISIBLE);
                    elapsedChrono.setBase(SystemClock.elapsedRealtime() - msSinceLastTurn);
                    elapsedChrono.start();
                } else {
                    elapsedChrono.stop();
                    elapsedChrono.setBase(SystemClock.elapsedRealtime() - (3600000 * mTask.periodicHours));
                    exceededChrono.setBase(SystemClock.elapsedRealtime() - (msSinceLastTurn - (3600000 * mTask.periodicHours)));
                    exceededChrono.setVisibility(View.VISIBLE);
                    exceededChrono.start();
                }
                elapsedChrono.setVisibility(View.VISIBLE);
            }
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

    public class TurnItemViewHolder extends ViewHolder {
        public TextView nameTextView;
        public TextView dateTextView;

        public TurnItemViewHolder(View convertView) {
            nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
        }

        public void update(StatusAdapter adapter, Object data) {
            Turn turn = (Turn)data;
            nameTextView.setText(turn.name);
            dateTextView.setText(turn.getDateString());
        }
    }
}
