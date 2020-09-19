/*
 * Copyright (C) 2020 Adrian Miozga <AdrianMiozga@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.wentura.focus.statistics;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.wentura.focus.Constants;
import com.wentura.focus.R;
import com.wentura.focus.Utility;
import com.wentura.focus.database.Database;
import com.wentura.focus.statistics.activitychart.ActivityPieChartRenderer;
import com.wentura.focus.statistics.activitychart.CustomPercentFormatter;
import com.wentura.focus.statistics.activitychart.LegendAdapter;
import com.wentura.focus.statistics.activitychart.LegendItem;
import com.wentura.focus.statistics.activitychart.PieChartItem;
import com.wentura.focus.statistics.historychart.ChartData;
import com.wentura.focus.statistics.historychart.DayData;
import com.wentura.focus.statistics.historychart.HistoryChart;
import com.wentura.focus.statistics.historychart.HistoryChartItem;
import com.wentura.focus.statistics.historychart.MonthData;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    private static List<HistoryChartItem> data = new ArrayList<>();
    private static HistoryChart historyChart;
    private static PieChart pieChart;
    private static int historySpinnerCurrentSelectedIndex;
    private static int activitiesSpinnerCurrentSelectedIndex;
    private static ChartData monthData;
    private static ChartData dayData;
    private Database database;
    private TextView numberTodayTextView;
    private TextView numberWeekTextView;
    private TextView numberMonthTextView;
    private TextView numberTotalTextView;
    private Spinner historySpinner;
    private Spinner activitiesSpinner;
    private RecyclerView legendRecyclerView;
    private TextView othersTextView;
    private PieData pieData;
    private boolean isActivitySpinnerSelectionFromTouch;
    private List<LegendItem> legendItems;
    private boolean shouldScrollDown;
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        historySpinner = findViewById(R.id.history_spinner);
        activitiesSpinner = findViewById(R.id.activities_spinner);
        numberTodayTextView = findViewById(R.id.number_today_text_view);
        numberWeekTextView = findViewById(R.id.number_week_text_view);
        numberMonthTextView = findViewById(R.id.number_month_text_view);
        numberTotalTextView = findViewById(R.id.number_total_text_view);

        scrollView = findViewById(R.id.statistics_scroll_view);
        scrollView.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        legendRecyclerView = findViewById(R.id.legend_recycler_view);
        legendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        legendRecyclerView.setHasFixedSize(true);

        othersTextView = findViewById(R.id.others_text_view);

        pieChart = findViewById(R.id.pie_chart);

        database = Database.getInstance(this);

        setupPieChartLook();

        setHistoryChartNoDataText();
        setupHistorySpinner();
        setupActivitiesSpinner();

        new LoadFromDatabase(this).execute();
    }

    private void setupPieChartLook() {
        pieData = createActivityPieData(new ArrayList<>());

        pieChart.setRenderer(new ActivityPieChartRenderer(pieChart));

        // Hole
        pieChart.setHoleColor(Color.BLACK);
        pieChart.setTransparentCircleRadius(0f);

        // Offsets
        pieChart.setExtraTopOffset(35f);
        pieChart.setExtraBottomOffset(45f);

        // Text Formatting
        pieChart.setEntryLabelTextSize(14f);

        // No Data Text
        pieChart.setNoDataText(getString(R.string.activity_pie_chart_no_data));
        pieChart.setNoDataTextColor(Color.WHITE);

        // Miscellaneous
        pieChart.setRotationEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setMinAngleForSlices(1f);
        pieChart.setUsePercentValues(true);
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setData(pieData);
    }

    private PieData createActivityPieData(List<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, null);

        // Colors
        List<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.pie_chart_1));
        colors.add(getResources().getColor(R.color.pie_chart_2));
        colors.add(getResources().getColor(R.color.pie_chart_3));
        colors.add(getResources().getColor(R.color.pie_chart_4));
        colors.add(getResources().getColor(R.color.pie_chart_5));
        colors.add(getResources().getColor(R.color.pie_chart_6));

        dataSet.setColors(colors);
        dataSet.setValueTextColors(colors);
        dataSet.setValueLineColor(Color.WHITE);

        // Activity Name Position
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        // Line Lengths
        dataSet.setValueLineWidth(2f);
        dataSet.setValueLinePart1Length(0.8f);

        PieData pieData = new PieData(dataSet);

        // Text Formatting
        pieData.setValueTextSize(12f);
        pieData.setValueFormatter(new CustomPercentFormatter());

        return pieData;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivitySpinnerSelectionFromTouch = false;
    }

    /**
     * Put activities below {@link Constants#CLUMP_ACTIVITIES_BELOW_THIS_PERCENT} into one activity for chart
     * readability. Also, set percentages on {@link PieChartItem}. Activities are sorted from highest to lowest
     * percent with the Others activity put in the middle.
     *
     * @param activities to prepare
     * @return prepared activities
     */
    private List<PieChartItem> prepareActivities(List<PieChartItem> activities) {
        if (activities.size() == 0) {
            return activities;
        }

        double totalTime = 0;
        double othersTime = 0;
        List<PieChartItem> result = new ArrayList<>(activities.size());

        for (PieChartItem pieChartItem : activities) {
            totalTime += pieChartItem.getTotalTime();
        }

        for (PieChartItem pieChartItem : activities) {
            double percent = pieChartItem.getTotalTime() / totalTime * 100;
            pieChartItem.setPercent(percent);
        }

        Collections.sort(activities, Comparator.comparing(PieChartItem::getPercent).reversed());

        int fullActivitiesAdded = 0;
        for (PieChartItem pieChartItem : activities) {
            if (activities.size() <= Constants.MAX_ITEMS_TO_SHOW_ON_ACTIVITY_CHART) {
                if (pieChartItem.getPercent() > Constants.CLUMP_ACTIVITIES_BELOW_THIS_PERCENT && ++fullActivitiesAdded <= Constants.MAX_ITEMS_TO_SHOW_ON_ACTIVITY_CHART) {
                    result.add(pieChartItem);
                } else {
                    othersTime += pieChartItem.getTotalTime();
                }
            } else {
                if (pieChartItem.getPercent() > Constants.CLUMP_ACTIVITIES_BELOW_THIS_PERCENT && ++fullActivitiesAdded < Constants.MAX_ITEMS_TO_SHOW_ON_ACTIVITY_CHART) {
                    result.add(pieChartItem);
                } else {
                    othersTime += pieChartItem.getTotalTime();
                }
            }
        }

        if (othersTime > 0) {
            if (result.size() > 2) {
                result.add(result.size() / 2, new PieChartItem((int) othersTime, othersTime / totalTime * 100, ""));
            } else {
                result.add(new PieChartItem((int) othersTime, othersTime / totalTime * 100, ""));
            }
        }
        return result;
    }

    private void setHistoryChartNoDataText() {
        LineChart lineChart = findViewById(R.id.history_chart);
        Paint paint = lineChart.getPaint(Chart.PAINT_INFO);
        paint.setColor(Color.WHITE);
        lineChart.setNoDataText(getString(R.string.loading_chart));
        lineChart.invalidate();
    }

    private void setupHistorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.history_spinner, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        historySpinnerCurrentSelectedIndex = sharedPreferences.getInt(Constants.HISTORY_SPINNER_SETTING, 1);

        historySpinner.setAdapter(adapter);
        historySpinner.setSelection(historySpinnerCurrentSelectedIndex);
        historySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (historySpinnerCurrentSelectedIndex == position) {
                    return;
                }

                historySpinnerCurrentSelectedIndex = position;

                sharedPreferences.edit().putInt(Constants.HISTORY_SPINNER_SETTING, position).apply();

                if (position == 1) {
                    historyChart.displayData(monthData);
                } else {
                    historyChart.displayData(dayData);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupActivitiesSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.activities_spinner, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        activitiesSpinnerCurrentSelectedIndex = sharedPreferences.getInt(Constants.ACTIVITIES_SPINNER_SETTING, 1);

        activitiesSpinner.setAdapter(adapter);
        activitiesSpinner.setSelection(activitiesSpinnerCurrentSelectedIndex);
        activitiesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activitiesSpinnerCurrentSelectedIndex = position;

                sharedPreferences.edit().putInt(Constants.ACTIVITIES_SPINNER_SETTING, position).apply();

                Database.databaseExecutor.execute(() -> {
                    List<PieChartItem> pieChartItems;

                    switch (position) {
                        case 0:
                            pieChartItems = database.pomodoroDao().getPieChartItems(LocalDate.now().toString());
                            break;
                        case 1:
                            pieChartItems =
                                    database.pomodoroDao().getPieChartItems(LocalDate.now().minusDays(6).toString(), LocalDate.now().toString());
                            break;
                        case 2:
                            pieChartItems =
                                    database.pomodoroDao().getPieChartItems(LocalDate.now().minusMonths(1).toString(), LocalDate.now().toString());
                            break;

                        default:
                            pieChartItems = database.pomodoroDao().getAllPieChartItems();
                            break;
                    }

                    runOnUiThread(() -> refreshPieChart(pieChartItems));
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void refreshPieChart(List<PieChartItem> activities) {
        loadPieChart(activities);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(Constants.SCROLL_PIE_CHART_AUTOMATICALLY, true)) {
            if (isActivitySpinnerSelectionFromTouch) {
                TextView activityTextView = findViewById(R.id.activities_text_view);

                if (shouldScrollDown) {
                    ObjectAnimator scrollToBottom = ObjectAnimator.ofInt(scrollView, "scrollY", activityTextView.getTop());
                    scrollToBottom.setDuration(500);
                    scrollToBottom.start();
                }
            }
        }
        isActivitySpinnerSelectionFromTouch = true;
    }

    private void loadPieChart(List<PieChartItem> activities) {
        List<PieEntry> entries = new ArrayList<>();

        List<PieChartItem> preparedPieChartItems = prepareActivities(activities);

        for (PieChartItem pieChartItem : preparedPieChartItems) {
            entries.add(new PieEntry(pieChartItem.getTotalTime(), pieChartItem.getActivityName(),
                    Utility.formatPieChartTime(pieChartItem.getTotalTime())));
        }

        pieData = createActivityPieData(entries);

        // Center Text
        pieChart.setCenterText(Utility.formatPieChartTime((long) pieData.getYValueSum()));

        // Hole
        pieChart.setHoleRadius(50f);

        if (preparedPieChartItems.size() == 0) {
            pieChart.setData(null);
        } else {
            pieChart.setData(pieData);
        }

        pieChart.invalidate();

        int previousLegendSize;

        if (legendItems == null) {
            previousLegendSize = 0;
        } else {
            previousLegendSize = legendItems.size();
        }

        legendItems = new ArrayList<>();

        List<PieChartItem> others = new ArrayList<>(activities);
        others.removeAll(preparedPieChartItems);

        Collections.sort(others, Comparator.comparingDouble(PieChartItem::getPercent).reversed());

        for (PieChartItem pieChartItem : others) {
            legendItems.add(new LegendItem(pieChartItem.getActivityName(), pieChartItem.getPercent(),
                    Utility.formatPieChartTime(pieChartItem.getTotalTime())));
        }

        shouldScrollDown = previousLegendSize <= legendItems.size();

        if (legendItems.size() > 0) {
            for (PieChartItem pieChartItem : preparedPieChartItems) {
                if (pieChartItem.getActivityName().isEmpty()) {
                    othersTextView.setVisibility(View.VISIBLE);
                    othersTextView.setText(getResources().getString(R.string.others_text,
                            pieChartItem.getPercent()));

                    for (int i = 0; i < pieData.getDataSet().getEntryCount(); i++) {
                        PieEntry pieEntry = pieData.getDataSet().getEntryForIndex(i);

                        if (pieEntry.getLabel().isEmpty()) {
                            int[] colors = pieData.getColors();

                            othersTextView.setTextColor(colors[i]);

                            break;
                        }
                    }
                }
            }
        } else {
            othersTextView.setVisibility(View.GONE);
        }

        if (legendItems.size() == 0) {
            legendRecyclerView.setVisibility(View.GONE);
        } else {
            legendRecyclerView.setVisibility(View.VISIBLE);
            legendRecyclerView.setAdapter(new LegendAdapter(legendItems));
        }
    }

    private static class LoadFromDatabase extends AsyncTask<Void, Void, Void> {
        private final WeakReference<StatisticsActivity> activityWeakReference;
        HistoryChartItem historyChartItemToday;
        List<HistoryChartItem> historyChartItemsWeek;
        List<HistoryChartItem> historyChartItemsMonth;
        List<HistoryChartItem> historyChartItemsTotal;

        LoadFromDatabase(StatisticsActivity context) {
            this.activityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            StatisticsActivity statisticsActivity = activityWeakReference.get();

            if (statisticsActivity == null || statisticsActivity.isFinishing()) {
                return null;
            }

            historyChartItemToday = statisticsActivity.database.pomodoroDao().getAllGroupByDate(LocalDate.now().toString());

            historyChartItemsWeek =
                    statisticsActivity.database.pomodoroDao().getAllDatesBetweenGroupByDate(LocalDate.now().minusDays(Constants.MAX_ITEMS_TO_SHOW_ON_ACTIVITY_CHART).toString(),
                            LocalDate.now().minusDays(1).toString());

            historyChartItemsMonth =
                    statisticsActivity.database.pomodoroDao().getAllDatesBetweenGroupByDate(LocalDate.now().minusDays(29).toString(),
                            LocalDate.now().minusDays(7).toString());

            historyChartItemsTotal =
                    statisticsActivity.database.pomodoroDao().getAllDateLessGroupByDate(LocalDate.now().minusDays(29).toString());

            data = statisticsActivity.database.pomodoroDao().getAllGroupByDate();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StatisticsActivity statisticsActivity = activityWeakReference.get();

            if (statisticsActivity == null || statisticsActivity.isFinishing()) {
                return;
            }

            int timeToday = 0;

            if (historyChartItemToday != null) {
                timeToday =
                        historyChartItemToday.getCompletedWorkTime() + historyChartItemToday.getIncompleteWorkTime();
            }

            int timeWeek = timeToday;

            for (HistoryChartItem historyChartItem : historyChartItemsWeek) {
                timeWeek += historyChartItem.getCompletedWorkTime();
                timeWeek += historyChartItem.getIncompleteWorkTime();
            }

            int timeMonth = timeWeek;

            for (HistoryChartItem historyChartItem : historyChartItemsMonth) {
                timeMonth += historyChartItem.getCompletedWorkTime();
                timeMonth += historyChartItem.getIncompleteWorkTime();
            }

            int timeTotal = timeMonth;

            for (HistoryChartItem historyChartItem : historyChartItemsTotal) {
                timeTotal += historyChartItem.getCompletedWorkTime();
                timeTotal += historyChartItem.getIncompleteWorkTime();
            }

            statisticsActivity.numberTodayTextView.setText(Utility.formatPieChartTime(timeToday));
            statisticsActivity.numberWeekTextView.setText(Utility.formatPieChartTime(timeWeek));
            statisticsActivity.numberMonthTextView.setText(Utility.formatPieChartTime(timeMonth));
            statisticsActivity.numberTotalTextView.setText(Utility.formatPieChartTime(timeTotal));

            monthData = new MonthData(data);
            monthData.generate();

            dayData = new DayData(data);
            dayData.generate();

            historyChart = new HistoryChart(statisticsActivity);
            historyChart.setupChart();

            if (historySpinnerCurrentSelectedIndex == 1) {
                historyChart.displayData(monthData);
            } else {
                historyChart.displayData(dayData);
            }
        }
    }
}