package com.wentura.pomodoro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static com.wentura.pomodoro.Constants.BREAK_DURATION_SETTING;
import static com.wentura.pomodoro.Constants.IS_BREAK_STATE;
import static com.wentura.pomodoro.Constants.IS_TIMER_RUNNING;
import static com.wentura.pomodoro.Constants.TIMER_LEFT_IN_MILLISECONDS;
import static com.wentura.pomodoro.Constants.WORK_DURATION_SETTING;

public class NotificationButtonReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationButtonReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(Constants.BUTTON_ACTION);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editPreferences = preferences.edit();

        switch (action) {
            case Constants.BUTTON_STOP: {
                stopNotificationService(context);
                stopEndNotificationService(context);

                editPreferences.putBoolean(IS_TIMER_RUNNING, false);
                editPreferences.putBoolean(IS_BREAK_STATE, false);
                editPreferences.apply();

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.BUTTON_ACTION, Constants.BUTTON_STOP);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);

                Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                break;
            }
            case Constants.BUTTON_SKIP: {
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                stopEndNotificationService(context);
                stopNotificationService(context);

                if (isBreakState) {
                    editPreferences.putBoolean(IS_BREAK_STATE, false);

                    editPreferences.putInt(Constants.LAST_SESSION_DURATION,
                            Integer.parseInt(preferences.getString(WORK_DURATION_SETTING,
                                    Constants.DEFAULT_WORK_TIME)));

                    if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("myDebug")) {
                        editPreferences.putLong(TIMER_LEFT_IN_MILLISECONDS,
                                Integer.parseInt(preferences.getString(WORK_DURATION_SETTING,
                                        Constants.DEFAULT_WORK_TIME)));
                    } else {
                        editPreferences.putLong(TIMER_LEFT_IN_MILLISECONDS,
                                Integer.parseInt(preferences.getString(WORK_DURATION_SETTING,
                                        Constants.DEFAULT_WORK_TIME)) * 60000);
                    }

                    Utility.toggleDoNotDisturb(context, RINGER_MODE_SILENT);
                } else {
                    editPreferences.putBoolean(IS_BREAK_STATE, true);

                    editPreferences.putInt(Constants.LAST_SESSION_DURATION,
                            Integer.parseInt(preferences.getString(BREAK_DURATION_SETTING,
                                    Constants.DEFAULT_BREAK_TIME)));

                    if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("myDebug")) {
                        editPreferences.putLong(TIMER_LEFT_IN_MILLISECONDS,
                                Integer.parseInt(preferences.getString(WORK_DURATION_SETTING,
                                        Constants.DEFAULT_WORK_TIME)));
                    } else {
                        editPreferences.putLong(TIMER_LEFT_IN_MILLISECONDS,
                                Integer.parseInt(preferences.getString(WORK_DURATION_SETTING,
                                        Constants.DEFAULT_WORK_TIME)) * 60000);
                    }
                    Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                }
                editPreferences.putBoolean(IS_TIMER_RUNNING, true);
                editPreferences.apply();

                Intent updateTimer = new Intent(Constants.BUTTON_CLICKED);
                updateTimer.putExtra(Constants.BUTTON_ACTION, Constants.BUTTON_SKIP);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateTimer);

                startNotificationService(context);
                break;
            }
            case Constants.BUTTON_START: {
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                editPreferences.putBoolean(IS_TIMER_RUNNING, true);
                editPreferences.apply();

                startNotificationService(context);

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.BUTTON_ACTION, Constants.BUTTON_START);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);

                if (!isBreakState) {
                    Utility.toggleDoNotDisturb(context, RINGER_MODE_SILENT);
                }
                break;
            }
            case Constants.BUTTON_PAUSE: {
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                long timerLeftInMilliseconds =
                        preferences.getLong(Constants.TIMER_LEFT_IN_MILLISECONDS, 0);

                editPreferences.putBoolean(IS_TIMER_RUNNING, false);
                editPreferences.putInt(Constants.LAST_SESSION_DURATION, (int) timerLeftInMilliseconds / 60000);
                editPreferences.apply();

                if (!isBreakState) {
                    Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                }

                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.putExtra(Constants.NOTIFICATION_SERVICE,
                        Constants.NOTIFICATION_SERVICE_PAUSE);
                context.startService(serviceIntent);

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.BUTTON_ACTION, Constants.BUTTON_PAUSE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);
                break;
            }
        }
    }

    private void stopNotificationService(Context context) {
        Intent stopService = new Intent(context, NotificationService.class);
        context.stopService(stopService);
    }

    private void stopEndNotificationService(Context context) {
        Intent stopService = new Intent(context, EndNotificationService.class);
        context.stopService(stopService);
    }

    private void startNotificationService(Context context) {
        Intent startService = new Intent(context, NotificationService.class);
        context.startService(startService);
    }
}
