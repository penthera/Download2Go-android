package com.penthera.download2go7;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.autodownload.IPlaylist;
import com.penthera.virtuososdk.client.autodownload.IPlaylistManager;
import com.penthera.virtuososdk.client.database.PlaylistItemColumns;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlaylistItemsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    static final String TAG = PlaylistItemsActivity.class.getName();

    static final String PLAYLIST_NAME = "playlist_name";

    static final String[] PROJECTION = new String[]{
        PlaylistItemColumns._ID,
        PlaylistItemColumns.ASSET_ID,
        PlaylistItemColumns.STATUS,
        PlaylistItemColumns.DOWNLOADED,
        PlaylistItemColumns.DELETED,
        PlaylistItemColumns.PLAYED_BACK,
        PlaylistItemColumns.PENDING,
        PlaylistItemColumns.LAST_PENDING
        };

    static final int LOADER_ID = 1;

    PlaylistItemAdapter playlistAdapter;
    Virtuoso mVirtuoso;

    private String playlistName;

    private ListView mList;
    private View mEmptyView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_activity);

        mEmptyView = findViewById(android.R.id.empty);
        mList = findViewById(android.R.id.list);
        mList.setEmptyView(mEmptyView);
        mVirtuoso = new Virtuoso(this);

        playlistAdapter = new PlaylistItemAdapter(this, null);
        mList.setAdapter(playlistAdapter);


        Intent intent = getIntent();
        playlistName = intent.getStringExtra(PLAYLIST_NAME);

        if (playlistName != null) {
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
            setIntent(intent);
    }



    // onCreateLoader
    @Override
    @NonNull
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        IPlaylist playlist = mVirtuoso.getAssetManager().getPlaylistManager().find(playlistName);

        Log.i(TAG, "onCreateLoader for playlist " + playlistName);
        return new CursorLoader(this,
        playlist != null ? playlist.getItemsContentUri() : Uri.parse("content://pass_something_to_prevent_crash"),
        PROJECTION,null,null,null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        IPlaylist playlist = mVirtuoso.getAssetManager().getPlaylistManager().find(playlistName);
        Log.i(TAG, "onCreateLoader");

        if (data != null) {
            data.setNotificationUri(getContentResolver(),playlist != null ? playlist.getItemsContentUri() : Uri.parse("content://pass_something_to_prevent_crash") );
            playlistAdapter.swapCursor(data);
        }
    }

    // onLoaderReset
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset");

        if (playlistAdapter != null)
            playlistAdapter.swapCursor(null);
    }



    private class PlaylistItemAdapter extends CursorAdapter {

        private LayoutInflater mInflater;
        private IPlaylistManager playlistManager;
        private SimpleDateFormat dateFormat;

        public PlaylistItemAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mInflater = LayoutInflater.from(context);
            playlistManager = mVirtuoso.getAssetManager().getPlaylistManager();
            dateFormat = new SimpleDateFormat("MM/dd/yyyy',' HH:mm:ss a", Locale.US);
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {

            String assetId = cursor.getString(cursor.getColumnIndex(PlaylistItemColumns.ASSET_ID));
            int status = cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.STATUS));
            long downloadedTimestamp = cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.DOWNLOADED));
            boolean deleted = cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.DELETED)) > 0;
            boolean expired = cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.EXPIRED)) > 0;
            long playedBackTimestamp = cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.PLAYED_BACK));
            boolean pending = cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.PENDING)) > 0;
            long lastPending = cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.LAST_PENDING));

            TextView nameTxt = view.findViewById(R.id.playlistitem_name);
            nameTxt.setText(assetId);

            TextView statusTxt = view.findViewById(R.id.playlistitem_status);
            statusTxt.setText(playlistManager.PlaylistItemStatusAsString(status));

            TextView downloadedTxt = view.findViewById(R.id.playlistitem_downloaded);
            downloadedTxt.setText(formattedStringFromTimestamp(downloadedTimestamp, true));

            TextView deletedTxt = view.findViewById(R.id.playlistitem_deleted);
            deletedTxt.setText(deleted ? "yes" : "no");

            TextView expiredTxt = view.findViewById(R.id.playlistitem_expired);
            expiredTxt.setText(expired ? "yes" : "no");

            TextView playbackTxt = view.findViewById(R.id.playlistitem_playback);
            playbackTxt.setText(formattedStringFromTimestamp(playedBackTimestamp, true));

            TextView pendingTxt = view.findViewById(R.id.playlistitem_pending);
            pendingTxt.setText(pending ? "yes" : "no");

            TextView lastPendingTxt = view.findViewById(R.id.playlistitem_lastpending);
            lastPendingTxt.setText(formattedStringFromTimestamp(lastPending, false));
        }

        String formattedStringFromTimestamp(long timestamp, boolean showYesNo) {
            if (timestamp > 0) {
                return (showYesNo ? "yes " : "") + dateFormat.format(new Date(timestamp));
            } else {
                return showYesNo ? "no" : "";
            }
        }

        @Override
        public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
            return mInflater.inflate(R.layout.playlistitem_row, arg2 ,false);
        }
    }
}
