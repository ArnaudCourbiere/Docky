package me.courbiere.android.docky.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import me.courbiere.android.docky.R;

/**
 * DockItemArrayAdapter.
 */
public class DockItemArrayAdapter extends ArrayAdapter<String> {
    private static final String TAG = "DockItemArrayAdapter";

    private Context mContext;
    private int mLayoutResourceId;
    private List<String> mDockItems;

    public DockItemArrayAdapter(Context context, int layoutResourceId, List<String> dockItems) {
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
            holder.itemName = (TextView) convertView.findViewById(R.id.item_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.itemName.setText(mDockItems.get(position));

        return convertView;
    }

    private class ViewHolder {
        public TextView itemName;
    }
}
