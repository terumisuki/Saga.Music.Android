package saga.alexwooten.com;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DataHelper {

    private static final String DATABASE_NAME = "saga.db";
    private static final int DATABASE_VERSION = 3;
    private Context context;
    private SQLiteDatabase db;


    // TRACKS SECTION ===============================================================================================================================
    private static final String TBL_TRACKS = "tracks";

    // Insert a track into the tracks table.
    private SQLiteStatement trackInsertStmt;
    private static final String TRACK_INSERT = "insert into " + TBL_TRACKS + "(id, title, file_location, file_downloaded) values (?, ?, ?, ?)";
    public long trackInsert(String mediaId, String title, String fileLocation) {
        if (mediaId.equalsIgnoreCase("0")) {
            return -1;
        }

        this.trackInsertStmt.bindString(1, mediaId);
        this.trackInsertStmt.bindString(2, title);
        this.trackInsertStmt.bindString(3, fileLocation);
        this.trackInsertStmt.bindString(4, "0");
        return this.trackInsertStmt.executeInsert();
    }

    // Clean the tracks table.
    public void tracksDeleteAll() {
        this.db.delete(TBL_TRACKS, null, null);
    }

    // delete a track
    public void trackDelete(int mediaId) {
        this.db.delete(TBL_TRACKS, "id=?", new String[] { Integer.toString(mediaId) });
    }

    // get a track
    public Track trackGet(int mediaId){
        Track track = new Track();
        String q = "SELECT * FROM " + TBL_TRACKS + " WHERE id = " + mediaId + ";";
        Cursor cursor = this.db.rawQuery(q, null);
        if (cursor.moveToFirst()){
            track.mediaId = cursor.getInt(0);
            track.title = cursor.getString(1);
            track.fileLocation = cursor.getString(2);
            String sFileDownloaded = cursor.getString(3);
            if (sFileDownloaded.equals("1")){
                track.fileDownloaded = true;
            } else {
                track.fileDownloaded = false;
            }
        }
        return track;
    }

    public void trackFlagFileDownloaded(int mediaId){
        trackFlagFileUpdate(mediaId, true);
    }

    public void trackFlagFileNeedsDownloading(int mediaId){
        trackFlagFileUpdate(mediaId, false);
    }

    public void trackFlagFileUpdate(int mediaId, boolean on){
        if (mediaId < 1){
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "trackFlagFileUpdate(" + mediaId + ") ..... bad mediaId passed in");
        }
        String onOff = "0";
        if (on){
            onOff = "1";
        } else {
            onOff = "0";
        }
        String strFilter = "id=" + mediaId;
        ContentValues args = new ContentValues();
        args.put("file_downloaded", onOff);
        this.db.update(TBL_TRACKS, args, strFilter, null);
    }

    public List<String> tracksSelectAll() {
        List<String> lstTracks = new ArrayList<String>();
        Cursor cursor = this.db.query(TBL_TRACKS, new String[] { "id" }, null, null, null, null, "id desc");
        if (cursor.moveToFirst()) {
            do {
                lstTracks.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return lstTracks;
    }


    public List<String> tracksSelectAllWithFilesDownloaded() {
        List<String> lstTracks = new ArrayList<String>();
        Cursor cursor = this.db.rawQuery("select * from tracks where file_downloaded = '1'", null);
        if (cursor.moveToFirst()) {
            do {
                lstTracks.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return lstTracks;
    }
    // END TRACKS SECTION ===========================================================================================================================





    // QUEUE SECTION ===============================================================================================================================

    private static final String TBL_QUEUE = "queue";

    // Insert a track into the queue table.
    private SQLiteStatement queueInsertStmt;
    private static final String QUEUE_INSERT = "insert into " + TBL_QUEUE + "(media_id) values (?)";
    public long queueInsert(String mediaId) {
        this.queueInsertStmt.bindString(1, mediaId);
        return this.queueInsertStmt.executeInsert();
    }

    // Clean the queue table.
    public void queueDeleteAll() {
        this.db.delete(TBL_QUEUE, null, null);
    }

    public void queueDelete(int queueId) {
        this.db.delete(TBL_QUEUE, "id=?", new String[] { Integer.toString(queueId) });
    }

    public Track queueGet(int queueId){
        Track track = new Track();
        Cursor cursor = this.db.query(TBL_QUEUE, null, "id = " + queueId, null, null, null, null);
        if (cursor.moveToFirst()){
            track.mediaId = cursor.getInt(0);
        }
        return track;
    }

    // END QUEUE SECTION ===========================================================================================================================




    public int TableRowCount(String tblName){
        String q = "SELECT Count(*) FROM " + tblName + ";";
        int count = 0;
        Cursor cursor = this.db.rawQuery(q, null);
        if (cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        return count;
    }



    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + TBL_TRACKS + "(id integer primary key, title text, file_location text, file_downloaded text)");
            db.execSQL("create table " + TBL_QUEUE + "(id integer primary key AUTOINCREMENT, media_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Example", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("drop table if exists " + TBL_TRACKS);
            db.execSQL("drop table if exists " + TBL_QUEUE);
            onCreate(db);
        }
    }


    public DataHelper(Context context) {
        this.context = context;
        OpenHelper openHelper = new OpenHelper(this.context);
        this.db = openHelper.getWritableDatabase();
        try {
            this.trackInsertStmt = this.db.compileStatement(TRACK_INSERT);
            this.queueInsertStmt = this.db.compileStatement(QUEUE_INSERT);
        } catch (SQLException e){
            e.toString();
        }
    }

}