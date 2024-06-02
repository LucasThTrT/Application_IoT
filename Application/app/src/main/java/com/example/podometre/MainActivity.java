package com.example.podometre;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private List<Float> verticalAcceleration = new ArrayList<>();
    private Context podoContext;
    private static final URL url;
    private ScheduledExecutorService scheduler;
    private TextView description;
    Boolean isSending = false;
    private Button bouton_envoie_serveur_continue;
    private Boolean isSendingContinue = false;
    private RequestQueue queue;
    private final static int NBmaxSample = 750; //Pris en 2 secondes à une fréquence de 50Hz
    private Integer sample = 0;
    private Integer nombrePas = 0;
    private TextView textPas;
    private static final int fSampling = 50; // 50 Hz
    private static final int TTOTALSampling = NBmaxSample/fSampling; // 2 secondes



    static {
        try {
            url = new URL("https://wd852cn056.execute-api.eu-north-1.amazonaws.com/dev/step");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public MainActivity() throws MalformedURLException {
    }

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView
                texteentree = findViewById(R.id.Texte_entree);

        texteentree.setTypeface(null, Typeface.BOLD);

        podoContext = this;

        this.queue = Volley.newRequestQueue(podoContext);

        description = findViewById(R.id.Description);
        //description.setTextColor(Color.BLUE);
        description.setTypeface(null, Typeface.BOLD);
        description.setText("");

        bouton_envoie_serveur_continue = findViewById(R.id.BoutonServeurContinu);
        bouton_envoie_serveur_continue.setText("Envoyer au serveur (CONTINU)");
        bouton_envoie_serveur_continue.setOnClickListener(v -> {
            isSendingContinue = !isSendingContinue;
            if (isSendingContinue) {
                bouton_envoie_serveur_continue.setText("Arrêter l'envoie (CONTINU)");
            } else {
                bouton_envoie_serveur_continue.setText("Envoyer au serveur");
                bouton_envoie_serveur_continue.setBackgroundColor(Color.BLUE);
            }
        });

        Button bouton_reset_pas = findViewById(R.id.boutonReset);
        bouton_reset_pas.setText("Reset Nombre de pas");
        bouton_reset_pas.setOnClickListener(v -> {
            nombrePas = 0;
            majViewPas();
        });


        this.textPas = findViewById(R.id.text_pas);
        textPas.setText("Pas : " + nombrePas.toString());
        textPas.setTextSize(70);
        textPas.setTypeface(null, Typeface.BOLD);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);

        scheduler = Executors.newScheduledThreadPool(1);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @SuppressLint("SetTextI18n")
    private void majViewPas() {
        textPas.setText("Pas : " + nombrePas.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @SuppressLint("SetTextI18n")
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2]; // Accélération verticale
            verticalAcceleration.add(z);
            int taille_samples = verticalAcceleration.size();
            String s_acceleration_instant = String.valueOf(z);
            description.setText("Accélération instantanée (axe z) : "
                    + s_acceleration_instant + " m/s²");
            if (isSendingContinue) {
                // On envoie la donnée au serveur
                sample++;
                if (sample == NBmaxSample) {
                    // Conversion de l'accélération en JSON
                    JSONObject json = new JSONObject();
                    try {
                        JSONArray jsonArrayEnvoie = new JSONArray();
                        for (Float zi : verticalAcceleration) {
                            jsonArrayEnvoie.put(zi);
                        }
                        json = new JSONObject().put("accelerations", jsonArrayEnvoie);

                        System.out.println(json.toString());
                        sendPostJSON(json);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    sample = 0;
                    verticalAcceleration.clear();
                }
            } else {
                description.setTextColor(Color.BLUE);
                sample = 0;
            }
        }
    }

    private void sendPostJSON(JSONObject json) {
        Log.d("sendPostJSON","Début SPJSON Envoie de la donnée : " + json);
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url.toString(), json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject s) {
                        Log.d("sendPostJSON","Envoie effectué");
                        Log.d("sendPostJSON","Réponse : " + s);
                        String body = null;
                        int pasMesure;
                        try {
                            body = s.getString("body");
                            JSONObject jsonBody = new JSONObject(body);
                            pasMesure = jsonBody.getInt("steps");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        nombrePas += pasMesure;
                        majViewPas();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("sendPostJSON","ERROR SEND POST" + volleyError.getMessage());
            }
        }) {
            @Override
            public byte[] getBody() {
                Log.d("sendPostJSON","get Body : " + new String(super.getBody()));
                return super.getBody();
            }
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            return headers;
        }
        };
        this.queue.add(stringRequest);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // On laise cette méthode vide car on a pas besoin de gérer
        // les changements de précision du capteur
    }
}