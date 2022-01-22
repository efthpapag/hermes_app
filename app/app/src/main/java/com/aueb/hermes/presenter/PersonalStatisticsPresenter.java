package com.aueb.hermes.presenter;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;

import com.aueb.hermes.view.PersonalStatisticsFragment;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PersonalStatisticsPresenter {

    private final Fragment parent;
    private final String BACKEND_IP_ADDRESS;
    private final int TIME_SLOT_SIZE;
    private final String BACKEND_GET_DATA_URL;
    private final String UUID;
    private final DateTimeFormatter formatter;

    public PersonalStatisticsPresenter(Fragment parent, SharedPreferences sharedPreferences){
        this.parent =parent;
        this.BACKEND_IP_ADDRESS = sharedPreferences.getString("BACKEND_IP_ADDRESS", "192.168.68.110:8080");
        this.TIME_SLOT_SIZE = sharedPreferences.getInt("TIME_SLOT_SIZE", 4);
        this.BACKEND_GET_DATA_URL = "http://" + BACKEND_IP_ADDRESS + "/";
        this.UUID = sharedPreferences.getString("PREF_UNIQUE_ID", null);
        this.formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH");
    }

    public void getStatistics(String URL, String action){

        final JSONObject[] data = new JSONObject[1];

        Thread thread = new Thread(() -> {
            String apiURL = BACKEND_GET_DATA_URL + "api/" + URL;

            try {
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/json");

                //read received data
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                data[0] = new JSONObject(response.toString());

                if (data[0].length() > 0){
                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                    Iterator<String> iter = data[0].keys();
                    String key;
                    Map<Date, Long> points = new HashMap<>();
                    while (iter.hasNext()){
                        key = iter.next();
                        LocalDateTime localTime = LocalDateTime.parse(key, formatter);
                        Instant instant = localTime.toInstant(ZoneOffset.UTC);
                        try {
                            points.put(Date.from(instant), data[0].getLong(key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // Sort data points according to date
                    Date[] dates = new Date[points.size()];
                    int k = 0;
                    for (Date date : points.keySet()) {
                        dates[k++] = date;
                    }
                    Date temp;
                    for (int i = 0; i < dates.length; i++) {
                        for (int j = 0; j < dates.length - i - 1; j++) {
                            if (dates[j].compareTo(dates[j + 1]) > 0) {
                                temp = dates[j];
                                dates[j] = dates[j + 1];
                                dates[j + 1] = temp;
                            }
                        }
                    }

                    for (Date date : dates) {
                        series.appendData(new DataPoint(date, points.get(date)), true, dates.length, false);
                    }
                    ((PersonalStatisticsFragment) parent).setPersonalNetworkSeries(series);
                }

                //notify the view that the query was completed
                Intent intent = new Intent();
                intent.setAction(action);
                parent.getActivity().sendBroadcast(intent);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        });
        thread.start();

    }

}
