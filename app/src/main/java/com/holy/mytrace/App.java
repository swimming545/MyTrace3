package com.holy.mytrace;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.holy.mytrace.helpers.SQLiteHelper;

import java.time.LocalDateTime;

public class App extends Application {

    public static final String CHANNEL_NAME = "MyTrace channel";
    public static final String CHANNEL_ID = "com.holy.mytrace.notification_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // 14일 지난 기록을 폐기한다
        LocalDateTime fortnightAgo = LocalDateTime.now().minusDays(14);
        SQLiteHelper.getInstance(this).discardWaypoints(fortnightAgo);

        // 노티피케이션 채널 생성
        createNotificationChannel();
    }

    private void createNotificationChannel() {

        // n Oreo 이상 운영체제이면 노티피케이션 채널을 만든다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}
