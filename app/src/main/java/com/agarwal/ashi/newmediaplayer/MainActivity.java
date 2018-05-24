package com.agarwal.ashi.newmediaplayer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements NotificationService.Callbacks {
    MediaPlayer mediaPlayer;
    Intent serviceIntent;
    NotificationService notificationService;
    LinearLayout customMediaController;
    Button playPauseButton, forwardButton, rewindButton;
    SeekBar seekBar;
    TextView timer;
    ProgressBar progressBar;
    Handler handler = new Handler();
    RemoteViews notificationLayout;
    PendingIntent pendingIntent;
    NotificationManagerCompat notificationManager;
    final Integer CALL = 0x2;
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;
    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;
    int permissionGranted = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(MainActivity.this, NotificationService.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        customMediaController = findViewById(R.id.customMediaController);
        progressBar = findViewById(R.id.progressBar);
        playPauseButton = findViewById(R.id.bttn);
        forwardButton = findViewById(R.id.forward);
        rewindButton = findViewById(R.id.rewind);
        timer = findViewById(R.id.timer);
        seekBar = findViewById(R.id.seekBar);
        handler.postDelayed(runnable, 1000);
        setNotification();
        askForPermission(Manifest.permission.READ_PHONE_STATE, CALL);
        if (permissionGranted == 1) {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        //Incoming call: Pause music
                        pauseMusic();
                    } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                        //Not in call: Play music
                        if (mediaPlayer != null && !mediaPlayer.isPlaying())
                            playMusic();
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        //A call is dialing, active or on hold
                        pauseMusic();
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            };
            telephonyManager = (TelephonyManager) this
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.i("", "AUDIOFOCUS_GAIN");
                        playMusic();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        pauseMusic();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        pauseMusic();
                        break;
                }
            }
        };
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for play back
        if (audioManager != null) {
            audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    @Override
    public void updateClient(final MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        if (this.mediaPlayer != null) {
            seekBar.setMax(this.mediaPlayer.getDuration());
            this.mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    progressBar.setSecondaryProgress(percent);
                }
            });
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekMediaPlayer(seekBar);
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.seekTo(0);
                    mp.start();
                }
            });
        }
    }


    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    playPauseButton.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                } else {
                    playPauseButton.setBackgroundResource(R.drawable.ic_play_circle_outline_black_24dp);
                }
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                int t = currentPosition / 1000;
                int min = t / 60;
                int sec = t % 60;
                if (min < 10) {
                    if (sec < 10) {
                        String string = "0" + min + ":0" + sec;
                        timer.setText(string);
                    } else {
                        String string = "0" + min + ":" + sec;
                        timer.setText(string);
                    }
                }
                if (min >= 10) {
                    if (sec < 10) {
                        String string = min + ":0" + sec;
                        timer.setText(string);
                    } else {
                        String string = min + ":" + sec;
                        timer.setText(string);
                    }
                }
            }
            handler.postDelayed(runnable, 1000);
        }
    };

    public void onForwardButtonClick(View view) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 2000);
            seekBar.setProgress(mediaPlayer.getCurrentPosition() + 2000);
        }
    }

    public void onRewindButtonClick(View view) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 2000);
            seekBar.setProgress(mediaPlayer.getCurrentPosition() - 2000);
        }
    }

    public void onPlayPauseClick(View view) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            } else {
                playMusic();
            }
        }
    }

    public void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            playPauseButton.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
            notificationLayout.setImageViewResource(R.id.playnotify, R.drawable.ic_pause_black_24dp);
            updateNotification();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playPauseButton.setBackgroundResource(R.drawable.ic_play_circle_outline_black_24dp);
            notificationLayout.setImageViewResource(R.id.playnotify, R.drawable.ic_play_arrow_black_24dp);
            updateNotification();
        }
    }

    private void seekMediaPlayer(SeekBar seekBar) {
        mediaPlayer.seekTo(seekBar.getProgress());
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            notificationService = binder.getServiceInstance();
            notificationService.registerClient(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(serviceIntent);
        unbindService(mConnection);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

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

    public void setNotification() {
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
    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            permissionGranted = 1;
        }
    }

}
