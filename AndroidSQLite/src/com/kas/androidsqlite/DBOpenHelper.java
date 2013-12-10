package com.kas.androidsqlite;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Kassim on 10/3/13.
 */
public class DBOpenHelper extends SQLiteOpenHelper {

	private String DB_PATH;
    private String DB_NAME;
    private final Context myContext;

    /**
     * 
     * @param context
     * @param DB_NAME Database name in the assets folder for example data.db 
     */
    public DBOpenHelper (Context context, String DB_NAME) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
        this.DB_NAME = DB_NAME;
        DB_PATH = myContext.getApplicationContext().getFilesDir().getPath().replace("files", "databases/");
    }

    /**
     * 
     * @throws IOException
     */
    public void createDatabase() throws IOException {

        if (!isDatabaseExist()) {
            this.getReadableDatabase();

            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * This method deletes the current database if it exist
     * @return returns true if it was deleted
     */
    public boolean deleteDatabase() {
    	if(isDatabaseExist()){
    		String outFileName = DB_PATH + DB_NAME;
        	File file = new File(outFileName);
        	return file.delete();
    	}
    	return false;
    }
    
    private void copyDataBase() throws IOException {

        // Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        // Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        // transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    private boolean isDatabaseExist() {
        SQLiteDatabase sqliteDatabase = null;

        try {
            String myPath = DB_PATH + DB_NAME;
            sqliteDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        } catch (SQLiteException e) {
        	sqliteDatabase = null;
        }

        if (sqliteDatabase != null) {
        	sqliteDatabase.close();
        }
        return sqliteDatabase != null ? true : false;
    }

    public SQLiteDatabase openDataBase() throws SQLException {
        // Open the database
        String myPath = DB_PATH + DB_NAME;
        return SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
