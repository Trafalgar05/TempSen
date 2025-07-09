package com.example.tempsen;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<String> regions = new ArrayList<>();
    private RegionAdapter adapter;
    private RequestQueue queue;

    // États de l'interface
    private LinearLayout loadingState;
    private LinearLayout errorState;
    private TextView statusText;
    private TextView errorText;
    private Button retryButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation de la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("\uD83C\uDF0D\n Régions du Sénégal");

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Initialisation de Volley
        queue = Volley.newRequestQueue(this);

        // Configuration du bouton Réessayer
        retryButton.setOnClickListener(v -> {
            showLoadingState();
            loadRegions();
        });

        // Chargement initial des régions
        showLoadingState();
        loadRegions();
    }

    private void initializeViews() {
        // États
        loadingState = findViewById(R.id.loadingState);
        errorState = findViewById(R.id.errorState);

        // Éléments de l'état de chargement
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);

        // Éléments de l'état d'erreur
        errorText = findViewById(R.id.errorText);
        retryButton = findViewById(R.id.retryButton);

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerRegions);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegionAdapter(regions, region -> {
            Intent intent = new Intent(this, TemperatureActivity.class);
            intent.putExtra("region", region);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadRegions() {
        String url = "https://ucao.openumeric.com/api/regions";
        statusText.setText("Chargement des régions...");

        if (!isNetworkAvailable()) {
            showError("Pas de connexion Internet");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray regionsArray = response.getJSONArray("regions");
                            regions.clear();

                            for (int i = 0; i < regionsArray.length(); i++) {
                                regions.add(regionsArray.getString(i));
                            }

                            adapter.notifyDataSetChanged();
                            showSuccess();
                        } else {
                            showError("Erreur: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        showError("Erreur de traitement des données");
                        Log.e("MainActivity", "JSON error", e);
                    }
                },
                error -> {
                    showError("Erreur de connexion");
                    Log.e("MainActivity", "API error", error);
                });

        queue.add(request);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showLoadingState() {
        loadingState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        statusText.setText("Chargement des régions...");
    }

    private void showError(String message) {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorText.setText(message);
    }

    private void showSuccess() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);


        Log.d("MainActivity", regions.size() + " régions chargées");
    }
}