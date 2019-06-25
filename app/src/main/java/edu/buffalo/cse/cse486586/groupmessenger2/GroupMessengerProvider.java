package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

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
        //from PA2A
        Context con = getContext();
        String filename = values.get("key").toString();
        String filecontent = values.get("value").toString();

        FileOutputStream outputStream;
        try{
            File file = new File(con.getFilesDir(), filename);
            if(file.exists()){
                file.delete();
            }
            outputStream = new FileOutputStream(new File(con.getFilesDir(), filename));
            outputStream.write(filecontent.getBytes());
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        //Log.v("insert", values.toString());
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
        Context context = getContext();
        File file = new File(context.getFilesDir(), selection);
        StringBuilder txt = new StringBuilder();
        try{
            BufferedReader bR = new BufferedReader(new FileReader(file));
            String line;
            while((line = bR.readLine()) != null){
                txt.append(line);
            }
        }catch (IOException e){
            Log.e("error", "error io");
        }
        Log.v("query", selection);
        MatrixCursor mat = new MatrixCursor(new String[]{"key", "value"});
        mat.addRow(new Object[] {selection, txt.toString()});
        return mat;
    }
}