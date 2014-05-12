package me.courbiere.android.docky.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

import me.courbiere.android.docky.ui.view.DraggableListView;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Sortable CursorAdapter. Used for the drag and drop list view.
 */
public class SortableCursorAdapter
        extends SimpleCursorAdapter implements DraggableListView.SortableAdapter {

    private static final String TAG = "SortableCursorAdapter";

    /**
     * Items positions mappings, key is list view position, value is cursor position.
     */
    private SparseIntArray mListMapping = new SparseIntArray();

    public SortableCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    public SortableCursorAdapter(Context context, int layout, Cursor c, String[] from,
            int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    /**
     * @see android.widget.SimpleCursorAdapter#swapCursor(android.database.Cursor)
     */
    @Override
    public Cursor swapCursor(Cursor c) {
        Cursor old = super.swapCursor(c);
        resetMappings();
        return old;
    }

    /**
     * @see android.widget.SimpleCursorAdapter#changeCursor(android.database.Cursor)
     */
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        resetMappings();
    }

    private void resetMappings() {
        mListMapping.clear();
    }

    /**
     * @see android.widget.ListAdapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return super.getItem(mListMapping.get(position, position));
    }

    /**
     * @see android.widget.ListAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return super.getItemId(mListMapping.get(position, position));
    }

    /**
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(mListMapping.get(position, position), convertView, parent);
    }

    /**
     * @see android.widget.BaseAdapter#getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return super.getDropDownView(mListMapping.get(position, position), convertView, parent);
    }

    /**
     * Swaps two items positions.
     *
     * @param from First item's position.
     * @param to Second item's position.
     */
    @Override
    public void swap(int from, int to) {
        if (from != to) {
            int cursorFrom = mListMapping.get(from, from);

            if (from > to) {
                for (int i = from; i > to; i--) {
                    mListMapping.put(i, mListMapping.get(i - 1, i - 1));
                }
            } else {
                for (int i = from; i < to; i++) {
                    mListMapping.put(i, mListMapping.get(i + 1, i + 1));
                }
            }

            mListMapping.put(to, cursorFrom);

            cleanMapping();
            notifyDataSetChanged();
        }
    }

    /**
     * Removes unnecessary mappings from the sparse array.
     */
    private void cleanMapping() {
        ArrayList<Integer> toRemove = new ArrayList<>();

        int size = mListMapping.size();
        for (int i = 0; i < size; ++i) {
            if (mListMapping.keyAt(i) == mListMapping.valueAt(i)) {
                toRemove.add(mListMapping.keyAt(i));
            }
        }

        size = toRemove.size();
        for (int i = 0; i < size; ++i) {
            mListMapping.delete(toRemove.get(i));
        }
    }
}
