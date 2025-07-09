package com.example.tempsen;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class TemperatureActivity extends AppCompatActivity {

    private static final String TAG = "TemperatureActivity";

    // TextViews pour les cartes de température
    private TextView yesterdayDate, yesterdayMorning, yesterdayAfternoon, yesterdayEvening, yesterdayWeather;
    private TextView todayDate, todayMorning, todayAfternoon, todayEvening, todayWeather;
    private TextView tomorrowDate, tomorrowMorning, tomorrowAfternoon, tomorrowEvening, tomorrowWeather;

    private TextView statusText;
    private Button retryButton;
    private ProgressBar loadingProgress;
    private ScrollView contentLayout;

    private RequestQueue queue;
    private String region;
    private int completedRequests = 0;
    private int totalRequests = 3;
    private int successfulRequests = 0;

    private String yesterday, today, tomorrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        // Configuration de la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_temp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Récupération de la région
        region = getIntent().getStringExtra("region");
        if (region == null) {
            region = "Dakar";
        }
        getSupportActionBar().setTitle("Météo - " + region);

        // Initialisation des vues
        initializeViews();

        // Initialisation de Volley
        queue = Volley.newRequestQueue(this);

        // Préparation des dates
        prepareDates();

        // Action du bouton retry
        retryButton.setOnClickListener(v -> {
            showLoadingState();
            resetAndReload();
        });

        // Chargement initial
        showLoadingState();
        startLoading();
    }

    private void initializeViews() {
        // Vues des cartes hier
        yesterdayDate = findViewById(R.id.yesterdayDate);
        yesterdayMorning = findViewById(R.id.yesterdayMorning);
        yesterdayAfternoon = findViewById(R.id.yesterdayAfternoon);
        yesterdayEvening = findViewById(R.id.yesterdayEvening);
        yesterdayWeather = findViewById(R.id.yesterdayWeather);

        // Vues des cartes aujourd'hui
        todayDate = findViewById(R.id.todayDate);
        todayMorning = findViewById(R.id.todayMorning);
        todayAfternoon = findViewById(R.id.todayAfternoon);
        todayEvening = findViewById(R.id.todayEvening);
        todayWeather = findViewById(R.id.todayWeather);

        // Vues des cartes demain
        tomorrowDate = findViewById(R.id.tomorrowDate);
        tomorrowMorning = findViewById(R.id.tomorrowMorning);
        tomorrowAfternoon = findViewById(R.id.tomorrowAfternoon);
        tomorrowEvening = findViewById(R.id.tomorrowEvening);
        tomorrowWeather = findViewById(R.id.tomorrowWeather);

        // Vues de contrôle
        statusText = findViewById(R.id.statusText);
        retryButton = findViewById(R.id.retryButton);
        loadingProgress = findViewById(R.id.loadingProgress);
        contentLayout = findViewById(R.id.contentLayout);

        // État initial
        setInitialLoadingText();
    }

    private void setInitialLoadingText() {
        String loading = "Chargement...";

        // Hier
        yesterdayMorning.setText(loading);
        yesterdayAfternoon.setText(loading);
        yesterdayEvening.setText(loading);
        yesterdayWeather.setText(loading);

        // Aujourd'hui
        todayMorning.setText(loading);
        todayAfternoon.setText(loading);
        todayEvening.setText(loading);
        todayWeather.setText(loading);

        // Demain
        tomorrowMorning.setText(loading);
        tomorrowAfternoon.setText(loading);
        tomorrowEvening.setText(loading);
        tomorrowWeather.setText(loading);
    }

    private void prepareDates() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE dd MMMM", Locale.FRENCH);

        // Aujourd'hui
        today = sdf.format(cal.getTime());
        todayDate.setText(displayFormat.format(cal.getTime()));

        // Hier
        cal.add(Calendar.DAY_OF_YEAR, -1);
        yesterday = sdf.format(cal.getTime());
        yesterdayDate.setText(displayFormat.format(cal.getTime()));

        // Demain
        cal.add(Calendar.DAY_OF_YEAR, 2);
        tomorrow = sdf.format(cal.getTime());
        tomorrowDate.setText(displayFormat.format(cal.getTime()));
    }

    private void showLoadingState() {
        contentLayout.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        updateStatus("Chargement des températures...");
    }

    private void showContentState() {
        contentLayout.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void showErrorState() {
        contentLayout.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void startLoading() {
        if (!isInternetAvailable()) {
            updateStatus("Pas de connexion internet");
            showErrorState();
            return;
        }

        // Chargement des trois jours
        fetchDetailedTemperature(yesterday,
                yesterdayMorning, yesterdayAfternoon, yesterdayEvening, yesterdayWeather);
        fetchDetailedTemperature(today,
                todayMorning, todayAfternoon, todayEvening, todayWeather);
        fetchDetailedTemperature(tomorrow,
                tomorrowMorning, tomorrowAfternoon, tomorrowEvening, tomorrowWeather);
    }

    private void fetchDetailedTemperature(String date, TextView morningView,
                                          TextView afternoonView, TextView eveningView, TextView weatherView) {
        // Coordonnées par défaut (Dakar)
        double latitude = 14.7167;
        double longitude = -17.4677;

        // Ajustement selon la région
        switch (region.toLowerCase()) {
            case "thiès":
                latitude = 14.7889;
                longitude = -16.9244;
                break;
            case "saint-louis":
                latitude = 16.0200;
                longitude = -16.4889;
                break;
            case "kaolack":
                latitude = 14.1547;
                longitude = -16.0719;
                break;
            case "ziguinchor":
                latitude = 12.5681;
                longitude = -16.2717;
                break;
            case "tambacounda":
                latitude = 13.7671;
                longitude = -13.6681;
                break;
            case "diourbel":
                latitude = 14.6542;
                longitude = -16.2288;
                break;
            case "fatick":
                latitude = 14.3347;
                longitude = -16.4022;
                break;
            case "kaffrine":
                latitude = 14.1058;
                longitude = -15.5481;
                break;
            case "kolda":
                latitude = 12.8939;
                longitude = -14.9406;
                break;
            case "louga":
                latitude = 15.6181;
                longitude = -16.2267;
                break;
            case "matam":
                latitude = 15.6559;
                longitude = -13.2553;
                break;
            case "sédhiou":
                latitude = 12.7086;
                longitude = -15.5569;
                break;
            case "kédougou":
                latitude = 12.5569;
                longitude = -12.1756;
                break;
        }

        // url api open-meteo que j'ai utilisé car l'endpoint pour les températures de "openumeric" marchait pas pour mon cas
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,weathercode&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto&start_date=%s&end_date=%s",
                latitude, longitude, date, date);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    parseDetailedResponse(response, morningView, afternoonView, eveningView, weatherView);
                },
                error -> {
                    Log.e(TAG, "Erreur API", error);
                    morningView.setText("Erreur");
                    afternoonView.setText("Erreur");
                    eveningView.setText("Erreur");
                    weatherView.setText("Erreur de chargement");
                    onRequestCompleted(false);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void parseDetailedResponse(JSONObject response, TextView morningView,
                                       TextView afternoonView, TextView eveningView, TextView weatherView) {
        try {
            // Données horaires
            JSONObject hourly = response.getJSONObject("hourly");
            JSONArray hourlyTemps = hourly.getJSONArray("temperature_2m");
            JSONArray hourlyWeatherCodes = hourly.getJSONArray("weathercode");

            // Données journalières
            JSONObject daily = response.getJSONObject("daily");
            JSONArray dailyWeatherCodes = daily.getJSONArray("weathercode");
            JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
            JSONArray minTemps = daily.getJSONArray("temperature_2m_min");

            if (hourlyTemps.length() >= 24) {
                // Températures approximatives pour matin (8h), après-midi (14h), soir (20h)
                double morningTemp = hourlyTemps.getDouble(8);
                double afternoonTemp = hourlyTemps.getDouble(14);
                double eveningTemp = hourlyTemps.getDouble(20);

                // Codes météo correspondants
                int morningWeather = hourlyWeatherCodes.getInt(8);
                int afternoonWeather = hourlyWeatherCodes.getInt(14);
                int eveningWeather = hourlyWeatherCodes.getInt(20);

                // Affichage des températures avec emojis
                morningView.setText(String.format("🌅 %.0f°C\n%s", morningTemp, getWeatherEmoji(morningWeather)));
                afternoonView.setText(String.format("☀️ %.0f°C\n%s", afternoonTemp, getWeatherEmoji(afternoonWeather)));
                eveningView.setText(String.format("🌙 %.0f°C\n%s", eveningTemp, getWeatherEmoji(eveningWeather)));

                // Description générale du jour
                if (dailyWeatherCodes.length() > 0) {
                    int dailyCode = dailyWeatherCodes.getInt(0);
                    double maxTemp = maxTemps.getDouble(0);
                    double minTemp = minTemps.getDouble(0);

                    String description = String.format("%s %s\nMin: %.0f°C | Max: %.0f°C",
                            getWeatherEmoji(dailyCode), getWeatherDescription(dailyCode), minTemp, maxTemp);
                    weatherView.setText(description);
                }

                onRequestCompleted(true);
            } else {
                morningView.setText("Pas de données");
                afternoonView.setText("Pas de données");
                eveningView.setText("Pas de données");
                weatherView.setText("Pas de données");
                onRequestCompleted(false);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Erreur parsing", e);
            morningView.setText("Erreur");
            afternoonView.setText("Erreur");
            eveningView.setText("Erreur");
            weatherView.setText("Erreur parsing");
            onRequestCompleted(false);
        }
    }

    private String getWeatherEmoji(int code) {
        switch (code) {
            case 0: return "☀️";
            case 1: return "🌤️";
            case 2: return "⛅";
            case 3: return "☁️";
            case 45: case 48: return "🌫️";
            case 51: case 53: case 55: return "🌦️";
            case 61: case 63: case 65: return "🌧️";
            case 71: case 73: case 75: return "🌨️";
            case 80: case 81: case 82: return "🌦️";
            case 95: case 96: case 99: return "⛈️";
            default: return "🌤️";
        }
    }

    private String getWeatherDescription(int code) {
        switch (code) {
            case 0: return "Ensoleillé";
            case 1: return "Dégagé";
            case 2: return "Nuageux";
            case 3: return "Couvert";
            case 45: case 48: return "Brouillard";
            case 51: case 53: case 55: return "Bruine";
            case 61: case 63: case 65: return "Pluie";
            case 71: case 73: case 75: return "Neige";
            case 80: case 81: case 82: return "Averses";
            case 95: case 96: case 99: return "Orage";
            default: return "Variable";
        }
    }

    private void onRequestCompleted(boolean success) {
        completedRequests++;
        if (success) {
            successfulRequests++;
        }

        if (completedRequests >= totalRequests) {
            if (successfulRequests > 0) {
                showContentState();
            } else {
                updateStatus("Toutes les requêtes ont échoué");
                showErrorState();
            }
        }
    }

    private void resetAndReload() {
        completedRequests = 0;
        successfulRequests = 0;
        setInitialLoadingText();
        startLoading();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void updateStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queue != null) {
            queue.cancelAll(TAG);
        }
    }
}