package ch.epfl.unison;

import java.util.HashSet;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class LibraryHelper {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.LibraryHelper";

    private static final String DATABASE_NAME = "library.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "lib_entry";

    public static final String C_ID = BaseColumns._ID;
    public static final String C_LOCAL_ID = "local_id";
    public static final String C_ARTIST = "artist";
    public static final String C_TITLE = "title";

    private static final String WHERE_ALL = C_LOCAL_ID + " = ? AND "
            + C_ARTIST + " = ? AND " + C_TITLE + " = ?";

    private final OpenHelper dbHelper;

    public LibraryHelper(Context context) {
        this.dbHelper = new OpenHelper(context);
    }

    public Set<MusicItem> getEntries() {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();
        Cursor cur = db.query(TABLE_NAME,
                new String[] { C_LOCAL_ID, C_ARTIST, C_TITLE },
                null, null, null, null, null);
        Set<MusicItem> set = new HashSet<MusicItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(C_LOCAL_ID);
            int colArtist = cur.getColumnIndex(C_ARTIST);
            int colTitle = cur.getColumnIndex(C_TITLE);
            do {
                set.add(new MusicItem(cur.getInt(colId),
                        cur.getString(colArtist), cur.getString(colTitle)));
            } while (cur.moveToNext());
        }
        db.close();
        return set;
    }

    public boolean isEmpty() {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();
        Cursor cur = db.query(TABLE_NAME,
                new String[] { C_LOCAL_ID, C_ARTIST, C_TITLE },
                null, null, null, null, null);
        boolean isEmpty = !cur.moveToFirst();
        db.close();
        return isEmpty;
    }

    public void insert(MusicItem item) {
        ContentValues values = new ContentValues();
        values.put(C_LOCAL_ID, item.localId);
        values.put(C_ARTIST, item.artist);
        values.put(C_TITLE, item.title);

        SQLiteDatabase db = this.dbHelper.getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void delete(MusicItem item) {
        SQLiteDatabase db = this.dbHelper.getWritableDatabase();
        db.delete(TABLE_NAME, WHERE_ALL,
                new String[] { String.valueOf(item.localId), item.artist, item.title});
        db.close();
    }

    public boolean exists(MusicItem item) {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME,
                new String[] { C_LOCAL_ID },
                WHERE_ALL,
                new String[] { String.valueOf(item.localId), item.artist, item.title},
                null, null, null, "1");  // LIMIT 1
        boolean exists = c.moveToFirst();
        db.close();
        return exists;
    }

    public void truncate() {
        SQLiteDatabase db = this.dbHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    public void close() {
        this.dbHelper.close();
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        private static final String DATABASE_SCHEMA = "CREATE TABLE " + TABLE_NAME + " ("
                + C_ID + " int PRIMARY KEY, " + C_LOCAL_ID + " int UNIQUE, "
                + C_ARTIST + " text, " + C_TITLE + " text)";

        public OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_SCHEMA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            this.onCreate(db);
        }
    }

}
