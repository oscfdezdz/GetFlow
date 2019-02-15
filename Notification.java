package com.wentura.pomodoroapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

class Notification {

    private Context context;
    private boolean isTimerRunning;
    private boolean isBrakeState;
    private boolean isNotificationOpenedFromActivity;

    NotificationCompat.Builder buildNotification(Context context, long millisUntilFinished,
                                                 boolean breakState, boolean timerIsRunning, boolean isNotificationOpenedFromActivity) {
        this.context = context;
        this.isTimerRunning = timerIsRunning;
        this.isBrakeState = breakState;
        this.isNotificationOpenedFromActivity = isNotificationOpenedFromActivity;

        return setupNotification(millisUntilFinished);
    }

    private NotificationCompat.Builder setupNotification(long millisUntilFinished) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_TIMER)
                .setSmallIcon(R.drawable.ic_logo)
                .setColor(context.getColor(R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(Constants.POMODORO)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setShowWhen(false);

        addButtonsToNotification(builder);
        createIntentToOpenApp(builder);
        setTimeLeftNotificationContent(millisUntilFinished, builder);
        displayNotification(builder);

        return builder;
    }

    private void displayNotification(NotificationCompat.Builder builder) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(Constants.TIME_LEFT_NOTIFICATION, builder.build());
    }

    private void setTimeLeftNotificationContent(long millisUntilFinished, NotificationCompat.Builder builder) {
        if (isBrakeState) {
            builder.setContentText(context.getString(R.string.break_time_left) + " " + calculateTimeLeft
                    (millisUntilFinished));
        } else {
            builder.setContentText(context.getString(R.string.work_time_left) + " " + calculateTimeLeft
                    (millisUntilFinished));
        }
    }

    private void createIntentToOpenApp(NotificationCompat.Builder builder) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                Constants.PENDING_INTENT_OPEN_APP_REQUEST_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentIntent(pendingIntent);
    }

    private void addButtonsToNotification(NotificationCompat.Builder builder) {
        addPauseResumeButton(builder);
        addSkipButton(builder);
        addStopButton(builder);
    }

    private void addStopButton(NotificationCompat.Builder builder) {
        builder.addAction(R.drawable.ic_stop_button, context.getString(R.string.stop),
                createButtonPendingIntent(Constants.BUTTON_STOP));
    }

    private void addSkipButton(NotificationCompat.Builder builder) {
        builder.addAction(R.drawable.ic_skip_button, context.getString(R.string.skip),
                createButtonPendingIntent(Constants.BUTTON_SKIP));
    }

    private void addPauseResumeButton(NotificationCompat.Builder builder) {
        if (isTimerRunning) {
            builder.addAction(R.drawable.ic_play_button, context.getString(R.string.pause),
                    createButtonPendingIntent(Constants.BUTTON_PAUSE_RESUME));
        } else {
            builder.addAction(R.drawable.ic_play_button, context.getString(R.string.resume),
                    createButtonPendingIntent(Constants.BUTTON_PAUSE_RESUME));
        }
    }

    private PendingIntent createButtonPendingIntent(String actionValue) {
        return PendingIntent.getBroadcast(context, getRequestCode(actionValue),
                createButtonIntent(actionValue), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int getRequestCode(String actionValue) {
        switch (actionValue) {
            case Constants.BUTTON_SKIP:
                return Constants.PENDING_INTENT_SKIP_REQUEST_CODE;
            case Constants.BUTTON_PAUSE_RESUME:
                return Constants.PENDING_INTENT_PAUSE_RESUME_REQUEST_CODE;
            case Constants.BUTTON_STOP:
                return Constants.PENDING_INTENT_STOP_REQUEST_CODE;
            default:
                return -1;
        }
    }

    private String calculateTimeLeft(long milliseconds) {
        return formatTime(getMinutes(milliseconds), getSeconds(milliseconds));
    }

    @NonNull
    private String formatTime(int minutes, int seconds) {
        String timeLeft;

        timeLeft = "" + minutes;
        timeLeft += ":";
        if (seconds < 10) {
            timeLeft += "0";
        }
        timeLeft += "" + seconds;
        return timeLeft;
    }

    private int getSeconds(long milliseconds) {
        return (int) (milliseconds % 60000 / 1000);
    }

    private int getMinutes(long milliseconds) {
        return (int) (milliseconds / 60000);
    }

    @NonNull
    private Intent createButtonIntent(String actionValue) {
        Intent buttonIntent;
        if (isNotificationOpenedFromActivity) {
            buttonIntent = new Intent(context, ActivityNotificationButtonReceiver.class);
        } else {
            buttonIntent = new Intent(context, NonActivityNotificationButtonReceiver.class);
        }
        buttonIntent.putExtra(Constants.BUTTON_ACTION, actionValue);
        return buttonIntent;
    }
}