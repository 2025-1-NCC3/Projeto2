
package br.fecap.pi.saferide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
    private static final double UBERGREEN_PRECO_BASE = 5.0;
    private static final double UBERGREEN_PRECO_KM = 2.14;
    private static final double WOMENS_MODE_TAXA = 1.1;

    // Cores para o switch
    private static final String SWITCH_THUMB_ACTIVE_COLOR = "#E91E63";    // Rosa vibrante quando ativo
    private static final String SWITCH_TRACK_ACTIVE_COLOR = "#80E91E63";   // Rosa semi-transparente quando ativo
    private static final String SWITCH_THUMB_INACTIVE_COLOR = "#AAAAAA";  // Cinza esmaecido quando inativo
    private static final String SWITCH_TRACK_INACTIVE_COLOR = "#80AAAAAA"; // Cinza semi-transparente quando inativo

    // Novos elementos de UI
    private LinearLayout uberXOption;
    private LinearLayout uberBlackOption;
    private LinearLayout uberGreenOption;
    private TextView uberXPrice;
    private TextView uberBlackPrice;
    private TextView uberGreenPrice;

    private SwitchCompat switchWomen;
    private ImageView backButton;
    private NestedScrollView bottomSheet;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;

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
        setupBottomSheet();
        restoreInstanceState(savedInstanceState);
        setupUsuario();
        setupClickListeners();
        getIntentData();
        setupMapFragment();
    }

    private void initViews() {
        // Bottom sheet e elementos internos
        bottomSheet = findViewById(R.id.bottomSheet);
        uberXOption = findViewById(R.id.uberXOption);
        uberBlackOption = findViewById(R.id.uberBlackOption);
        uberGreenOption = findViewById(R.id.uberGreenOption);

        // TextViews para preços
        uberXPrice = findViewById(R.id.uberXPrice); // Você precisará adicionar IDs aos TextViews no XML
        uberBlackPrice = findViewById(R.id.uberBlackPrice);
        uberGreenPrice = findViewById(R.id.uberGreenPrice);

        // Outros elementos
        switchWomen = findViewById(R.id.switchWomen);

        // Configurando cores do switch
        setSwitchColors(switchWomen, false);

        backButton = findViewById(R.id.backButton);
    }

    /**
     * Define as cores do switch com base no estado (ativado/desativado)
     * @param switchView O switch a ser colorido
     * @param isChecked Se o switch está ativado ou não
     */
    private void setSwitchColors(SwitchCompat switchView, boolean isChecked) {
        if (isChecked) {
            // Cores quando ativado
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_THUMB_ACTIVE_COLOR)));
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_TRACK_ACTIVE_COLOR)));
        } else {
            // Cores esmaecidas quando desativado
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_THUMB_INACTIVE_COLOR)));
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_TRACK_INACTIVE_COLOR)));
        }
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Definir a altura do peek com base na altura da tela
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        // Definir como aproximadamente 40% da altura da tela
        int peekHeight = (int) (screenHeight * 0.4);
        bottomSheetBehavior.setPeekHeight(peekHeight);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Pode adicionar comportamentos específicos para diferentes estados se necessário
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Animações durante o deslize do bottomSheet se necessário
            }
        });
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

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        uberXOption.setOnClickListener(v -> handleUberXClick());
        uberBlackOption.setOnClickListener(v -> handleUberBlackClick());
        uberGreenOption.setOnClickListener(v -> handleUberGreenClick());
    }

    private void handleUberXClick() {
        if (!corridaConfirmada) {
            corridaConfirmada = true;
            String preco = calcularPrecoUberX();
            confirmarViagem("UberX", preco);
            disableOptions();
        }
    }

    private void handleUberBlackClick() {
        if (!corridaConfirmada) {
            corridaConfirmada = true;
            String preco = calcularPrecoUberBlack();
            confirmarViagem("UberBlack", preco);
            disableOptions();
        }
    }

    private void handleUberGreenClick() {
        if (!corridaConfirmada) {
            corridaConfirmada = true;
            String preco = calcularPrecoUberGreen();
            confirmarViagem("Uber Green", preco);
            disableOptions();
        }
    }

    private void disableOptions() {
        uberXOption.setEnabled(false);
        uberXOption.setAlpha(0.5f);
        uberBlackOption.setEnabled(false);
        uberBlackOption.setAlpha(0.5f);
        uberGreenOption.setEnabled(false);
        uberGreenOption.setAlpha(0.5f);
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
            switchWomen.setVisibility(View.VISIBLE);
            switchWomen.setChecked(womenOnlyMode);

            // Ajustar as cores iniciais com base no estado atual
            setSwitchColors(switchWomen, womenOnlyMode);

            switchWomen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                womenOnlyMode = isChecked;
                showModeToast(isChecked);
                // Atualizar cores do switch quando o estado muda
                setSwitchColors(switchWomen, isChecked);
                if (distanciaEmKm > 0) {
                    atualizarPrecos();
                }
            });
        } else {
            switchWomen.setVisibility(View.GONE);
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
        uberXOption.setEnabled(true);
        uberXOption.setAlpha(1.0f);
        uberBlackOption.setEnabled(true);
        uberBlackOption.setAlpha(1.0f);
        uberGreenOption.setEnabled(true);
        uberGreenOption.setAlpha(1.0f);
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

        // Iniciar a busca de direções imediatamente após o mapa estar pronto
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
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(origemLatLng);
            builder.include(destinoLatLng);
            final LatLngBounds bounds = builder.build();

            int padding = 120; // offset from edges of the map in pixels
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover câmera: " + e.getMessage(), e);
            // Fallback para caso o cálculo de bounds falhe
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 12));
        }
    }

    private class DirectionsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String url = getDirectionsUrl(origemLatLng, destinoLatLng);
                Log.d(TAG, "URL de direções: " + url);
                return downloadUrl(url);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao obter direções", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String data) {
            if (data == null || data.isEmpty()) {
                Log.e(TAG, "Dados de direções vazios ou nulos");
                Toast.makeText(MapsActivity.this,
                        "Não foi possível obter dados da rota", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Dados recebidos: " + data.substring(0, Math.min(data.length(), 200)) + "...");
            parseDirectionsData(data);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&key=" + getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;

        Log.d(TAG, "Requesting directions URL: " + url);
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "HTTP Response Code: " + responseCode);

            if (responseCode != 200) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                Log.e(TAG, "Input stream is null");
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            if (stringBuilder.length() == 0) {
                Log.e(TAG, "Stream was empty");
                return null;
            }

            return stringBuilder.toString();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void parseDirectionsData(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            String status = jsonObject.getString("status");

            Log.d(TAG, "Directions API status: " + status);

            if (!status.equals("OK")) {
                String errorMsg = jsonObject.has("error_message") ?
                        jsonObject.getString("error_message") : "Status: " + status;
                Log.e(TAG, "API Error: " + errorMsg);
                Toast.makeText(this, "Erro na API de direções: " + status, Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() == 0) {
                Log.e(TAG, "Nenhuma rota encontrada");
                Toast.makeText(this, "Nenhuma rota encontrada", Toast.LENGTH_SHORT).show();
                return;
            }

            // Processar a primeira rota encontrada
            processRouteData(routes.getJSONObject(0));

        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar dados de direções", e);
            Toast.makeText(this, "Erro ao processar dados da rota", Toast.LENGTH_SHORT).show();
        }
    }

    private void processRouteData(JSONObject route) throws Exception {
        // Obter o polyline da rota
        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
        String encodedPath = overviewPolyline.getString("points");
        Log.d(TAG, "Encoded Path: " + encodedPath);

        // Decodificar o polyline em uma lista de coordenadas
        List<LatLng> decodedPath = decodePoly(encodedPath);

        if (decodedPath.isEmpty()) {
            Log.e(TAG, "Lista de pontos decodificada está vazia");
            return;
        }

        Log.d(TAG, "Desenhando polyline com " + decodedPath.size() + " pontos");

        // Desenhar a polyline no mapa
        runOnUiThread(() -> {
            PolylineOptions options = new PolylineOptions()
                    .addAll(decodedPath)
                    .width(12)
                    .color(Color.BLUE)
                    .geodesic(true);

            mMap.addPolyline(options);
        });

        // Processar informações de distância e duração
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

        Log.d(TAG, "Distância: " + distance + ", Tempo: " + duration);

        runOnUiThread(() -> {
            atualizarPrecos();
            Toast.makeText(MapsActivity.this,
                    "Distância: " + distance + ", Tempo: " + duration,
                    Toast.LENGTH_LONG).show();
        });
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

    private String calcularPrecoUberGreen() {
        double precoTotal = UBERGREEN_PRECO_BASE + (UBERGREEN_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) {
            precoTotal *= WOMENS_MODE_TAXA;
        }
        return formatarPreco(precoTotal);
    }

    private String formatarPreco(double preco) {
        return new DecimalFormat("0.00").format(preco);
    }

    private void atualizarPrecos() {
        String precoUberX = calcularPrecoUberX();
        String precoUberBlack = calcularPrecoUberBlack();
        String precoUberGreen = calcularPrecoUberGreen();

        // Atualiza os textos de preço nos TextViews com o símbolo R$ para corresponder ao layout
        uberXPrice.setText("R$ " + precoUberX);
        uberBlackPrice.setText("R$ " + precoUberBlack);
        uberGreenPrice.setText("R$ " + precoUberGreen);
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
            Log.e(TAG, "Error decoding polyline", e);
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