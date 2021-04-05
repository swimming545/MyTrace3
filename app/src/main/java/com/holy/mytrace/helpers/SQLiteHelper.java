package com.holy.mytrace.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.holy.mytrace.models.Waypoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SQLiteHelper extends SQLiteOpenHelper {

    // 데이터베이스 이름
    private static final String DATABASE_NAME = "database";
    // 현재 버전
    private static final int DATABASE_VERSION = 3;

    // 일 테이블의 정보
    public static final String TABLE_WAYPOINTS = "waypoints";
    public static final String COLUMN_WAYPOINT_ID = "id";
    public static final String COLUMN_WAYPOINT_LATITUDE = "latitude";
    public static final String COLUMN_WAYPOINT_LONGITUDE = "longitude";
    public static final String COLUMN_WAYPOINT_BEGIN_TIME = "begin_time";
    public static final String COLUMN_WAYPOINT_END_TIME = "end_time";

    // 데이터베이스 헬퍼 객체
    private static SQLiteHelper instance;

    // 데이터베이스 헬퍼 객체 구하기.
    public static synchronized SQLiteHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SQLiteHelper(context.getApplicationContext());
        }
        return instance;
    }

    // 생성자
    public SQLiteHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 초기화 : 데이터베이스에 테이블을 생성한다.
        String CREATE_WAYPOINTS_TABLE = "CREATE TABLE " + TABLE_WAYPOINTS +
                "(" +
                COLUMN_WAYPOINT_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_WAYPOINT_LATITUDE + " NUMBER NOT NULL, " +
                COLUMN_WAYPOINT_LONGITUDE + " NUMBER NOT NULL, " +
                COLUMN_WAYPOINT_BEGIN_TIME + " TEXT NOT NULL, " +
                COLUMN_WAYPOINT_END_TIME + " TEXT NOT NULL" +
                ")";
        db.execSQL(CREATE_WAYPOINTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // 기존의 데이터베이스 버전이 현재와 다르면 테이블을 지우고 빈 테이블 다시 만들기.
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WAYPOINTS);
            onCreate(db);
        }
    }

    // 정보 추가

    public void addWaypoint(Waypoint waypoint) {

        // 쓰기용 DB 를 연다.
        SQLiteDatabase db = getWritableDatabase();

        // DB 입력 시작
        db.beginTransaction();
        try {
            // 정보를 모두 values 객체에 입력한다
            ContentValues values = new ContentValues();
            values.put(COLUMN_WAYPOINT_LATITUDE, waypoint.getLatitude());
            values.put(COLUMN_WAYPOINT_LONGITUDE, waypoint.getLongitude());
            values.put(COLUMN_WAYPOINT_BEGIN_TIME, waypoint.getBeginTime().toString());
            values.put(COLUMN_WAYPOINT_END_TIME, waypoint.getEndTime().toString());

            // 데이터베이스에 values 를 입력한다.
            db.insertOrThrow(TABLE_WAYPOINTS, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // 날짜로 검색

    public List<Waypoint> getWaypointsByDate(int year, int month, int date) {

        List<Waypoint> waypointList = new ArrayList<>();
        String strDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month,date);

        // 읽기용 DB 열기
        SQLiteDatabase db = getReadableDatabase();

        // 데이터베이스의 테이블을 가리키는 커서를 가져온다.
        String SELECT_WAYPOINTS =
                "SELECT * FROM " + TABLE_WAYPOINTS
                        + " WHERE " + COLUMN_WAYPOINT_BEGIN_TIME + " LIKE '" + strDate + "%'"
                        + " ORDER BY " + COLUMN_WAYPOINT_BEGIN_TIME + " DESC";
        Cursor cursor = db.rawQuery(SELECT_WAYPOINTS, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    // 커서를 움직이면서 테이블의 정보들을 가져온다.
                    int id = cursor.getInt(cursor.getColumnIndex(COLUMN_WAYPOINT_ID));
                    double latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_WAYPOINT_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_WAYPOINT_LONGITUDE));
                    String strBeginTime = cursor.getString(cursor.getColumnIndex(COLUMN_WAYPOINT_BEGIN_TIME));
                    String strEndTime = cursor.getString(cursor.getColumnIndex(COLUMN_WAYPOINT_END_TIME));

                    // 정보로 객체를 만들어 리스트에 추가한다.
                    Waypoint waypoint = new Waypoint(id, latitude, longitude,
                            LocalDateTime.parse(strBeginTime), LocalDateTime.parse(strEndTime));
                    waypointList.add(waypoint);

                    // 테이블 끝에 도달할 때까지 실시한다.
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return waypointList;
    }

}
