package de.spas.postman;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by uwe on 09.10.13.
 */
public class GameStorage extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "postmandb";
    private static final String TABLE_POSTSTATION = "poststation";
    private static final String TABLE_LETTER = "letter";
    private static final String TABLE_PLAYER = "player";
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_CREATE_POSTSTATION = "create table " + TABLE_POSTSTATION + " (name text, longitude real, latitude real);";
    private static final String TABLE_CREATE_LETTER = "create table " + TABLE_LETTER + " (target text, value integer);";
    private static final String TABLE_CREATE_PLAYER = "create table " + TABLE_PLAYER + " (cash integer, laststation text);";
    private static final String TABLE_INSERT_PLAYER = "insert into "+TABLE_PLAYER + " (cash) values (?);";
    private final int defaultCash;

    public GameStorage(Context context, int defaultCash) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.defaultCash=defaultCash;
        Log.d("postman", "gameengine created");
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("postman", "creating db");
        try {
            db.execSQL(TABLE_CREATE_POSTSTATION);
            db.execSQL(TABLE_CREATE_LETTER);
            db.execSQL(TABLE_CREATE_PLAYER);
            db.execSQL(TABLE_INSERT_PLAYER,new Object[]{ Integer.valueOf(defaultCash) });
            Log.d("postman", "created db.");
        } catch(Exception e) {
            Log.e("postman", "exception during creation of db: ", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int from, int to) {

    }

    public PostStation findPostStationByName(String name) {
        for(PostStation s : findPostStations()) {
            if(name.equals(s.name)) return s;
        }
        return null;
    }


    class PostStation {
        String name;
        double longitude;
        double latitude;

        @Override
        public boolean equals(Object o) {
            return name.equals(((PostStation)o).name);
        }
    }

    class Letter {
        long id;
        String target;
        int value;
    }

    class Player {
        int cash;
        String lastStation;
    }



    public List<PostStation> findPostStations() {
        List<PostStation> list = new ArrayList<PostStation>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_POSTSTATION,null);
        while(res.moveToNext()) {
            PostStation ps = new PostStation();
            ps.name = res.getString(0);
            ps.longitude = res.getDouble(1);
            ps.latitude = res.getDouble(2);
            list.add(ps);
        }
        res.close();
        db.close();
        Log.d("postman", "found " + list.size() + " poststations");
        return list;
    }

    public List<Letter> findLetters() {
        List<Letter> list = new ArrayList<Letter>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor res = db.rawQuery("select ROWID,* from " + TABLE_LETTER,null);
        while(res.moveToNext()) {
            Letter l = new Letter();
            l.id = res.getLong(0);
            l.target = res.getString(1);
            l.value = res.getInt(2);
            list.add(l);
        }
        res.close();
        db.close();
        Log.d("postman", "found " + list.size() + " letters");
        return list;
    }

    public void addLetter(String target, int value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("target",target);
        values.put("value", value);
        db.insert(TABLE_LETTER,"",values);
        Log.d("postman", "inserted letter to " + target);
    }

    public void deleteLetter(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LETTER,"ROWID=?", new String[] {Long.toString(id)});
        Log.d("postman", "deleted letter " + id);
    }

    public  Player findPlayer() {
        SQLiteDatabase db = getReadableDatabase();
        Player p = new Player();
        Cursor res = db.rawQuery("select * from " + TABLE_PLAYER,null);
        if(res.moveToNext()) {
            p.cash = res.getInt(0);
            p.lastStation = res.getString(1);
        }
        res.close();
        db.close();
        return p;
    }

    public void addPostStation(String name, double longitude, double latitude) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name",name);
        values.put("longitude", longitude);
        values.put("latitude", latitude);
        db.insert(TABLE_POSTSTATION,"",values);
        Log.d("postman", "inserted poststation " + name);
    }


    public void addCash(int cash) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("update " + TABLE_PLAYER + " set cash=cash+?",new Object[] {Integer.valueOf(cash)});
        Log.d("postman", "added cash " + cash);
    }

    public void setLastStation(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("laststation",name);
        db.update(TABLE_PLAYER,values,"",null);
    }

    public PostStation findPostStationNear(double longitude, double latitude, double distance) {
        List<PostStation> list = findPostStations();
        for(PostStation ps : list) {
            float res[] = new float[1];
            Location.distanceBetween(latitude,longitude,ps.latitude,ps.longitude,res);
            if(res[0]<distance) {
                Log.d("postman", "found poststation " + ps.name + " in distance " + res[0] + " to given location");
                return  ps;
            }
        }
        return  null;
    }



}
