package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String KEY = values.getAsString("key");
        String VALUE = values.getAsString("value");
        try
        {
            FileOutputStream os = getContext().openFileOutput(KEY, Context.MODE_PRIVATE);
            os.write(VALUE.getBytes());
            os.close();
        }
        catch (IOException e)
        {
            Log.e(TAG,e.getMessage());
        }
        Log.v("insert",values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {


/**/
        String table_fields[] = {"key", "value"};
        MatrixCursor cursor = new MatrixCursor(table_fields);
        try
        {
            BufferedReader br = new BufferedReader
                                (new InputStreamReader(getContext().openFileInput(selection)));
            /* Building a cursor with single row of values */
            cursor.addRow(new String[]{selection, br.readLine()});

            br.close();
            return cursor;
        }
        catch (IOException e)
        {
            Log.e(TAG,e.getMessage());
        }

        Log.v("query", selection);
        return null;
    }
}
