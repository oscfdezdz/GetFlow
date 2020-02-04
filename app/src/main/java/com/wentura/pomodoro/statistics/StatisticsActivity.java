package com.wentura.pomodoro.statistics;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wentura.pomodoro.Constants;
import com.wentura.pomodoro.R;
import com.wentura.pomodoro.Utility;
import com.wentura.pomodoro.database.Database;

import java.lang.ref.WeakReference;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    private Database database;

    // SQL database contains only records with at least one completed work or break sessions
    // This method fills the rest of the days for consistent statistic showing
    private static void createDatesWithoutPomodoros(List<StatisticsItem> statisticsItems) {
        int days = 0;

        if (statisticsItems.isEmpty()) {
            for (int i = 0; i < Constants.HOW_MANY_DAYS_TO_SHOW; i++) {
                statisticsItems.add(i, new StatisticsItem(Utility.subtractDaysFromCurrentDate(days),
                        0, 0, 0, 0, 0, 0));
                days++;
            }
        } else {
            for (int i = 0; i < Constants.HOW_MANY_DAYS_TO_SHOW; i++) {
                if (i >= statisticsItems.size()) {
                    statisticsItems.add(i, new StatisticsItem(Utility.subtractDaysFromCurrentDate(days),
                            0, 0, 0, 0, 0, 0));
                } else {
                    if (!statisticsItems.get(i).getDate().equals(Utility.subtractDaysFromCurrentDate(days))) {
                        statisticsItems.add(i, new StatisticsItem(Utility.subtractDaysFromCurrentDate(days),
                                0, 0, 0, 0, 0, 0));
                    }
                }
                days++;
            }
        }
    }

    private static void formatDates(List<StatisticsItem> statisticsItems) {
        for (int i = 0; i < statisticsItems.size(); i++) {
            statisticsItems.get(i).setDate(Utility.formatDate(statisticsItems.get(i).getDate()));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        database = Database.getInstance(this);

        new LoadFromDatabase(this).execute();
    }

    private static class LoadFromDatabase extends AsyncTask<Void, Void, Void> {
        List<StatisticsItem> statisticsItems;

        private WeakReference<StatisticsActivity> activityWeakReference;

        LoadFromDatabase(StatisticsActivity context) {
            this.activityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            StatisticsActivity statisticsActivity = activityWeakReference.get();

            if (statisticsActivity == null || statisticsActivity.isFinishing()) {
                return null;
            }

            statisticsItems =
                    statisticsActivity.database.pomodoroDao().getAllDatesBetween(Utility.subtractDaysFromCurrentDate(Constants.HOW_MANY_DAYS_TO_SHOW - 1),
                            Utility.getCurrentDate());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StatisticsActivity statisticsActivity = activityWeakReference.get();

            if (statisticsActivity == null || statisticsActivity.isFinishing()) {
                return;
            }

            RecyclerView recyclerView = statisticsActivity.findViewById(R.id.recycler_view);

            createDatesWithoutPomodoros(statisticsItems);
            formatDates(statisticsItems);

            if (!statisticsItems.isEmpty()) {
                statisticsItems.get(0).setDate(statisticsActivity.getString(R.string.today));
            }

            StatisticsAdapter statisticsAdapter = new StatisticsAdapter(statisticsItems);

            recyclerView.setAdapter(statisticsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(statisticsActivity));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.background_down, R.anim.foreground_down);
    }
}