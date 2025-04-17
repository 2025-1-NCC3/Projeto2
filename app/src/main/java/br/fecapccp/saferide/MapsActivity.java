package br.fecapccp.saferide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private LatLng origemLatLng, destinoLatLng;
    private String origemEndereco, destinoEndereco;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtém as coordenadas da intent
        Intent intent = getIntent();
        if (intent != null) {
            origemLatLng = new LatLng(
                    intent.getDoubleExtra("ORIGEM_LAT", 0),
                    intent.getDoubleExtra("ORIGEM_LNG", 0)
            );
            destinoLatLng = new LatLng(
                    intent.getDoubleExtra("DESTINO_LAT", 0),
                    intent.getDoubleExtra("DESTINO_LNG", 0)
            );
            origemEndereco = intent.getStringExtra("ORIGEM_ENDERECO");
            destinoEndereco = intent.getStringExtra("DESTINO_ENDERECO");
        }

        // Verifica se as coordenadas são válidas
        if (origemLatLng == null || destinoLatLng == null ||
                (origemLatLng.latitude == 0 && origemLatLng.longitude == 0) ||
                (destinoLatLng.latitude == 0 && destinoLatLng.longitude == 0)) {
            Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Log das coordenadas para verificação
        Log.d(TAG, "Origem: " + origemLatLng.latitude + ", " + origemLatLng.longitude);
        Log.d(TAG, "Destino: " + destinoLatLng.latitude + ", " + destinoLatLng.longitude);

        // Obtém o fragmento do mapa e notifica quando estiver pronto
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o mapa", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Adiciona um botão de confirmação
        addConfirmButton();
    }

    private void addConfirmButton() {
        View rootView = findViewById(R.id.main);
        if (rootView instanceof RelativeLayout) {
            Button confirmButton = new Button(this);
            confirmButton.setText("Confirmar Viagem");
            confirmButton.setBackgroundColor(Color.BLACK);
            confirmButton.setTextColor(Color.WHITE);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.setMargins(50, 50, 50, 50);

            confirmButton.setLayoutParams(params);
            ((RelativeLayout) rootView).addView(confirmButton);

            confirmButton.setOnClickListener(v -> {
                Toast.makeText(this, "Viagem confirmada!", Toast.LENGTH_LONG).show();
                // Aqui você adicionaria a lógica para confirmar a viagem
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configura o mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Verifica e solicita permissões de localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Adiciona marcadores para origem e destino
        mMap.addMarker(new MarkerOptions()
                .position(origemLatLng)
                .title("Origem: " + origemEndereco)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(destinoLatLng)
                .title("Destino: " + destinoEndereco)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Ajusta a câmera para mostrar os dois pontos
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origemLatLng);
        builder.include(destinoLatLng);
        final LatLngBounds bounds = builder.build();

        try {
            // Aplica um padding à borda
            int padding = 100; // Em pixels
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover câmera: " + e.getMessage(), e);
            // Fallback para caso os bounds sejam inválidos
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 12));
        }

        // Inicia o processo de obtenção da rota em uma thread separada
        fetchDirections();
    }

    private void fetchDirections() {
        new Thread(() -> {
            try {
                String url = getDirectionsUrl(origemLatLng, destinoLatLng);
                Log.d(TAG, "URL da API Directions: " + url);

                String data = downloadUrl(url);
                Log.d(TAG, "Dados recebidos da API");

                if (data == null || data.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(MapsActivity.this,
                            "Não foi possível obter dados da rota", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> parseDirectionsData(data));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao obter direções: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MapsActivity.this,
                        "Erro ao obter rota: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origem
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destino
        String strDest = "destination=" + dest.latitude + "," + dest.longitude;
        // Modo (direciona para carro por padrão)
        String mode = "mode=driving";
        // Chave API
        String key = "key=" + getString(R.string.google_maps_key);
        // Parâmetros
        String parameters = strOrigin + "&" + strDest + "&" + mode + "&" + key;
        // Formato de saída
        String output = "json";
        // URL completa
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        HttpURLConnection urlConnection = null;
        InputStream iStream = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Código de resposta HTTP: " + responseCode);

            if (responseCode != 200) {
                Log.e(TAG, "Erro HTTP: " + responseCode);
                return null;
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
        } catch (Exception e) {
            Log.e(TAG, "Exception downloading URL: " + e.getMessage(), e);
            throw e;  // Propaga a exceção para ser tratada no método chamador
        } finally {
            if (iStream != null) iStream.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return data;
    }

    private void parseDirectionsData(String data) {
        try {
            if (data == null || data.isEmpty()) {
                Log.e(TAG, "Dados de direções vazios");
                Toast.makeText(this, "Não foi possível obter dados da rota", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject jsonObject = new JSONObject(data);

            // Verifica se há erro na resposta
            if (jsonObject.has("status") && !jsonObject.getString("status").equals("OK")) {
                String status = jsonObject.getString("status");
                String errorMsg = jsonObject.has("error_message") ?
                        jsonObject.getString("error_message") : "Status: " + status;
                Log.e(TAG, "Erro na API: " + errorMsg);
                Toast.makeText(this, "Erro na API: " + status, Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray routes = jsonObject.getJSONArray("routes");
            Log.d(TAG, "Número de rotas encontradas: " + routes.length());

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);

                // Verifica se overview_polyline existe
                if (!route.has("overview_polyline")) {
                    Log.e(TAG, "overview_polyline não encontrado na resposta");
                    Toast.makeText(this, "Formato de resposta inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");

                if (!overviewPolyline.has("points")) {
                    Log.e(TAG, "points não encontrado em overview_polyline");
                    Toast.makeText(this, "Formato de resposta inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                String encodedPath = overviewPolyline.getString("points");
                Log.d(TAG, "Caminho codificado obtido");

                List<LatLng> decodedPath = decodePoly(encodedPath);
                Log.d(TAG, "Número de pontos decodificados: " + decodedPath.size());

                if (decodedPath.isEmpty()) {
                    Log.e(TAG, "Caminho decodificado vazio");
                    Toast.makeText(this, "Erro ao decodificar rota", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Desenha a rota no mapa
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(decodedPath)
                        .width(12)  // Aumentei a largura para melhor visibilidade
                        .color(Color.BLUE)
                        .geodesic(true);  // Garante que a linha siga a curvatura da Terra

                mMap.addPolyline(polylineOptions);
                Log.d(TAG, "Polyline adicionada ao mapa");

                // Obtém e exibe informações sobre a viagem
                JSONArray legs = route.getJSONArray("legs");
                if (legs.length() > 0) {
                    JSONObject leg = legs.getJSONObject(0);
                    String distance = leg.getJSONObject("distance").getString("text");
                    String duration = leg.getJSONObject("duration").getString("text");

                    Toast.makeText(this, "Distância: " + distance + ", Tempo: " + duration,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Nenhuma rota na resposta");
                Toast.makeText(this, "Nenhuma rota encontrada", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar dados de direções: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao processar dados da rota: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

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

                LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
                poly.add(p);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao decodificar polyline: " + e.getMessage(), e);
        }

        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }
}