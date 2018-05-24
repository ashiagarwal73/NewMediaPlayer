package com.agarwal.ashi.newmediaplayer;

import android.app.Activity;
import android.app.Notification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import android.widget.RemoteViews;

import java.io.IOException;

public class NotificationService extends Service {
    Callbacks activity;
    MediaPlayer mediaPlayer;
    RemoteViews notificationLayout;
    NotificationManagerCompat notificationManager;
    private final IBinder mBinder = new LocalBinder();
    Handler handler = new Handler();
    PendingIntent pendingIntent;
    int x = 0;
    String url = "Enter URL"; // your URL here
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public NotificationService getServiceInstance() {
            return NotificationService.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity) {
        this.activity = (Callbacks) activity;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    activity.updateClient(mediaPlayer);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });

            handleNotification(intent);
        } else {
            handleNotification(intent);

        }

        return Service.START_STICKY;
    }

    private void handleNotification(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case Constants.ACTION.STARTFOREGROUND_ACTION:
                    showNotification();
                    break;
                case Constants.ACTION.PLAY_ACTION:
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        notificationLayout.setImageViewResource(R.id.playnotify, R.drawable.ic_play_arrow_black_24dp);
                        updateNotification();

                    } else {
                        mediaPlayer.start();
                        notificationLayout.setImageViewResource(R.id.playnotify, R.drawable.ic_pause_black_24dp);
                        updateNotification();
                    }

                    break;
                case Constants.ACTION.PREV_ACTION: {
                    int pos = mediaPlayer.getCurrentPosition();
                    mediaPlayer.seekTo(pos - 2000);
                    break;
                }
                case Constants.ACTION.NEXT_ACTION: {
                    int pos = mediaPlayer.getCurrentPosition();
                    mediaPlayer.seekTo(pos + 2000);
                    break;
                }
            }
        }
    }

    private void showNotification() {
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.playnotify, pendingPlayIntent);
        Intent rewindIntent = new Intent(this, NotificationService.class);
        rewindIntent.setAction(Constants.ACTION.PREV_ACTION);
        PendingIntent pendingRewindIntent = PendingIntent.getService(this, 0,
                rewindIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.rewindnotify, pendingRewindIntent);
        Intent forwardIntent = new Intent(this, NotificationService.class);
        forwardIntent.setAction(Constants.ACTION.NEXT_ACTION);
        PendingIntent pendingForwardIntent = PendingIntent.getService(this, 0,
                forwardIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.forwardnotify, pendingForwardIntent);
        updateNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(1);
    }

    public void updateNotification() {
        Notification customNotification = new NotificationCompat.Builder(this, "notify")
                .setSmallIcon(R.drawable.ic_music_note_black_24dp)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, customNotification);
    }

    public interface Callbacks {
        void updateClient(MediaPlayer mediaPlayer);
    }
}
