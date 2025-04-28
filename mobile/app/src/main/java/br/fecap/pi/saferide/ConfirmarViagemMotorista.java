package br.fecap.pi.saferide;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import br.fecap.pi.saferide.R;

public class ConfirmarViagemMotorista extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ConfirmarViagemMotorista";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 16f;

    // Interface elements
    private GoogleMap mMap;
    private MaterialButton tripButton;
    private ImageView backButton;
    private Chip chipWomen;

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

    // Simulação de viagens locais
    private List<Trip> mockAvailableTrips = new ArrayList<>();
    private ScheduledExecutorService tripSimulationExecutor;

    // Executor para operações em background
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem_motorista);

        // Inicializar o handler na thread principal para atualizações de UI
        mainHandler = new Handler(Looper.getMainLooper());

        // Restaurar estado se houver
        if (savedInstanceState != null) {
            isOnline = savedInstanceState.getBoolean("isOnline", false);
            isSearchingForTrip = savedInstanceState.getBoolean("isSearching", false);
            isTripAssigned = savedInstanceState.getBoolean("isTripAssigned", false);
            womenOnlyMode = savedInstanceState.getBoolean("womenOnlyMode", false);
        }

        // Inicializar executor com pool fixo
        executor = Executors.newFixedThreadPool(2);

        try {
            // Configurar insets para o layout edge-to-edge
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Initialize UI elements
            tripButton = findViewById(R.id.tripButton);
            backButton = findViewById(R.id.backButton);
            chipWomen = findViewById(R.id.chipWomen);

            // Obter o usuário atual da intent (ou SharedPreferences em um app real)
            usuarioAtual = getUsuarioFromIntent();

            // Verificação apropriada do gênero do usuário
            configureWomenModeVisibility();

            // Initialize map
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                throw new IllegalStateException("Map fragment not found");
            }

            // Initialize location service
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationCallback();

            // Set click listeners
            setupClickListeners();

            // Check location permissions
            checkLocationPermission();

        } catch (Exception e) {
            Log.e(TAG, "Erro na inicialização da activity", e);
            Toast.makeText(this, "Erro ao iniciar. Tente novamente.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Obter usuário da intent
    private Usuario getUsuarioFromIntent() {
        // Em um cenário real, você receberia o usuário de uma fonte confiável como SharedPreferences
        // ou um gerenciador de sessão após o login
        Usuario usuario = null;

        try {
            // Verificar se temos o usuário na intent
            if (getIntent().hasExtra("usuario")) {
                usuario = (Usuario) getIntent().getSerializableExtra("usuario");
            } else {
                // Criar um usuário simulado para fins de teste
                usuario = new Usuario("Nome", "Sobrenome", "email@exemplo.com", "11999999999");
                usuario.setTipoConta("Motorista");
                usuario.setGenero("Masculino"); // Valor padrão
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recuperar usuário", e);
            // Criar um usuário padrão em caso de falha
            usuario = new Usuario("Nome", "Sobrenome", "email@exemplo.com", "11999999999");
            usuario.setTipoConta("Motorista");
            usuario.setGenero("Masculino");
        }

        return usuario;
    }

    private void configureWomenModeVisibility() {
        // Verificar se o motorista é do gênero feminino para mostrar a opção "somente mulheres"
        if (usuarioAtual != null && "Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            chipWomen.setVisibility(View.VISIBLE);
            chipWomen.setChecked(womenOnlyMode);

            chipWomen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                womenOnlyMode = isChecked;
                if (isChecked) {
                    Toast.makeText(this, "Modo somente mulheres ativado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Modo normal ativado", Toast.LENGTH_SHORT).show();
                }

                // Se estiver buscando viagens, reinicia a busca com o novo filtro
                if (isSearchingForTrip) {
                    stopSearchingForTrip();
                    startSearchingForTrip();
                }
            });
        } else {
            chipWomen.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isOnline", isOnline);
        outState.putBoolean("isSearching", isSearchingForTrip);
        outState.putBoolean("isTripAssigned", isTripAssigned);
        outState.putBoolean("womenOnlyMode", womenOnlyMode);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        tripButton.setOnClickListener(v -> {
            try {
                toggleDriverStatus();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao alternar status", e);
                Toast.makeText(this, "Erro ao mudar status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleDriverStatus() {
        if (!isOnline) {
            goOnline();
        } else if (!isSearchingForTrip) {
            startSearchingForTrip();
        } else {
            stopSearchingForTrip();
        }
    }

    private void goOnline() {
        if (currentLocation != null) {
            isOnline = true;
            updateButtonState("Buscar viagens", android.R.color.holo_green_dark);
            Toast.makeText(this, "Você está online! Agora você pode buscar viagens.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Aguardando sua localização...", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSearchingForTrip() {
        isSearchingForTrip = true;
        updateButtonState("Procurando passageiros...", android.R.color.darker_gray);
        startTripRequestSimulation();
        Toast.makeText(this, "Procurando passageiros próximos...", Toast.LENGTH_SHORT).show();
    }

    private void stopSearchingForTrip() {
        isSearchingForTrip = false;
        updateButtonState("Buscar viagens", android.R.color.holo_green_dark);
        stopTripRequestSimulation();
        Toast.makeText(this, "Você parou de procurar passageiros", Toast.LENGTH_SHORT).show();
    }

    // Método para atualizar a UI de forma segura na thread principal
    private void updateButtonState(final String text, final int colorResId) {
        mainHandler.post(() -> {
            if (!isFinishing() && !isDestroyed()) {
                tripButton.setText(text);
                tripButton.setBackgroundTintList(ContextCompat.getColorStateList(this, colorResId));
            }
        });
    }

    private void startTripRequestSimulation() {
        stopTripRequestSimulation(); // Garante que não há simulação anterior rodando

        // Usa ScheduledExecutorService para melhor gerenciamento de recursos
        tripSimulationExecutor = Executors.newSingleThreadScheduledExecutor();

        // Agenda a geração de uma viagem aleatória depois de um tempo aleatório
        tripSimulationExecutor.schedule(() -> {
            if (isSearchingForTrip && !isTripAssigned && currentLocation != null) {
                Trip mockTrip = generateNearbyTripRequest();

                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed() && isSearchingForTrip && !isTripAssigned) {
                        showTripRequest("trip_" + System.currentTimeMillis(), mockTrip);
                    }
                });
            }
        }, new Random().nextInt(8000) + 3000, TimeUnit.MILLISECONDS);
    }

    private void stopTripRequestSimulation() {
        if (tripSimulationExecutor != null && !tripSimulationExecutor.isShutdown()) {
            tripSimulationExecutor.shutdownNow();
            tripSimulationExecutor = null;
        }
    }

    private Trip generateNearbyTripRequest() {
        Trip trip = new Trip();
        try {
            if (currentLocation == null) {
                return trip;
            }

            // Usar ThreadLocalRandom é mais eficiente em ambientes concorrentes, mas Random também funciona
            Random random = new Random();

            // Gerar coordenadas próximas da localização atual
            double latOffset = (random.nextDouble() * 0.025) - 0.0125;
            double lngOffset = (random.nextDouble() * 0.025) - 0.0125;

            trip.setOriginLat(currentLocation.getLatitude() + latOffset);
            trip.setOriginLng(currentLocation.getLongitude() + lngOffset);

            double destLatOffset = (random.nextDouble() * 0.09) - 0.045;
            double destLngOffset = (random.nextDouble() * 0.09) - 0.045;

            trip.setDestinationLat(trip.getOriginLat() + destLatOffset);
            trip.setDestinationLng(trip.getOriginLng() + destLngOffset);

            // Endereços simulados
            String[] neighborhoods = {"Centro", "Jardim das Flores", "Vila Nova", "Parque Industrial"};
            String[] streets = {"Rua das Palmeiras", "Avenida Principal", "Rua dos Girassóis"};

            trip.setOriginAddress(streets[random.nextInt(streets.length)] + ", " +
                    (random.nextInt(1000) + 100) + " - " +
                    neighborhoods[random.nextInt(neighborhoods.length)]);

            trip.setDestinationAddress(streets[random.nextInt(streets.length)] + ", " +
                    (random.nextInt(1000) + 100) + " - " +
                    neighborhoods[random.nextInt(neighborhoods.length)]);

            // Calcula distância e valor com otimização
            float[] results = new float[1];
            Location.distanceBetween(
                    trip.getOriginLat(), trip.getOriginLng(),
                    trip.getDestinationLat(), trip.getDestinationLng(),
                    results);
            trip.setDistanceKm(results[0] / 1000);
            trip.setFare(2.50 + (trip.getDistanceKm() * 2.00));

            trip.setStatus("waiting");
            trip.setRequestedAt(System.currentTimeMillis());

            // Filtro por gênero baseado na opção "somente mulheres"
            if (womenOnlyMode) {
                trip.setPassengerGender("Feminino");
                trip.setPassengerId("passenger_" + (random.nextInt(500) * 2 + 1));
            } else {
                trip.setPassengerGender(random.nextBoolean() ? "Masculino" : "Feminino");
                trip.setPassengerId("passenger_" + random.nextInt(1000));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao gerar viagem simulada", e);
        }
        return trip;
    }

    private void showTripRequest(String tripId, Trip trip) {
        try {
            // Verificar se a viagem atende ao filtro "somente mulheres"
            if (womenOnlyMode && !"Feminino".equalsIgnoreCase(trip.getPassengerGender())) {
                startTripRequestSimulation(); // Buscar outra viagem que atenda ao filtro
                return;
            }

            // Criar e mostrar o diálogo em um handler da thread principal para evitar problemas
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Nova solicitação de viagem")
                            .setMessage(formatTripRequestMessage(trip))
                            .setPositiveButton("Aceitar", (dialog, which) -> acceptTrip(tripId, trip))
                            .setNegativeButton("Recusar", (dialog, which) -> startTripRequestSimulation())
                            .setCancelable(false)
                            .show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao exibir solicitação de viagem", e);
            startTripRequestSimulation(); // Tenta novamente
        }
    }

    private String formatTripRequestMessage(Trip trip) {
        StringBuilder message = new StringBuilder();
        message.append("Origem: ").append(trip.getOriginAddress()).append("\n");
        message.append("Destino: ").append(trip.getDestinationAddress()).append("\n");
        message.append("Distância: ").append(String.format("%.1f", trip.getDistanceKm())).append(" km\n");
        message.append("Valor: R$ ").append(String.format("%.2f", trip.getFare()));

        // Adicionar informação sobre o gênero do passageiro se relevante
        if ("Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            message.append("\n\nPassageiro: ");
            if ("Feminino".equalsIgnoreCase(trip.getPassengerGender())) {
                message.append("Mulher");
            } else if ("Masculino".equalsIgnoreCase(trip.getPassengerGender())) {
                message.append("Homem");
            } else {
                message.append("Não identificado");
            }
        }

        return message.toString();
    }

    private void acceptTrip(String tripId, Trip trip) {
        try {
            isTripAssigned = true;
            currentTripId = tripId;
            trip.setStatus("accepted");
            trip.setDriverId("driver_" + (usuarioAtual != null ? usuarioAtual.getCpf() : "local"));
            trip.setAcceptedAt(System.currentTimeMillis());

            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    tripButton.setText("A caminho do passageiro");
                    tripButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
                    tripButton.setEnabled(false);
                    displayRouteToPassenger(trip);
                }
            });

            Toast.makeText(this, "Viagem aceita! Dirija-se ao local de embarque.", Toast.LENGTH_LONG).show();

            // Simular a chegada ao local de embarque após alguns segundos
            mainHandler.postDelayed(() -> {
                if (isTripAssigned && currentTripId.equals(tripId) && !isFinishing() && !isDestroyed()) {
                    tripButton.setText("Passageiro a bordo");
                    tripButton.setEnabled(true);
                    tripButton.setOnClickListener(v -> startTrip(tripId, trip));
                    Toast.makeText(this, "Você chegou ao local de embarque!", Toast.LENGTH_LONG).show();
                }
            }, 8000);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao aceitar viagem", e);
            Toast.makeText(this, "Erro ao aceitar viagem", Toast.LENGTH_SHORT).show();
            // Em caso de erro, restaurar o estado anterior
            isTripAssigned = false;
            currentTripId = null;
            updateButtonState("Buscar viagens", android.R.color.holo_green_dark);
        }
    }

    private void startTrip(String tripId, Trip trip) {
        try {
            trip.setStatus("in_progress");

            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    tripButton.setText("Finalizar viagem");
                    tripButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark));
                    tripButton.setOnClickListener(v -> finishTrip(tripId, trip));

                    // Adicionar marcador de destino no mapa
                    if (mMap != null) {
                        LatLng destination = new LatLng(trip.getDestinationLat(), trip.getDestinationLng());
                        mMap.addMarker(new MarkerOptions()
                                .position(destination)
                                .title("Destino")
                                .snippet(trip.getDestinationAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    }
                }
            });

            Toast.makeText(this, "Viagem iniciada! Dirija até o destino.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar viagem", e);
            Toast.makeText(this, "Erro ao iniciar viagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishTrip(String tripId, Trip trip) {
        try {
            trip.setStatus("completed");
            trip.setCompletedAt(System.currentTimeMillis());
            isTripAssigned = false;
            currentTripId = null;

            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    // Limpar o mapa e adicionar apenas o marcador do motorista
                    if (mMap != null) {
                        mMap.clear();
                        updateDriverLocationOnMap(currentLocation);
                    }

                    // Restaurar botão para o estado "buscar viagens"
                    tripButton.setText("Buscar viagens");
                    tripButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
                    tripButton.setOnClickListener(v -> toggleDriverStatus());

                    // Calcular duração e valor final da viagem
                    long tripTimeMillis = trip.getCompletedAt() - trip.getAcceptedAt();
                    int tripMinutes = (int) (tripTimeMillis / 60000) + 1;
                    double finalFare = trip.getFare() + (tripMinutes * 0.25);

                    // Mostrar resumo da viagem
                    new AlertDialog.Builder(this)
                            .setTitle("Viagem concluída!")
                            .setMessage(formatTripSummary(tripMinutes, trip, finalFare))
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao finalizar viagem", e);
            Toast.makeText(this, "Erro ao finalizar viagem", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTripSummary(int minutes, Trip trip, double fare) {
        StringBuilder summary = new StringBuilder();
        summary.append("Tempo: ").append(minutes).append(" minutos\n");
        summary.append("Distância: ").append(String.format("%.1f", trip.getDistanceKm())).append(" km\n");
        summary.append("Valor final: R$ ").append(String.format("%.2f", fare));

        // Adicionar informações adicionais se relevante
        if ("Feminino".equalsIgnoreCase(trip.getPassengerGender()) && womenOnlyMode) {
            summary.append("\n\nObrigado por contribuir com a segurança das mulheres!");
        }

        return summary.toString();
    }

    private void displayRouteToPassenger(Trip trip) {
        if (mMap != null && !isFinishing() && !isDestroyed()) {
            try {
                LatLng pickupLocation = new LatLng(trip.getOriginLat(), trip.getOriginLng());
                mMap.addMarker(new MarkerOptions()
                        .position(pickupLocation)
                        .title("Local de embarque")
                        .snippet(trip.getOriginAddress())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                if (!isUserInteracting.get()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 15f));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao exibir rota", e);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                // Otimizar configurações de UI do mapa para melhor desempenho
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                mMap.getUiSettings().setZoomGesturesEnabled(true);
                mMap.getUiSettings().setRotateGesturesEnabled(true);

                // Limitar atualizações de câmera para otimizar a performance
                mMap.setOnCameraMoveStartedListener(reason -> {
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        isUserInteracting.set(true);
                    }
                });

                mMap.setOnCameraIdleListener(() -> {
                    isUserInteracting.set(false);
                });

                startLocationUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao configurar mapa", e);
            Toast.makeText(this, "Erro ao carregar o mapa. Tente novamente.", Toast.LENGTH_SHORT).show();

            // Tentar reiniciar o mapa após um delay
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    if (mapFragment != null) {
                        mapFragment.getMapAsync(this);
                    }
                }
            }, 2000);
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                // Processar apenas a localização mais recente para economia de recursos
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;
                    updateDriverLocationOnMap(location);
                }
            }
        };
    }

    private void updateDriverLocationOnMap(Location location) {
        if (location == null || mMap == null || isFinishing() || isDestroyed()) return;

        // Throttling: limitar atualizações para evitar sobrecarga de UI
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMapUpdateTime < 1000) {
            return; // Ignora atualizações muito frequentes (menos de 1 segundo)
        }
        lastMapUpdateTime = currentTime;

        // Usar runOnUiThread ou mainHandler para atualizações de UI
        mainHandler.post(() -> {
            try {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (driverMarker == null) {
                    // Primeira vez - criar o marcador
                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("Sua localização")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                } else {
                    // Atualizar posição do marcador existente
                    driverMarker.setPosition(currentLatLng);

                    // Atualizar rotação para indicar a direção, se disponível
                    if (location.hasBearing()) {
                        driverMarker.setRotation(location.getBearing());
                    }
                }

                // Centralizar o mapa apenas quando necessário
                if (!hasInitialZoom && !isUserInteracting.get() && !isTripAssigned) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
                    hasInitialZoom = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar localização", e);
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Otimizar frequência de atualizações com base no estado da viagem
        int interval = isTripAssigned ? 5000 : 10000;  // Mais frequente durante viagens
        int priority = isTripAssigned ?
                Priority.PRIORITY_HIGH_ACCURACY :
                Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        // Criar uma solicitação de localização otimizada
        LocationRequest locationRequest = new LocationRequest.Builder(priority, interval)
                .setMinUpdateIntervalMillis(isTripAssigned ? 2000 : 5000)
                .setMaxUpdateDelayMillis(isTripAssigned ? 5000 : 15000)
                .build();

        // Iniciar as atualizações de localização
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Receber localização inicial
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                updateDriverLocationOnMap(location);

                // Se o usuário já estava online, atualizar o estado do botão
                if (isOnline && !isFinishing() && !isDestroyed()) {
                    updateButtonState("Buscar viagens", android.R.color.holo_green_dark);
                }
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, iniciar atualizações de localização
                startLocationUpdates();
            } else {
                // Permissão negada
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permissão necessária")
                        .setMessage("Este aplicativo precisa de acesso à sua localização para funcionar corretamente.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurar estado de atualizações de localização
        if (fusedLocationClient != null && locationCallback != null &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

        // Restaurar estado da busca de viagens
        if (isSearchingForTrip && !isTripAssigned) {
            startTripRequestSimulation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar atualizações de localização para economizar recursos
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Em um app real, salvaria o estado atual no servidor
        if (isSearchingForTrip && !isTripAssigned) {
            stopTripRequestSimulation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar recursos
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Encerrar executores para evitar vazamento de memória
        stopTripRequestSimulation();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            executor = null;
        }

        // Remover callbacks pendentes do handler
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        // Se estiver em viagem, confirmar saída
        if (isTripAssigned) {
            new AlertDialog.Builder(this)
                    .setTitle("Viagem em andamento")
                    .setMessage("Você tem uma viagem em andamento. Deseja realmente sair?")
                    .setPositiveButton("Sim", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Não", null)
                    .show();
        } else if (isSearchingForTrip) {
            // Se estiver procurando viagens, parar a busca e sair
            stopSearchingForTrip();
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    // Classe interna para armazenar dados de uma viagem
    public static class Trip implements java.io.Serializable {
        private double originLat;
        private double originLng;
        private double destinationLat;
        private double destinationLng;
        private String originAddress;
        private String destinationAddress;
        private double distanceKm;
        private double fare;
        private String status;
        private long requestedAt;
        private long acceptedAt;
        private long completedAt;
        private String passengerId;
        private String driverId;
        private String passengerGender;

        public Trip() {
            // Construtor padrão
        }

        // Getters e Setters
        public double getOriginLat() {
            return originLat;
        }

        public void setOriginLat(double originLat) {
            this.originLat = originLat;
        }

        public double getOriginLng() {
            return originLng;
        }

        public void setOriginLng(double originLng) {
            this.originLng = originLng;
        }

        public double getDestinationLat() {
            return destinationLat;
        }

        public void setDestinationLat(double destinationLat) {
            this.destinationLat = destinationLat;
        }

        public double getDestinationLng() {
            return destinationLng;
        }

        public void setDestinationLng(double destinationLng) {
            this.destinationLng = destinationLng;
        }

        public String getOriginAddress() {
            return originAddress;
        }

        public void setOriginAddress(String originAddress) {
            this.originAddress = originAddress;
        }

        public String getDestinationAddress() {
            return destinationAddress;
        }

        public void setDestinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public double getFare() {
            return fare;
        }

        public void setFare(double fare) {
            this.fare = fare;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getRequestedAt() {
            return requestedAt;
        }

        public void setRequestedAt(long requestedAt) {
            this.requestedAt = requestedAt;
        }

        public long getAcceptedAt() {
            return acceptedAt;
        }

        public void setAcceptedAt(long acceptedAt) {
            this.acceptedAt = acceptedAt;
        }

        public long getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(long completedAt) {
            this.completedAt = completedAt;
        }

        public String getPassengerId() {
            return passengerId;
        }

        public void setPassengerId(String passengerId) {
            this.passengerId = passengerId;
        }

        public String getDriverId() {
            return driverId;
        }

        public void setDriverId(String driverId) {
            this.driverId = driverId;
        }

        public String getPassengerGender() {
            return passengerGender;
        }

        public void setPassengerGender(String passengerGender) {
            this.passengerGender = passengerGender;
        }
    }
}