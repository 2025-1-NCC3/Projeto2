package br.fecap.pi.saferide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import br.fecap.pi.saferide.R;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private LatLng origemLatLng, destinoLatLng;
    private String origemEndereco, destinoEndereco;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Preços base e por km
    private static final double UBERX_PRECO_BASE = 5.0;
    private static final double UBERX_PRECO_KM = 2.14;
    private static final double UBERBLACK_PRECO_BASE = 7.5;
    private static final double UBERBLACK_PRECO_KM = 2.87;
    private static final double WOMENS_MODE_TAXA = 1.1;

    private MaterialButton uberXButton;
    private MaterialButton uberBlackButton;
    private Chip chipWomen;
    private ImageView backButton;

    private double distanciaEmKm = 0;
    private boolean corridaConfirmada = false;
    private Usuario usuarioAtual;
    private boolean womenOnlyMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        restoreInstanceState(savedInstanceState);
        setupUsuario();
        setupButtons();
        getIntentData();
        setupMapFragment();
    }

    private void initViews() {
        uberXButton = findViewById(R.id.uberXButton);
        uberBlackButton = findViewById(R.id.uberBlackButton);
        chipWomen = findViewById(R.id.chipWomen);
        backButton = findViewById(R.id.backButton);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            womenOnlyMode = savedInstanceState.getBoolean("womenOnlyMode", false);
        }
    }

    private void setupUsuario() {
        usuarioAtual = getUsuarioFromIntent();
        configureWomenModeVisibility();
    }

    private void setupButtons() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        uberXButton.setOnClickListener(v -> handleUberXClick());
        uberBlackButton.setOnClickListener(v -> handleUberBlackClick());
    }

    private void handleUberXClick() {
        if (!corridaConfirmada) {
            corridaConfirmada = true;
            String preco = calcularPrecoUberX();
            confirmarViagem("UberX", preco);
            disableButtons();
        }
    }

    private void handleUberBlackClick() {
        if (!corridaConfirmada) {
            corridaConfirmada = true;
            String preco = calcularPrecoUberBlack();
            confirmarViagem("UberBlack", preco);
            disableButtons();
        }
    }

    private void disableButtons() {
        uberXButton.setEnabled(false);
        uberBlackButton.setEnabled(false);
    }

    private void getIntentData() {
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

        if (isInvalidCoordinates()) {
            Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean isInvalidCoordinates() {
        return origemLatLng == null || destinoLatLng == null ||
                (origemLatLng.latitude == 0 && origemLatLng.longitude == 0) ||
                (destinoLatLng.latitude == 0 && destinoLatLng.longitude == 0);
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o mapa", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("womenOnlyMode", womenOnlyMode);
    }

    private Usuario getUsuarioFromIntent() {
        try {
            if (getIntent().hasExtra("usuario")) {
                return (Usuario) getIntent().getSerializableExtra("usuario");
            } else {
                return createDefaultUser();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recuperar usuário", e);
            return createDefaultUser();
        }
    }

    private Usuario createDefaultUser() {
        Usuario usuario = new Usuario("Ana", "Silva", "ana@exemplo.com", "11999999999");
        usuario.setTipoConta("Passageiro");
        usuario.setGenero("Feminino");
        return usuario;
    }

    private void configureWomenModeVisibility() {
        if (usuarioAtual != null && "Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            chipWomen.setVisibility(View.VISIBLE);
            chipWomen.setChecked(womenOnlyMode);

            chipWomen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                womenOnlyMode = isChecked;
                showModeToast(isChecked);
                if (distanciaEmKm > 0) {
                    atualizarPrecosNaBotoes();
                }
            });
        } else {
            chipWomen.setVisibility(View.GONE);
            womenOnlyMode = false;
        }
    }

    private void showModeToast(boolean isChecked) {
        Toast.makeText(this,
                isChecked ? "Modo somente motoristas mulheres ativado" : "Modo normal ativado",
                Toast.LENGTH_SHORT).show();
    }

    private void confirmarViagem(String tipoUber, String preco) {
        try {
            showConfirmationToast(tipoUber, preco);
            new TripConfirmationTask().execute(tipoUber, preco);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao confirmar viagem", e);
            handleConfirmationError();
        }
    }

    private void showConfirmationToast(String tipoUber, String preco) {
        String message = tipoUber + " confirmado! Valor: R$ " + preco +
                "\n" + (womenOnlyMode ? "Procurando motorista mulher..." : "Procurando motorista...");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleConfirmationError() {
        Toast.makeText(this, "Erro ao confirmar viagem. Tente novamente.", Toast.LENGTH_SHORT).show();
        corridaConfirmada = false;
        uberXButton.setEnabled(true);
        uberBlackButton.setEnabled(true);
    }

    private class TripConfirmationTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                Thread.sleep(2000);
                return null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Erro no Task de confirmação", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showTripRequestedToast();
        }
    }

    private void showTripRequestedToast() {
        Toast.makeText(MapsActivity.this,
                "Viagem solicitada com sucesso! Aguarde um motorista aceitar.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMapFeatures();
        addMarkers();
        adjustCamera();
        new DirectionsTask().execute();
    }

    private void setupMapFeatures() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void addMarkers() {
        mMap.addMarker(new MarkerOptions()
                .position(origemLatLng)
                .title("Origem: " + origemEndereco)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(destinoLatLng)
                .title("Destino: " + destinoEndereco)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void adjustCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origemLatLng);
        builder.include(destinoLatLng);
        final LatLngBounds bounds = builder.build();

        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover câmera: " + e.getMessage(), e);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 12));
        }
    }

    private class DirectionsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String url = getDirectionsUrl(origemLatLng, destinoLatLng);
                return downloadUrl(url);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao obter direções", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String data) {
            if (data == null || data.isEmpty()) {
                Toast.makeText(MapsActivity.this,
                        "Não foi possível obter dados da rota", Toast.LENGTH_SHORT).show();
                return;
            }
            parseDirectionsData(data);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + dest.latitude + "," + dest.longitude +
                "&mode=driving" +
                "&key=" + getString(R.string.google_maps_key);
    }

    private String downloadUrl(String strUrl) throws IOException {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);
            urlConnection.connect();

            if (urlConnection.getResponseCode() != 200) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            return buffer.toString();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar stream", e);
                }
            }
        }
    }

    private void parseDirectionsData(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);

            if (!jsonObject.getString("status").equals("OK")) {
                handleApiError(jsonObject);
                return;
            }

            JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                processRouteData(routes.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar dados de direções", e);
            Toast.makeText(this, "Erro ao processar dados da rota", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleApiError(JSONObject jsonObject) throws Exception {
        String status = jsonObject.getString("status");
        String errorMsg = jsonObject.has("error_message") ?
                jsonObject.getString("error_message") : "Status: " + status;
        Log.e(TAG, "Erro na API: " + errorMsg);
        Toast.makeText(this, "Erro na API: " + status, Toast.LENGTH_SHORT).show();
    }

    private void processRouteData(JSONObject route) throws Exception {
        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
        String encodedPath = overviewPolyline.getString("points");

        List<LatLng> decodedPath = decodePoly(encodedPath);
        if (!decodedPath.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(decodedPath)
                    .width(12)
                    .color(Color.BLUE)
                    .geodesic(true));
        }

        JSONArray legs = route.getJSONArray("legs");
        if (legs.length() > 0) {
            processLegData(legs.getJSONObject(0));
        }
    }

    private void processLegData(JSONObject leg) throws Exception {
        int distanciaMetros = leg.getJSONObject("distance").getInt("value");
        distanciaEmKm = distanciaMetros / 1000.0;

        String distance = leg.getJSONObject("distance").getString("text");
        String duration = leg.getJSONObject("duration").getString("text");

        atualizarPrecosNaBotoes();

        Toast.makeText(this, "Distância: " + distance + ", Tempo: " + duration,
                Toast.LENGTH_LONG).show();
    }

    private String calcularPrecoUberX() {
        double precoTotal = UBERX_PRECO_BASE + (UBERX_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) {
            precoTotal *= WOMENS_MODE_TAXA;
        }
        return formatarPreco(precoTotal);
    }

    private String calcularPrecoUberBlack() {
        double precoTotal = UBERBLACK_PRECO_BASE + (UBERBLACK_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) {
            precoTotal *= WOMENS_MODE_TAXA;
        }
        return formatarPreco(precoTotal);
    }

    private String formatarPreco(double preco) {
        return new DecimalFormat("0.00").format(preco);
    }

    private void atualizarPrecosNaBotoes() {
        String precoUberX = calcularPrecoUberX();
        String precoUberBlack = calcularPrecoUberBlack();

        uberXButton.setText("UberX - R$ " + precoUberX);
        uberBlackButton.setText("UberBlack - R$ " + precoUberBlack);

        String descricao = womenOnlyMode ? " com motorista mulher" : "";
        uberXButton.setContentDescription("UberX" + descricao + " - R$ " + precoUberX);
        uberBlackButton.setContentDescription("UberBlack" + descricao + " - R$ " + precoUberBlack);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

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

            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (corridaConfirmada) {
            showCancelConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showCancelConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancelar corrida")
                .setMessage("Você tem uma solicitação de corrida em andamento. Deseja realmente cancelar?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    corridaConfirmada = false;
                    finish();
                })
                .setNegativeButton("Não", null)
                .show();
    }
}