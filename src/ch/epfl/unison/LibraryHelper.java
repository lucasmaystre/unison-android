package ch.epfl.unison;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class LibraryHelper {

    private static final String DATABASE_NAME = "library.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "lib_entry";
    private static final String C_ID = BaseColumns._ID;
    private static final String C_LOCAL_ID = "local_id";
    private static final String C_ARTIST = "artist";
    private static final String C_TITLE = "title";

    private final OpenHelper dbHelper;

    public LibraryHelper(Context context) {
        this.dbHelper = new OpenHelper(context);
    }

    public Cursor getEntries() {
        SQLiteDatabase db = this.dbHelper.getReadableDatabase();
        return db.query(TABLE_NAME,
                new String[] { C_LOCAL_ID, C_ARTIST, C_TITLE },
                null, null, null, null, null);
    }

    public boolean isEmpty() {
        Cursor c = this.getEntries();
        return c.moveToFirst();
    }

    public void insertOrIgnore(ContentValues values) {
        SQLiteDatabase db = this.dbHelper.getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
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
