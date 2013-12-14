package me.courbiere.android.docky.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.item.AppInfo;

/**
 * DockItemArrayAdapter.
 */
public class DockItemArrayAdapter extends ArrayAdapter<AppInfo> {
    private static final String TAG = "DockItemArrayAdapter";

    private Context mContext;
    private int mLayoutResourceId;
    private List<AppInfo> mDockItems;

    public DockItemArrayAdapter(Context context, int layoutResourceId, List<AppInfo> dockItems) {
        super(context, layoutResourceId, dockItems);

        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mDockItems = dockItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(mLayoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.app_icon);
            // holder.name = (TextView) convertView.findViewById(R.id.app_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppInfo app = mDockItems.get(position);

        holder.icon.setImageDrawable(app.icon);
        // holder.name.setText(app.title);

        return convertView;
    }

    private class ViewHolder {
        public ImageView icon;
        // public TextView name;
    }
}
