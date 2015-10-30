package me.geniusburger.turntracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

public class StatusAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SUB_HEADER = 1;
    private static final int TYPE_USER_ITEM = 2;
    private static final int TYPE_TURN_ITEM = 3;

    private List<User> mUsers = new ArrayList<>();
    private List<Turn> mTurns = new ArrayList<>();
    private Task mTask;
    private String mSubHeader1;
    private String mSubHeader2;
    private boolean mAutoNotify;
    private Context mContext;

    private LayoutInflater mInflater;

    public StatusAdapter(Context context, Task task, String subHeader1, String subHeader2, boolean autoNotify) {
        mContext = context;
        mTask = task;
        mSubHeader1 = subHeader1;
        mSubHeader2 = subHeader2;
        mAutoNotify = autoNotify;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addUser(final User user) {
        mUsers.add(user);
        if(mAutoNotify) {
            notifyDataSetChanged();
        }
    }

    public List<User> getUsers() {
        return mUsers;
    }

    public void clearUsers() {
        mUsers.clear();
    }

    public void addTurn(final Turn turn) {
        mTurns.add(turn);
        if(mAutoNotify) {
            notifyDataSetChanged();
        }
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
        return 4;
    }

    @Override
    public int getCount() {
        return 3 + mUsers.size() + mTurns.size();
    }

    @Override
    public Object getItem(int position) {
        if(position == 0) {
            return mTask;
        } else if(position == 1) {
            return mSubHeader1;
        } else if(position < mUsers.size() + 2) {
            return mUsers.get(position - 2);
        } else if(position == mUsers.size() + 2) {
            return mSubHeader2;
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

    public static class HeaderViewHolder extends ViewHolder {
        public TextView periodTextView;
        public TextView creatorTextView;
        public ImageView notificationImageView;

        public HeaderViewHolder(View convertView) {
            periodTextView = (TextView) convertView.findViewById(R.id.periodTextView);
            creatorTextView = (TextView) convertView.findViewById(R.id.creatorTextView);
            notificationImageView = (ImageView) convertView.findViewById(R.id.notificationImageView);
        }

        public void update(StatusAdapter adapter, Object data) {
            Task task = (Task)data;
            periodTextView.setText(task.periodicHours % 24 == 0 ? task.periodicHours / 24 + " days" : task.periodicHours + " hours");
            notificationImageView.setImageResource(task.notification ? R.drawable.ic_notifications_24dp : R.drawable.ic_notifications_none_24dp);
            String name = null;
            for(User user : adapter.mUsers) {
                if(user.id == task.creatorUserID) {
                    name = user.displayName;
                }
            }
            creatorTextView.setText(name);
        }
    }

    public static class SubHeaderViewHolder extends ViewHolder {
        public TextView subHeaderTextView;

        public SubHeaderViewHolder(View convertView) {
            subHeaderTextView = (TextView) convertView.findViewById(R.id.subHeaderTextView);
        }

        public void update(StatusAdapter adapter, Object data) {
            subHeaderTextView.setText((String)data);
        }
    }

    public static class UserItemViewHolder extends ViewHolder {
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

    public static class TurnItemViewHolder extends ViewHolder {
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
