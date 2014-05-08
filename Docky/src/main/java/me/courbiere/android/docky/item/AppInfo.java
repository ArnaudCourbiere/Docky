package me.courbiere.android.docky.item;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * TODO
 */
public class AppInfo implements Parcelable {
    private static final String TAG = "AppInfo";

    /**
     * The application name.
     */
    public CharSequence title;

    /**
     * The intent used to start the application.
     */
    public Intent intent;

    /**
     * The application icon.
     */
    public Drawable icon;

    /**
     * When set to true, indicates that the icon has been resized.
     * TODO: Find out if needed.
     */
    public boolean filtered;

    public AppInfo() {}

    /**
     * Creates the application intent based on a component name and various launch flags.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    public final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppInfo)) {
            return false;
        }

        AppInfo that = (AppInfo) o;

        return title.equals(that.title) &&
                intent.getComponent().getClassName().equals(
                        that.intent.getComponent().getClassName());
    }

    @Override
    public int hashCode() {
        int result;

        result = (title != null ? title.hashCode() : 0);
        final String name = intent.getComponent().getClassName();
        result = 31 * result + (name != null ? name.hashCode() : 0);

        return result;
    }

    /* Parcelable related methods and fields */

    public AppInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title.toString());
        dest.writeParcelable(intent, 0);

        // Convert icon to bitmap.
        if (icon != null) {
            Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
            dest.writeParcelable(bitmap, 0);
        } else {
            LOGW(TAG, "No icon found in AppInfo for " + title + ", " + intent);
        }
    }

    private void readFromParcel(Parcel in) {
        title = in.readString();
        intent = in.readParcelable(Intent.class.getClassLoader());
        Bitmap bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        icon = new BitmapDrawable(Resources.getSystem(), bitmap);
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new AppInfo(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new AppInfo[size];
        }
    };
}
