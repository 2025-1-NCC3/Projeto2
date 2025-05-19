package br.fecap.pi.saferide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Não precisa de "import br.fecap.pi.saferide.R;" se o package da classe é o mesmo do R.

public class ConfirmarViagemMotorista extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ConfirmarViagemMotorista";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 16f;

    // Cores Hexadecimais para Botões (inspiradas no Uber e apps de transporte)
    private static final String COLOR_BUTTON_FICAR_ONLINE = "#000000"; // Preto
    private static final String COLOR_BUTTON_BUSCAR_VIAGENS = "#00A676"; // Verde Claro/Moderno
    private static final String COLOR_BUTTON_PROCURANDO = "#545454";   // Cinza Escuro
    private static final String COLOR_BUTTON_A_CAMINHO = "#0079C0";      // Azul
    private static final String COLOR_BUTTON_INICIAR_VIAGEM = "#FFA000"; // Laranja/Âmbar
    private static final String COLOR_BUTTON_FINALIZAR_VIAGEM = "#D32F2F"; // Vermelho
    private static final String COLOR_BUTTON_PERMISSAO_NEGADA = "#757575"; // Cinza Médio

    // Cores para o switch (mantidas como estavam)
    private static final String SWITCH_THUMB_ACTIVE_COLOR = "#E91E63";
    private static final String SWITCH_TRACK_ACTIVE_COLOR = "#80E91E63";
    private static final String SWITCH_THUMB_INACTIVE_COLOR = "#AAAAAA";
    private static final String SWITCH_TRACK_INACTIVE_COLOR = "#80AAAAAA";

    // Interface elements
    private GoogleMap mMap;
    private MaterialButton tripButton;
    private ImageView backButton;
    private SwitchCompat switchWomen;

    // Location related variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private Marker driverMarker;
    private volatile long lastMapUpdateTime = 0;
    private boolean hasInitialZoom = false;
    private AtomicBoolean isUserInteracting = new AtomicBoolean(false);

    // Status tracking
    private volatile boolean isOnline = false;
    private volatile boolean isSearchingForTrip = false;
    private volatile boolean isTripAssigned = false;
    private volatile boolean womenOnlyMode = false;
    private String currentTripId = null;

    // Usuário atual
    private Usuario usuarioAtual;

    // Simulação de viagens
    private ScheduledExecutorService tripSimulationExecutor;

    // Executor para operações em background
    private ExecutorService executor;
    private Handler mainHandler;

    // Route drawing
    private Polyline currentRoutePolyline;
    private Marker pickupMarker;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem_motorista);

        mainHandler = new Handler(Looper.getMainLooper());

        if (savedInstanceState != null) {
            isOnline = savedInstanceState.getBoolean("isOnline", false);
            isSearchingForTrip = savedInstanceState.getBoolean("isSearching", false);
            isTripAssigned = savedInstanceState.getBoolean("isTripAssigned", false);
            womenOnlyMode = savedInstanceState.getBoolean("womenOnlyMode", false);
        }

        executor = Executors.newFixedThreadPool(2); // Aumentado para 2 para tasks de rota e outras

        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            tripButton = findViewById(R.id.tripButton);
            backButton = findViewById(R.id.backButton);
            switchWomen = findViewById(R.id.switchWomen);

            usuarioAtual = getUsuarioFromIntent();
            configureWomenModeVisibility();

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Log.e(TAG, "Map fragment não encontrado!");
                Toast.makeText(this, "Erro crítico ao carregar o mapa. O app será fechado.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationCallback();
            setupClickListeners();
            // checkLocationPermission(); // Será chamado em onMapReady ou onResume se necessário
        } catch (Exception e) {
            Log.e(TAG, "Erro na inicialização da activity", e);
            Toast.makeText(this, "Erro ao iniciar. Tente novamente.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Usuario getUsuarioFromIntent() {
        Usuario usuario = null;
        try {
            if (getIntent().hasExtra("usuario") && getIntent().getSerializableExtra("usuario") instanceof Usuario) {
                usuario = (Usuario) getIntent().getSerializableExtra("usuario");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recuperar usuário da intent.", e);
        }

        if (usuario == null) {
            Log.w(TAG, "Nenhum usuário válido na Intent, usando usuário simulado.");
            usuario = new Usuario("Motorista", "Simulado", "motorista@simulado.com", "00011122233");
            usuario.setTipoConta("Motorista");
            usuario.setGenero("Feminino"); // Mude para "Masculino" para testar a visibilidade do switch
        }
        return usuario;
    }

    private void setSwitchColors(SwitchCompat switchView, boolean isChecked) {
        if (switchView == null) return;
        try {
            if (isChecked) {
                switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_THUMB_ACTIVE_COLOR)));
                switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_TRACK_ACTIVE_COLOR)));
            } else {
                switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_THUMB_INACTIVE_COLOR)));
                switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor(SWITCH_TRACK_INACTIVE_COLOR)));
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cor inválida para o switch: " + e.getMessage());
        }
    }

    private void configureWomenModeVisibility() {
        if (usuarioAtual == null || switchWomen == null) {
            if (switchWomen != null) switchWomen.setVisibility(View.GONE);
            return;
        }
        if ("Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            switchWomen.setVisibility(View.VISIBLE);
            switchWomen.setChecked(womenOnlyMode);
            setSwitchColors(switchWomen, womenOnlyMode);
            switchWomen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                womenOnlyMode = isChecked;
                setSwitchColors(switchWomen, isChecked);
                Toast.makeText(this, isChecked ? "Modo viagem somente com mulheres ativado" : "Modo viagem normal ativado", Toast.LENGTH_SHORT).show();
                if (isSearchingForTrip) {
                    stopSearchingForTrip();
                    startSearchingForTrip();
                }
            });
        } else {
            switchWomen.setVisibility(View.GONE);
            womenOnlyMode = false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isOnline", isOnline);
        outState.putBoolean("isSearching", isSearchingForTrip);
        outState.putBoolean("isTripAssigned", isTripAssigned);
        outState.putBoolean("womenOnlyMode", womenOnlyMode);
        // TODO: Salvar currentTripId e detalhes da viagem atual se necessário
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        tripButton.setOnClickListener(v -> {
            try {
                toggleDriverStatus();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao alternar status do motorista", e);
                Toast.makeText(this, "Erro ao mudar status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleDriverStatus() {
        if (!isOnline) {
            goOnline();
        } else if (isTripAssigned) {
            // A lógica do botão quando em viagem é tratada nos métodos acceptTrip, startTrip, finishTrip
            Log.d(TAG, "Botão principal clicado durante viagem - ação já definida para o estado atual da viagem.");
        } else if (!isSearchingForTrip) {
            startSearchingForTrip();
        } else {
            stopSearchingForTrip();
        }
    }

    private void goOnline() {
        if (currentLocation == null) {
            Toast.makeText(this, "Aguardando sua localização para ficar online...", Toast.LENGTH_LONG).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates(); // Tenta iniciar updates para obter a localização
            } else {
                checkLocationPermission();
            }
            return;
        }
        isOnline = true;
        updateButtonState("Buscar Viagens", COLOR_BUTTON_BUSCAR_VIAGENS);
        Toast.makeText(this, "Você está online!", Toast.LENGTH_SHORT).show();
        // Garante que as atualizações de localização estão ativas
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void startSearchingForTrip() {
        if (!isOnline) {
            Toast.makeText(this, "Fique online para buscar viagens.", Toast.LENGTH_SHORT).show();
            return;
        }
        isSearchingForTrip = true;
        updateButtonState("Procurando Passageiros...", COLOR_BUTTON_PROCURANDO);
        startTripRequestSimulation();
        Toast.makeText(this, "Procurando passageiros...", Toast.LENGTH_SHORT).show();
    }

    private void stopSearchingForTrip() {
        isSearchingForTrip = false;
        stopTripRequestSimulation(); // Para a simulação
        if (isOnline) {
            updateButtonState("Buscar Viagens", COLOR_BUTTON_BUSCAR_VIAGENS);
        } else {
            updateButtonState("Ficar Online", COLOR_BUTTON_FICAR_ONLINE);
        }
        Toast.makeText(this, "Busca por passageiros interrompida.", Toast.LENGTH_SHORT).show();
    }

    private void updateButtonState(final String text, final String hexColorString) {
        mainHandler.post(() -> {
            if (tripButton == null || isFinishing() || isDestroyed()) return;
            tripButton.setText(text);
            try {
                tripButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hexColorString)));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Cor hexadecimal inválida: " + hexColorString, e);
                tripButton.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY)); // Fallback
            }
        });
    }

    // --- Simulação de Viagem (Lógica existente adaptada) ---
    private void startTripRequestSimulation() {
        stopTripRequestSimulation();
        tripSimulationExecutor = Executors.newSingleThreadScheduledExecutor();
        tripSimulationExecutor.schedule(() -> {
            if (isSearchingForTrip && !isTripAssigned && currentLocation != null) {
                Trip mockTrip = generateNearbyTripRequest();
                if (mockTrip.getOriginAddress() != null) {
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed() && isSearchingForTrip && !isTripAssigned) {
                            showTripRequest("trip_" + System.currentTimeMillis(), mockTrip);
                        }
                    });
                } else if (isSearchingForTrip && !isTripAssigned) {
                    startTripRequestSimulation(); // Tenta novamente se a viagem não foi gerada
                }
            }
        }, new Random().nextInt(7000) + 5000, TimeUnit.MILLISECONDS);
    }

    private void stopTripRequestSimulation() {
        if (tripSimulationExecutor != null && !tripSimulationExecutor.isShutdown()) {
            tripSimulationExecutor.shutdownNow();
            tripSimulationExecutor = null;
        }
    }

    private Trip generateNearbyTripRequest() {
        // ... (Lógica de generateNearbyTripRequest mantida como no seu original) ...
        // Adaptei levemente para garantir que retorne uma Trip válida ou null
        Trip trip = new Trip();
        if (currentLocation == null) {
            Log.w(TAG, "Não é possível gerar viagem simulada: localização atual é nula.");
            return trip; // Retorna viagem vazia
        }
        try {
            Random random = new Random();
            double latOffset = (random.nextDouble() * 0.025) - 0.0125;
            double lngOffset = (random.nextDouble() * 0.025) - 0.0125;
            trip.setOriginLat(currentLocation.getLatitude() + latOffset);
            trip.setOriginLng(currentLocation.getLongitude() + lngOffset);

            double destLatOffset = (random.nextDouble() * 0.09) - 0.045;
            double destLngOffset = (random.nextDouble() * 0.09) - 0.045;
            trip.setDestinationLat(trip.getOriginLat() + destLatOffset);
            trip.setDestinationLng(trip.getOriginLng() + destLngOffset);

            String[] neighborhoods = {"Centro", "Jardins", "Vila Madalena", "Pinheiros", "Moema", "Tatuapé"};
            String[] streets = {"Rua Augusta", "Av. Paulista", "Rua Oscar Freire", "Av. Faria Lima", "Rua da Consolação"};
            trip.setOriginAddress(streets[random.nextInt(streets.length)] + ", " + (random.nextInt(1000) + 100) + " - " + neighborhoods[random.nextInt(neighborhoods.length)]);
            trip.setDestinationAddress(streets[random.nextInt(streets.length)] + ", " + (random.nextInt(1000) + 100) + " - " + neighborhoods[random.nextInt(neighborhoods.length)]);

            float[] results = new float[1];
            Location.distanceBetween(trip.getOriginLat(), trip.getOriginLng(), trip.getDestinationLat(), trip.getDestinationLng(), results);
            trip.setDistanceKm(Math.max(0.5, results[0] / 1000.0));
            trip.setFare(Math.max(5.00, 2.50 + (trip.getDistanceKm() * 2.10)));
            trip.setStatus("waiting_for_driver");
            trip.setRequestedAt(System.currentTimeMillis());

            if (womenOnlyMode) {
                trip.setPassengerGender("Feminino");
            } else {
                trip.setPassengerGender(random.nextBoolean() ? "Masculino" : "Feminino");
            }
            trip.setPassengerId("passageiro_" + (random.nextInt(8999) + 1000));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao gerar viagem simulada", e);
            return new Trip(); // Retorna viagem vazia em caso de erro
        }
        return trip;
    }

    private void showTripRequest(String tripId, Trip trip) {
        // ... (Lógica de showTripRequest mantida como no seu original, adaptada para mainHandler) ...
        if (womenOnlyMode && !"Feminino".equalsIgnoreCase(trip.getPassengerGender())) {
            Log.d(TAG, "Viagem ignorada (não corresponde ao filtro 'somente mulheres'). Buscando novamente.");
            startTripRequestSimulation();
            return;
        }
        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Nova Solicitação de Viagem")
                    .setMessage(formatTripRequestMessage(trip))
                    .setPositiveButton("Aceitar", (dialog, which) -> acceptTrip(tripId, trip))
                    .setNegativeButton("Recusar", (dialog, which) -> {
                        Log.d(TAG, "Viagem recusada pelo motorista. Buscando novamente.");
                        startTripRequestSimulation();
                    })
                    .setCancelable(false)
                    .show();
        });
    }


    private String formatTripRequestMessage(Trip trip) {
        // ... (Lógica de formatTripRequestMessage mantida como no seu original) ...
        StringBuilder message = new StringBuilder();
        message.append("Origem: ").append(trip.getOriginAddress()).append("\n");
        message.append("Destino: ").append(trip.getDestinationAddress()).append("\n");
        message.append(String.format("Distância: %.1f km\n", trip.getDistanceKm()));
        message.append(String.format("Valor Estimado: R$ %.2f", trip.getFare()));
        if (usuarioAtual != null && "Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            message.append("\n\nPassageiro(a): ").append(trip.getPassengerGender());
        }
        return message.toString();
    }

    private void acceptTrip(String tripId, Trip trip) {
        isTripAssigned = true;
        isSearchingForTrip = false;
        currentTripId = tripId;
        trip.setStatus("accepted_by_driver");
        if (usuarioAtual != null) trip.setDriverId(usuarioAtual.getCpf() != null ? usuarioAtual.getCpf() : "driver_id_simulado");
        trip.setAcceptedAt(System.currentTimeMillis());

        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            updateButtonState("A Caminho do Passageiro", COLOR_BUTTON_A_CAMINHO);
            tripButton.setEnabled(true); // Botão pode ser clicável para cancelar ou ver detalhes
            tripButton.setOnClickListener(v -> { /* Lógica para "cancelar a caminho", por exemplo */ });

            clearMapElements(false); // Limpa marcadores e rota anterior, mantém motorista

            LatLng driverCurrentLatLng = null;
            if (currentLocation != null) {
                driverCurrentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            } else {
                Log.w(TAG, "Localização atual do motorista nula ao aceitar viagem.");
                Toast.makeText(this, "Não foi possível obter sua localização atual para traçar a rota.", Toast.LENGTH_LONG).show();
                // Pode querer reverter o estado da viagem ou tentar obter a localização novamente
                return;
            }

            LatLng pickupLatLng = new LatLng(trip.getOriginLat(), trip.getOriginLng());

            if (pickupMarker != null) pickupMarker.remove();
            pickupMarker = mMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Local de Embarque")
                    .snippet(trip.getOriginAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            fetchAndDrawRoute(driverCurrentLatLng, pickupLatLng); // Rota: Motorista -> Passageiro
            Toast.makeText(this, "Viagem aceita! Dirija-se ao local de embarque.", Toast.LENGTH_LONG).show();
        });

        // Simular chegada ao local de embarque
        mainHandler.postDelayed(() -> {
            if (isTripAssigned && currentTripId != null && currentTripId.equals(tripId) && !isFinishing() && !isDestroyed()) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    updateButtonState("Passageiro a Bordo (Iniciar Viagem)", COLOR_BUTTON_INICIAR_VIAGEM);
                    tripButton.setEnabled(true);
                    tripButton.setOnClickListener(v -> startTrip(tripId, trip));
                    Toast.makeText(this, "Você chegou ao local de embarque!", Toast.LENGTH_LONG).show();
                });
            }
        }, 12000); // Aumentei um pouco o tempo de simulação
    }

    private void startTrip(String tripId, Trip trip) {
        trip.setStatus("in_progress");
        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            updateButtonState("Finalizar Viagem", COLOR_BUTTON_FINALIZAR_VIAGEM);
            tripButton.setEnabled(true);
            tripButton.setOnClickListener(v -> finishTrip(tripId, trip));

            clearMapElements(false); // Limpa rota anterior (motorista->embarque), mantém motorista e embarque

            LatLng pickupLatLng = new LatLng(trip.getOriginLat(), trip.getOriginLng());
            LatLng destinationLatLng = new LatLng(trip.getDestinationLat(), trip.getDestinationLng());

            // Re-adiciona marcador de embarque se foi removido
            if(pickupMarker == null && mMap != null) {
                pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Origem").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            } else if (pickupMarker != null) {
                pickupMarker.setTitle("Origem da Viagem"); // Atualiza título
            }


            if (destinationMarker != null) destinationMarker.remove();
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title("Destino Final")
                    .snippet(trip.getDestinationAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            fetchAndDrawRoute(pickupLatLng, destinationLatLng); // Rota: Embarque -> Destino
            Toast.makeText(this, "Viagem iniciada! Siga até o destino.", Toast.LENGTH_SHORT).show();
        });
    }

    private void finishTrip(String tripId, Trip trip) {
        trip.setStatus("completed");
        trip.setCompletedAt(System.currentTimeMillis());
        isTripAssigned = false;
        currentTripId = null;

        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            clearMapElements(true); // Limpa tudo, motorista será readicionado
            if (currentLocation != null) { // Garante que currentLocation não é nulo
                updateDriverLocationOnMap(currentLocation);
            }


            if (isOnline) {
                updateButtonState("Buscar Viagens", COLOR_BUTTON_BUSCAR_VIAGENS);
            } else {
                updateButtonState("Ficar Online", COLOR_BUTTON_FICAR_ONLINE);
            }
            tripButton.setEnabled(true);
            tripButton.setOnClickListener(v -> toggleDriverStatus());


            long tripDurationMillis = trip.getCompletedAt() - trip.getAcceptedAt();
            int tripMinutes = Math.max(1, (int) (tripDurationMillis / (1000 * 60)));
            double finalFare = Math.max(trip.getFare(), 5.0 + (tripMinutes * 0.45)); // Exemplo de cálculo

            new AlertDialog.Builder(this)
                    .setTitle("Viagem Concluída!")
                    .setMessage(formatTripSummary(tripMinutes, trip, finalFare))
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (isOnline && !isSearchingForTrip) {
                            // Opcional: startSearchingForTrip();
                        }
                    })
                    .show();
            Toast.makeText(this, "Viagem finalizada!", Toast.LENGTH_LONG).show();
        });
    }


    private String formatTripSummary(int minutes, Trip trip, double fare) {
        // ... (Lógica de formatTripSummary mantida como no seu original) ...
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Tempo da Viagem: %d minutos\n", minutes));
        summary.append(String.format("Distância Percorrida: %.1f km\n", trip.getDistanceKm()));
        summary.append(String.format("Valor Final: R$ %.2f", fare));
        if (womenOnlyMode && "Feminino".equalsIgnoreCase(trip.getPassengerGender())) {
            summary.append("\n\nObrigado por usar o modo 'Somente Mulheres'!");
        }
        return summary.toString();
    }

    // --- Lógica do Mapa e Localização (adaptada e com otimizações) ---
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            Log.e(TAG, "GoogleMap não pôde ser inicializado.");
            Toast.makeText(this, "Erro ao carregar o mapa.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Verifica permissões antes de habilitar MyLocation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true); // Habilita a camada de localização do Google Maps
                mMap.getUiSettings().setMyLocationButtonEnabled(true); // Botão para centralizar no usuário
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException ao habilitar MyLocation Layer.", e);
            }
        } else {
            checkLocationPermission(); // Pede permissão se ainda não foi concedida
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false); // Desabilita toolbar de ação rápida do Google

        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserInteracting.set(true);
            }
        });
        mMap.setOnCameraIdleListener(() -> isUserInteracting.set(false));

        // Inicia atualizações de localização se permissão já concedida, senão checkLocationPermission tratará
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

        // Define estado inicial do botão principal
        if (isOnline) {
            if(isSearchingForTrip) updateButtonState("Procurando Passageiros...", COLOR_BUTTON_PROCURANDO);
            else if(isTripAssigned) { /* Estado será definido por acceptTrip/startTrip/finishTrip */ }
            else updateButtonState("Buscar Viagens", COLOR_BUTTON_BUSCAR_VIAGENS);
        } else {
            updateButtonState("Ficar Online", COLOR_BUTTON_FICAR_ONLINE);
        }
    }


    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    currentLocation = lastLocation;
                    updateDriverLocationOnMap(currentLocation);
                }
            }
        };
    }

    private void updateDriverLocationOnMap(Location location) {
        if (location == null || mMap == null || isFinishing() || isDestroyed()) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMapUpdateTime < 1500 && driverMarker != null) {
            return; // Limita atualizações para no máximo uma a cada 1.5 segundos
        }
        lastMapUpdateTime = currentTime;

        mainHandler.post(() -> {
            if (mMap == null || isFinishing() || isDestroyed()) return;
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (driverMarker == null) {
                driverMarker = mMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("Você")
                        .anchor(0.5f, 0.5f) // Centraliza o ícone
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_driver_marker))); // SUBSTITUA por seu ícone
            } else {
                driverMarker.setPosition(currentLatLng);
            }
            if (location.hasBearing()) {
                driverMarker.setRotation(location.getBearing());
            }
            if (!hasInitialZoom && !isUserInteracting.get() && !isTripAssigned) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
                hasInitialZoom = true;
            }
        });
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }
        if (locationCallback == null) {
            createLocationCallback();
        }

        long interval = isTripAssigned ? 4000L : 10000L;
        long fastestInterval = isTripAssigned ? 2000L : 5000L;
        int priority = isTripAssigned ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        LocationRequest locationRequest = new LocationRequest.Builder(priority, interval)
                .setMinUpdateIntervalMillis(fastestInterval)
                .build();
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Atualizações de localização solicitadas com prioridade: " + priority + " e intervalo: " + interval);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException ao solicitar atualizações de localização.", e);
            checkLocationPermission();
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, loc -> {
            if (loc != null) {
                currentLocation = loc;
                updateDriverLocationOnMap(currentLocation);
                if (isOnline && tripButton != null && "Aguardando sua localização...".equals(tripButton.getText().toString())) {
                    goOnline();
                }
            } else {
                Log.w(TAG, "Última localização conhecida é nula ao iniciar updates.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Erro ao obter última localização.", e));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Localização Necessária")
                        .setMessage("Este aplicativo precisa da sua localização para funcionar corretamente.")
                        .setPositiveButton("OK", (dialog, which) ->
                                ActivityCompat.requestPermissions(ConfirmarViagemMotorista.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE))
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            Toast.makeText(this, "Permissão de localização não concedida.", Toast.LENGTH_LONG).show();
                            updateButtonState("Permissão Negada", COLOR_BUTTON_PERMISSAO_NEGADA);
                            tripButton.setEnabled(false);
                        })
                        .create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        // Se a permissão já foi concedida, startLocationUpdates será chamado de onMapReady ou onResume
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de localização concedida pelo usuário.");
                if (mMap != null) { // Se o mapa estiver pronto, inicie as atualizações
                    startLocationUpdates();
                }
            } else {
                Log.w(TAG, "Permissão de localização negada pelo usuário.");
                Toast.makeText(this, "Permissão de localização é necessária para usar o app.", Toast.LENGTH_LONG).show();
                updateButtonState("Permissão Negada", COLOR_BUTTON_PERMISSAO_NEGADA);
                tripButton.setEnabled(false);
                // Considere finalizar a activity ou desabilitar funcionalidades do mapa
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && mMap != null) {
            startLocationUpdates();
        }
        if (isSearchingForTrip && !isTripAssigned) {
            startTripRequestSimulation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Atualizações de localização pausadas.");
        }
        stopTripRequestSimulation(); // Para simulação se a activity for pausada
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        stopTripRequestSimulation();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        // ... (Lógica de onBackPressed mantida como no seu original, mas use as novas constantes de cor) ...
        if (isTripAssigned) {
            new AlertDialog.Builder(this)
                    .setTitle("Viagem em Andamento")
                    .setMessage("Você tem uma viagem em andamento. Deseja realmente sair e cancelar a viagem?")
                    .setPositiveButton("Sim, Sair e Cancelar", (dialog, which) -> {
                        isTripAssigned = false; currentTripId = null; clearMapElements(true);
                        if(currentLocation != null) updateDriverLocationOnMap(currentLocation);
                        if (isOnline) stopSearchingForTrip();
                        isOnline = false;
                        updateButtonState("Ficar Online", COLOR_BUTTON_FICAR_ONLINE);
                        tripButton.setEnabled(true);
                        tripButton.setOnClickListener(v -> toggleDriverStatus());
                        super.onBackPressed();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        } else if (isSearchingForTrip) {
            stopSearchingForTrip();
            super.onBackPressed();
        } else if (isOnline) {
            new AlertDialog.Builder(this)
                    .setTitle("Ficar Offline?")
                    .setMessage("Você está online. Deseja ficar offline e sair?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        isOnline = false;
                        updateButtonState("Ficar Online", COLOR_BUTTON_FICAR_ONLINE);
                        tripButton.setEnabled(true);
                        super.onBackPressed();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }


    private void clearMapElements(boolean includeDriverMarker) {
        if (mMap == null) return;
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        if (pickupMarker != null) {
            pickupMarker.remove();
            pickupMarker = null;
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
            destinationMarker = null;
        }
        if (includeDriverMarker && driverMarker != null) {
            driverMarker.remove();
            driverMarker = null;
        }
    }

    private void fetchAndDrawRoute(LatLng origin, LatLng destination) {
        if (mMap == null || origin == null || destination == null) {
            Log.w(TAG, "Não é possível desenhar rota: mapa ou pontos nulos.");
            return;
        }
        // Use a chave correta do seu strings.xml (Maps_key ou Maps_key)
        String apiKey = getString(R.string.Maps_key);
        String url = getDirectionsUrl(origin, destination, apiKey);
        if (url != null) {
            new DirectionsTask().execute(url);
        } else {
            Toast.makeText(this, "Não foi possível montar URL para rota.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng destination, String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("SUA_CHAVE_API_AQUI")) {
            Log.e(TAG, "Chave da API do Google Maps não configurada em strings.xml para Directions!");
            Toast.makeText(this, "Chave da API para rotas não configurada!", Toast.LENGTH_LONG).show();
            return null;
        }
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=driving";
        String params = strOrigin + "&" + strDest + "&" + mode + "&key=" + apiKey;
        return "https://maps.googleapis.com/maps/api/directions/json?" + params;
    }

    private class DirectionsTask extends AsyncTask<String, Void, RouteInfoInternal> {
        @Override
        protected RouteInfoInternal doInBackground(String... url) {
            RouteInfoInternal routeInfo = new RouteInfoInternal();
            if (url == null || url.length == 0 || url[0] == null || url[0].isEmpty()) {
                routeInfo.errorMessage = "URL para Directions API está vazia ou nula.";
                return routeInfo;
            }
            try {
                String jsonData = downloadUrl(url[0]);
                if (jsonData == null || jsonData.isEmpty()) {
                    routeInfo.errorMessage = "Resposta vazia da Directions API.";
                    return routeInfo;
                }
                JSONObject jsonObject = new JSONObject(jsonData);
                routeInfo.apiStatus = jsonObject.optString("status", "STATUS_UNKNOWN");

                if (!"OK".equals(routeInfo.apiStatus)) {
                    routeInfo.errorMessage = jsonObject.optString("error_message", "Erro da API: " + routeInfo.apiStatus);
                    return routeInfo;
                }
                List<List<LatLng>> routes = parseDirections(jsonObject);
                if (routes != null && !routes.isEmpty()) {
                    routeInfo.path = routes.get(0); // Pegamos a primeira rota
                } else {
                    routeInfo.errorMessage = "Nenhuma rota encontrada no JSON.";
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro em DirectionsTask.doInBackground", e);
                routeInfo.errorMessage = "Falha ao processar dados da rota: " + e.getMessage();
            }
            return routeInfo;
        }

        @Override
        protected void onPostExecute(RouteInfoInternal routeInfo) {
            if (mMap == null || isFinishing() || isDestroyed()) return;

            if (currentRoutePolyline != null) {
                currentRoutePolyline.remove();
                currentRoutePolyline = null;
            }

            if (routeInfo.errorMessage != null) {
                Log.e(TAG, "Erro ao obter/desenhar rota: " + routeInfo.errorMessage + " (Status API: " + routeInfo.apiStatus + ")");
                Toast.makeText(ConfirmarViagemMotorista.this, "Não foi possível calcular/desenhar a rota: " + routeInfo.errorMessage, Toast.LENGTH_LONG).show();
            } else if (routeInfo.path != null && !routeInfo.path.isEmpty()) {
                PolylineOptions lineOptions = new PolylineOptions();
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                lineOptions.addAll(routeInfo.path);
                lineOptions.width(15); // Largura da linha
                lineOptions.color(Color.parseColor("#0D47A1")); // Um azul escuro para a rota
                lineOptions.geodesic(true);

                for (LatLng point : routeInfo.path) {
                    boundsBuilder.include(point);
                }
                currentRoutePolyline = mMap.addPolyline(lineOptions);

                if (!isUserInteracting.get()) {
                    try {
                        LatLngBounds bounds = boundsBuilder.build();
                        int padding = 150; // Aumentar padding
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Erro ao animar câmera para limites da rota: " + e.getMessage());
                        if (driverMarker != null && driverMarker.getPosition() != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverMarker.getPosition(), DEFAULT_ZOOM));
                        }
                    }
                }
                Log.d(TAG, "Rota desenhada com sucesso.");
            } else {
                Log.w(TAG, "Caminho da rota vazio ou nulo, mesmo com status OK da API.");
                Toast.makeText(ConfirmarViagemMotorista.this, "Não foi possível desenhar a rota (caminho vazio).", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Classe interna para encapsular o resultado da DirectionsTask
    private static class RouteInfoInternal {
        List<LatLng> path;
        String errorMessage;
        String apiStatus;
    }


    private String downloadUrl(String strUrl) throws Exception {
        // ... (downloadUrl mantido como no seu original) ...
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000); // Timeout de conexão
            urlConnection.setReadTimeout(15000);    // Timeout de leitura
            urlConnection.connect();
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
            Log.e(TAG, "Exceção ao baixar URL: " + e.toString());
            throw e;
        } finally {
            if (iStream != null) iStream.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return data;
    }

    // Removida a ParserTask separada, o parsing agora é feito em DirectionsTask.doInBackground
    // e o desenho em DirectionsTask.onPostExecute

    public List<List<LatLng>> parseDirections(JSONObject jObject) {
        // ... (parseDirections mantido como no seu original) ...
        List<List<LatLng>> routes = new ArrayList<>();
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;
        try {
            jRoutes = jObject.getJSONArray("routes");
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List<LatLng> path = new ArrayList<>();
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline;
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);
                        path.addAll(list);
                    }
                }
                routes.add(path);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear direções JSON", e);
            return null;
        }
        return routes;
    }

    private List<LatLng> decodePoly(String encoded) {
        // ... (decodePoly mantido como no seu original) ...
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                if (index >= len) break; // Evita StringIndexOutOfBoundsException
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                if (index >= len) break; // Evita StringIndexOutOfBoundsException
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }


    // --- Classe Trip e Usuario (mantidas como no seu original) ---
    public static class Trip implements java.io.Serializable {
        // ... (conteúdo da classe Trip) ...
        private double originLat, originLng, destinationLat, destinationLng;
        private String originAddress, destinationAddress;
        private double distanceKm, fare;
        private String status, passengerId, driverId, passengerGender;
        private long requestedAt, acceptedAt, completedAt;

        public Trip() {}

        public double getOriginLat() { return originLat; }
        public void setOriginLat(double originLat) { this.originLat = originLat; }
        public double getOriginLng() { return originLng; }
        public void setOriginLng(double originLng) { this.originLng = originLng; }
        public double getDestinationLat() { return destinationLat; }
        public void setDestinationLat(double destinationLat) { this.destinationLat = destinationLat; }
        public double getDestinationLng() { return destinationLng; }
        public void setDestinationLng(double destinationLng) { this.destinationLng = destinationLng; }
        public String getOriginAddress() { return originAddress; }
        public void setOriginAddress(String originAddress) { this.originAddress = originAddress; }
        public String getDestinationAddress() { return destinationAddress; }
        public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
        public double getFare() { return fare; }
        public void setFare(double fare) { this.fare = fare; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getRequestedAt() { return requestedAt; }
        public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }
        public long getAcceptedAt() { return acceptedAt; }
        public void setAcceptedAt(long acceptedAt) { this.acceptedAt = acceptedAt; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        public String getPassengerId() { return passengerId; }
        public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
        public String getDriverId() { return driverId; }
        public void setDriverId(String driverId) { this.driverId = driverId; }
        public String getPassengerGender() { return passengerGender; }
        public void setPassengerGender(String passengerGender) { this.passengerGender = passengerGender; }
    }

    // Mantenha sua classe Usuario definida
    public static class Usuario implements java.io.Serializable {
        private String nome, sobrenome, email, cpf, tipoConta, genero;
        public Usuario(String nome, String sobrenome, String email, String cpf) {
            this.nome = nome; this.sobrenome = sobrenome; this.email = email; this.cpf = cpf;
        }
        public String getGenero() { return genero; }
        public void setGenero(String genero) { this.genero = genero; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; } // Adicionado setCpf se necessário
        public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }
        // Adicione outros getters/setters conforme necessário
    }
}