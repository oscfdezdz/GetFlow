package com.wentura.focus;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.wentura.focus.database.Database;
import com.wentura.focus.database.Pomodoro;

class UpdateDatabaseBreaks extends AsyncTask<Void, Void, Void> {
    private static final String TAG = UpdateDatabaseBreaks.class.getSimpleName();
    private Database database;
    private int time;

    UpdateDatabaseBreaks(Context context, int time) {
        this.database = Database.getInstance(context);
        this.time = time;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String currentDate = Utility.getCurrentDate();

        Log.d(TAG, "doInBackground: Update database breaks");

        if (database.pomodoroDao().getLatestDate() != null && database.pomodoroDao().getLatestDate().equals(currentDate)) {
            database.pomodoroDao().updateBreaks(database.pomodoroDao().getBreaks(currentDate) + 1, currentDate);
            database.pomodoroDao().updateBreakTime(database.pomodoroDao().getBreakTime(currentDate) + time, currentDate);
        } else {
            database.pomodoroDao().insertPomodoro(new Pomodoro(currentDate, 0, 0, 0,
                    0, 1, time));
        }
        return null;
    }
}