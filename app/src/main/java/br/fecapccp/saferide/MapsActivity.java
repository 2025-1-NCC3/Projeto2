package br.fecapccp.saferide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
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

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap googleMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean locationPermissionGranted = false;

    // Dados da viagem
    private double origemLat, origemLng;
    private double destinoLat, destinoLng;
    private String origemEndereco, destinoEndereco;
    private boolean temRotaParaExibir = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maps);

        // Inicializar o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obter o fragment do mapa e registrar o callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Verificar permissões de localização
        checkLocationPermission();

        // Obter dados da intent
        if (getIntent().hasExtra("origem_lat") && getIntent().hasExtra("destino_lat")) {
            origemLat = getIntent().getDoubleExtra("origem_lat", 0);
            origemLng = getIntent().getDoubleExtra("origem_lng", 0);
            origemEndereco = getIntent().getStringExtra("origem_endereco");

            destinoLat = getIntent().getDoubleExtra("destino_lat", 0);
            destinoLng = getIntent().getDoubleExtra("destino_lng", 0);
            destinoEndereco = getIntent().getStringExtra("destino_endereco");

            // Verificar se temos coordenadas válidas
            if (origemLat != 0 && origemLng != 0 && destinoLat != 0 && destinoLng != 0) {
                temRotaParaExibir = true;
                Log.d(TAG, "Dados da rota recebidos com sucesso.");
                Log.d(TAG, "Origem: " + origemLat + "," + origemLng + " - " + origemEndereco);
                Log.d(TAG, "Destino: " + destinoLat + "," + destinoLng + " - " + destinoEndereco);
            } else {
                Log.e(TAG, "Coordenadas inválidas recebidas.");
            }
        } else {
            Log.e(TAG, "Extras não encontrados na Intent.");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                updateLocationUI();

                // Se temos uma rota, mostrar a rota
                // Caso contrário, mostrar localização atual
                if (temRotaParaExibir) {
                    exibirRota();
                } else {
                    getDeviceLocation();
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d(TAG, "Mapa carregado com sucesso.");

        // Atualizar UI baseado nas permissões
        updateLocationUI();

        // Verificar se temos dados de rota para exibir
        if (temRotaParaExibir) {
            exibirRota();
        } else if (locationPermissionGranted) {
            // Se não temos rota, exibir localização atual
            getDeviceLocation();
        }
    }

    private void exibirRota() {
        if (googleMap == null) {
            Log.e(TAG, "GoogleMap ainda não está inicializado.");
            return;
        }

        // Criar os pontos de origem e destino
        LatLng origem = new LatLng(origemLat, origemLng);
        LatLng destino = new LatLng(destinoLat, destinoLng);

        Log.d(TAG, "Exibindo rota - Origem: " + origemLat + "," + origemLng);
        Log.d(TAG, "Exibindo rota - Destino: " + destinoLat + "," + destinoLng);

        // Adicionar marcadores no mapa
        googleMap.addMarker(new MarkerOptions()
                .position(origem)
                .title("Origem: " + origemEndereco));

        googleMap.addMarker(new MarkerOptions()
                .position(destino)
                .title("Destino: " + destinoEndereco));

        // Ajustar a câmera para mostrar ambos os pontos
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origem);
        builder.include(destino);
        LatLngBounds bounds = builder.build();

        // Adicionar um padding para que os marcadores não fiquem nas bordas
        int padding = 100; // offset da borda em pixels
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

        // Desenhar linha reta como fallback inicial
        desenharLinhaReta(origem, destino);

        // Buscar e traçar a rota entre os pontos
        String url = getDirectionsUrl(origem, destino);
        Log.d(TAG, "URL da API Directions: " + url);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    private void desenharLinhaReta(LatLng origem, LatLng destino) {
        PolylineOptions lineOptions = new PolylineOptions()
                .add(origem)
                .add(destino)
                .width(12)
                .color(Color.RED)
                .geodesic(true);

        googleMap.addPolyline(lineOptions);

        Log.d(TAG, "Linha reta desenhada como alternativa até a rota ser carregada");
    }

    private void updateLocationUI() {
        if (googleMap == null) {
            return;
        }

        try {
            if (locationPermissionGranted) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                Log.d(TAG, "Localização do usuário habilitada no mapa.");
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                Log.d(TAG, "Permissão de localização não concedida.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erro ao configurar UI de localização: ", e);
        }
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                // Mover a câmera para a localização atual do usuário
                                LatLng currentLocation = new LatLng(location.getLatitude(),
                                        location.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        currentLocation, 15)); // Zoom de 15 (maior = mais próximo)
                                Log.d(TAG, "Localização atual obtida: " + location.getLatitude() + "," + location.getLongitude());
                            } else {
                                Log.e(TAG, "Não foi possível obter a localização atual");
                                Toast.makeText(MapsActivity.this,
                                        "Não foi possível obter a localização atual",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erro ao obter localização do dispositivo: ", e);
        }
    }

    // Métodos para obter e desenhar a rota
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origem
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destino
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Modo de transporte
        String mode = "mode=driving";
        // Chave da API
        String key = "key=" + getString(R.string.google_maps_api_key);
        // Construir os parâmetros da URL
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + key;
        // Formato de saída
        String output = "json";
        // URL da API Directions
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    // Classe AsyncTask para download dos dados da rota
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                Log.d(TAG, "Iniciando download da rota: " + url[0]);
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer download da rota", e);
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result.isEmpty()) {
                Log.e(TAG, "Resposta vazia da API Directions");
                Toast.makeText(MapsActivity.this, "Não foi possível obter dados da rota", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Download concluído, iniciando parsing...");
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        HttpURLConnection urlConnection = null;
        InputStream iStream = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, "Erro na resposta HTTP: " + responseCode);
                return "";
            }

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();

            Log.d(TAG, "Resposta da API (primeiros 500 caracteres): " +
                    (data.length() > 500 ? data.substring(0, 500) + "..." : data));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao fazer download da URL", e);
        } finally {
            if (iStream != null) iStream.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return data;
    }

    // Classe para parsear o JSON
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);

                // Verificar se há rotas na resposta
                if (!jObject.has("routes") || jObject.getJSONArray("routes").length() == 0) {
                    Log.e(TAG, "Nenhuma rota encontrada na resposta JSON: " +
                            (jsonData[0].length() > 300 ? jsonData[0].substring(0, 300) + "..." : jsonData[0]));
                    return null;
                }

                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
                Log.d(TAG, "Rota parseada com sucesso, número de rotas: " +
                        (routes != null ? routes.size() : "null"));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao parsear JSON: " + e.getMessage(), e);
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            if (result == null || result.isEmpty()) {
                Log.e(TAG, "Não foi possível encontrar a rota ou resultado vazio");
                Toast.makeText(MapsActivity.this, "Não foi possível encontrar a rota", Toast.LENGTH_SHORT).show();
                return;
            }

            // Percorrer todas as rotas
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);
                Log.d(TAG, "Processando rota " + (i+1) + " com " + path.size() + " pontos");

                // Percorrer todos os pontos da rota
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    try {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Erro ao converter coordenadas: " + e.getMessage());
                    }
                }

                // Adicionar todos os pontos à linha
                if (!points.isEmpty()) {
                    lineOptions.addAll(points);
                    lineOptions.width(12); // Largura da linha
                    lineOptions.color(Color.BLUE); // Cor da linha
                    lineOptions.geodesic(true);
                    Log.d(TAG, "PolylineOptions criada com " + points.size() + " pontos");
                }
            }

            // Desenhar a linha no mapa se houver pontos
            if (lineOptions != null && !points.isEmpty()) {
                googleMap.addPolyline(lineOptions);
                Log.d(TAG, "Rota desenhada com sucesso");

                // Remover a linha reta após a rota estar disponível
                // Isto requer redesenhar o mapa ou manter uma referência para a linha reta
                // Para simplificar, estamos apenas adicionando a nova rota por cima
            } else {
                Log.e(TAG, "Não foi possível criar as opções de linha para a rota");
                Toast.makeText(MapsActivity.this, "Não foi possível traçar a rota", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Classe para analisar o JSON da API Directions
     */
    public class DirectionsJSONParser {

        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;

            try {
                Log.d(TAG, "Iniciando parsing do JSON da rota");

                if (!jObject.has("routes")) {
                    Log.e(TAG, "JSON não contém rotas");
                    return routes;
                }

                jRoutes = jObject.getJSONArray("routes");
                Log.d(TAG, "Encontradas " + jRoutes.length() + " rotas no JSON");

                // Percorrer todas as rotas
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<>();

                    Log.d(TAG, "Na rota " + (i+1) + ", encontradas " + jLegs.length() + " legs");

                    // Percorrer todas as pernas (legs) da rota
                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        Log.d(TAG, "Na leg " + (j+1) + ", encontrados " + jSteps.length() + " passos");

                        // Percorrer todos os passos (steps) da perna
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            Log.d(TAG, "No passo " + (k+1) + ", decodificados " + list.size() + " pontos da polyline");

                            // Percorrer todos os pontos da linha poligonal (polyline)
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString(list.get(l).latitude));
                                hm.put("lng", Double.toString(list.get(l).longitude));
                                path.add(hm);
                            }
                        }
                    }
                    routes.add(path);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Erro ao processar JSON: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e(TAG, "Erro geral ao fazer parse: " + e.getMessage(), e);
            }

            return routes;
        }

        /**
         * Método para decodificar a string polyline de formato codificado do Google para uma lista de LatLng
         */
        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            try {
                while (index < len) {
                    int b, shift = 0, result = 0;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += dlat;

                    shift = 0;
                    result = 0;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lng += dlng;

                    LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                    poly.add(p);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao decodificar polyline: " + e.getMessage(), e);
            }

            return poly;
        }
    }
}