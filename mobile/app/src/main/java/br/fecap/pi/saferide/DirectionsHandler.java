package br.fecap.pi.saferide;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.fecap.pi.saferide.R;

public class DirectionsHandler {
    private static final String TAG = "DirectionsHandler";
    private final GoogleMap googleMap;
    private final MapsActivity context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DirectionsHandler(MapsActivity context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
    }

    public void requestDirections(LatLng origin, LatLng destination) {
        String url = getDirectionsUrl(origin, destination);
        Log.d(TAG, "Solicitando direções: " + url);

        // Desenhar linha reta como fallback imediato
        desenharLinhaReta(origin, destination);

        executorService.execute(() -> {
            try {
                String jsonData = downloadUrl(url);

                if (jsonData.isEmpty()) {
                    showError("Não foi possível obter dados da rota");
                    return;
                }

                // Processar os dados JSON
                JSONObject jsonObject = new JSONObject(jsonData);

                // Verificar status da resposta
                String status = jsonObject.getString("status");
                if (!"OK".equals(status)) {
                    showError("Erro na API: " + status);
                    return;
                }

                DirectionsJSONParser parser = new DirectionsJSONParser();
                List<List<HashMap<String, String>>> routes = parser.parse(jsonObject);

                if (routes == null || routes.isEmpty()) {
                    showError("Nenhuma rota encontrada");
                    return;
                }

                // Processar na thread principal
                mainHandler.post(() -> drawRouteOnMap(routes));

            } catch (Exception e) {
                Log.e(TAG, "Erro ao processar rota: " + e.getMessage(), e);
                showError("Erro ao processar rota");
            }
        });
    }

    private void showError(String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private String downloadUrl(String strUrl) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000); // 15 segundos timeout
            urlConnection.setReadTimeout(15000);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, "Resposta HTTP mal-sucedida: " + responseCode);
                return "";
            }

            inputStream = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String responseData = stringBuilder.toString();
            Log.d(TAG, "Resposta recebida com " + responseData.length() + " caracteres");
            return responseData;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar InputStream", e);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void drawRouteOnMap(List<List<HashMap<String, String>>> routes) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions;

        try {
            // Percorrer todas as rotas
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = routes.get(i);

                // Percorrer todos os pontos
                for (HashMap<String, String> point : path) {
                    try {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Erro ao converter coordenadas", e);
                    }
                }

                lineOptions.addAll(points);
                lineOptions.width(12); // Largura
                lineOptions.color(Color.BLUE); // Cor
                lineOptions.geodesic(true);

                // Adicionar a polilinha ao mapa
                if (!points.isEmpty()) {
                    googleMap.addPolyline(lineOptions);
                    Log.d(TAG, "Rota " + (i+1) + " desenhada com " + points.size() + " pontos");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao desenhar rota no mapa", e);
            showError("Erro ao desenhar rota no mapa");
        }
    }

    private void desenharLinhaReta(LatLng origem, LatLng destino) {
        PolylineOptions lineOptions = new PolylineOptions()
                .add(origem)
                .add(destino)
                .width(8)
                .color(Color.RED)
                .geodesic(true);

        googleMap.addPolyline(lineOptions);
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origem
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destino
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Modo
        String mode = "mode=driving";
        // Chave API
        String key = "key=" + context.getString(R.string.google_maps_key);
        // Parâmetros
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + key;
        // Formato de saída
        String output = "json";
        // URL
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }
}