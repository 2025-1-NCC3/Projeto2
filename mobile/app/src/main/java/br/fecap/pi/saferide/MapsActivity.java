package br.fecap.pi.saferide;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import androidx.core.widget.NestedScrollView;

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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;


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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private LatLng origemLatLng, destinoLatLng;
    private String origemEndereco, destinoEndereco;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final double UBERX_PRECO_BASE = 5.0;
    private static final double UBERX_PRECO_KM = 2.14;
    private static final double UBERBLACK_PRECO_BASE = 7.5;
    private static final double UBERBLACK_PRECO_KM = 2.87;
    private static final double UBERGREEN_PRECO_BASE = 5.0;
    private static final double UBERGREEN_PRECO_KM = 2.14;
    private static final double WOMENS_MODE_TAXA = 1.1;

    private static final String SWITCH_THUMB_ACTIVE_COLOR = "#E91E63";
    private static final String SWITCH_TRACK_ACTIVE_COLOR = "#80E91E63";
    private static final String SWITCH_THUMB_INACTIVE_COLOR = "#AAAAAA";
    private static final String SWITCH_TRACK_INACTIVE_COLOR = "#80AAAAAA";

    private LinearLayout uberXOption, uberBlackOption, uberGreenOption;
    private TextView uberXPrice, uberBlackPrice, uberGreenPrice;
    private SwitchCompat switchWomen;
    private ImageView backButton;
    private NestedScrollView bottomSheet;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;

    private TextView tripStatusInfoTextView;
    private LinearLayout rideSelectionLayout;

    private double distanciaEmKm = 0;
    private Usuario usuarioAtual;
    private boolean womenOnlyMode = false;

    private enum TripState { IDLE, REQUESTING, DRIVER_ASSIGNED, DRIVER_ARRIVED, EN_ROUTE_TO_DESTINATION, TRIP_COMPLETED, TRIP_CANCELLED }
    private TripState currentTripState = TripState.IDLE;
    private Handler simulationHandler = new Handler(Looper.getMainLooper());
    private Marker driverCarMarker;
    private LatLng driverSimulatedLocation;
    private List<LatLng> currentRoutePoints;
    private int currentRoutePointIndex = 0;
    private Polyline currentDrawnPolyline;
    private AlertDialog driverFoundDialog;
    private AtomicBoolean isUserInteracting = new AtomicBoolean(false);

    private static final String[] MALE_DRIVER_NAMES = {"Carlos S.", "João M.", "Lucas P.", "Pedro A.", "Mateus R."};
    private static final String[] FEMALE_DRIVER_NAMES = {"Ana L.", "Sofia C.", "Laura B.", "Maria G.", "Juliana F."};
    private static final String[] CAR_MODELS = {"Onix", "HB20", "Kwid", "Mobi", "Argo", "Gol", "Polo", "Versa", "Virtus"};
    private static final String[] CAR_COLORS = {"Preto", "Branco", "Prata", "Cinza", "Vermelho", "Azul"};
    private Random randomGenerator = new Random();

    private Runnable driverEnRouteRunnable;
    private Runnable tripProgressRunnable;


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
        if (savedInstanceState != null) {
            currentTripState = TripState.valueOf(savedInstanceState.getString("currentTripState", TripState.IDLE.name()));
        }

        setupUsuario();
        setupClickListeners();
        getIntentData();
        setupMapFragment();
        updateUiForCurrentTripState();
    }

    private void initViews() {
        bottomSheet = findViewById(R.id.bottomSheet);
        uberXOption = findViewById(R.id.uberXOption);
        uberBlackOption = findViewById(R.id.uberBlackOption);
        uberGreenOption = findViewById(R.id.uberGreenOption);
        uberXPrice = findViewById(R.id.uberXPrice);
        uberBlackPrice = findViewById(R.id.uberBlackPrice);
        uberGreenPrice = findViewById(R.id.uberGreenPrice);
        switchWomen = findViewById(R.id.switchWomen);
        backButton = findViewById(R.id.backButton);

        tripStatusInfoTextView = findViewById(R.id.trip_status_info);
        rideSelectionLayout = findViewById(R.id.ride_selection_layout);

        setSwitchColors(switchWomen, false);
        uberXPrice.setText("R$ --,--");
        uberBlackPrice.setText("R$ --,--");
        uberGreenPrice.setText("R$ --,--");
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
            Log.e(TAG, "Cor inválida para o switch", e);
        }
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int peekHeight = (int) (displayMetrics.heightPixels * 0.45);
        bottomSheetBehavior.setPeekHeight(peekHeight);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (currentTripState != TripState.IDLE && newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Opcional: Forçar expansão
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
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

    private Usuario getUsuarioFromIntent() {
        try {
            if (getIntent().hasExtra("usuario")) {
                return (Usuario) getIntent().getSerializableExtra("usuario");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recuperar usuário", e);
        }
        return createDefaultUser();
    }

    private Usuario createDefaultUser() {
        Usuario usuario = new Usuario("Passageiro", "Padrão", "passageiro@exemplo.com", "11122233344");
        usuario.setTipoConta("Passageiro");
        usuario.setGenero("Feminino");
        return usuario;
    }

    private void configureWomenModeVisibility() {
        if (usuarioAtual != null && "Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            switchWomen.setVisibility(View.VISIBLE);
            switchWomen.setChecked(womenOnlyMode);
            setSwitchColors(switchWomen, womenOnlyMode);
            switchWomen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                womenOnlyMode = isChecked;
                showModeToast(isChecked);
                setSwitchColors(switchWomen, isChecked);
                if (distanciaEmKm > 0) {
                    atualizarPrecosNaUI();
                }
            });
        } else {
            if(switchWomen != null) switchWomen.setVisibility(View.GONE);
            womenOnlyMode = false;
        }
    }

    private void showModeToast(boolean isChecked) {
        Toast.makeText(this,
                isChecked ? "Modo viagem somente com motoristas mulheres ativado" : "Modo viagem normal ativado",
                Toast.LENGTH_SHORT).show();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        View.OnClickListener rideTypeClickListener = v -> {
            if (currentTripState == TripState.IDLE && distanciaEmKm > 0) {
                String rideType = "";
                String price = "";
                int viewId = v.getId();
                if (viewId == R.id.uberXOption) {
                    rideType = "UberX";
                    price = calcularPrecoUberX();
                } else if (viewId == R.id.uberBlackOption) {
                    rideType = "UberBlack";
                    price = calcularPrecoUberBlack();
                } else if (viewId == R.id.uberGreenOption) {
                    rideType = "Uber Green";
                    price = calcularPrecoUberGreen();
                }
                iniciarProcessoDeViagem(rideType, price);
            } else if (distanciaEmKm <= 0) {
                Toast.makeText(this, "Aguarde o cálculo da rota e preço.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Viagem já solicitada ou em andamento.", Toast.LENGTH_SHORT).show();
            }
        };
        if(uberXOption != null) uberXOption.setOnClickListener(rideTypeClickListener);
        if(uberBlackOption != null) uberBlackOption.setOnClickListener(rideTypeClickListener);
        if(uberGreenOption != null) uberGreenOption.setOnClickListener(rideTypeClickListener);
    }

    private void disableRideOptions() {
        if(rideSelectionLayout != null) rideSelectionLayout.setVisibility(View.GONE);
        if(tripStatusInfoTextView != null) tripStatusInfoTextView.setVisibility(View.VISIBLE);
    }

    private void enableRideOptions() {
        if(rideSelectionLayout != null) rideSelectionLayout.setVisibility(View.VISIBLE);
        if(tripStatusInfoTextView != null) tripStatusInfoTextView.setVisibility(View.GONE);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            origemLatLng = new LatLng(intent.getDoubleExtra("ORIGEM_LAT", 0), intent.getDoubleExtra("ORIGEM_LNG", 0));
            destinoLatLng = new LatLng(intent.getDoubleExtra("DESTINO_LAT", 0), intent.getDoubleExtra("DESTINO_LNG", 0));
            origemEndereco = intent.getStringExtra("ORIGEM_ENDERECO");
            destinoEndereco = intent.getStringExtra("DESTINO_ENDERECO");
        }
        if (isInvalidCoordinates()) {
            Toast.makeText(this, "Coordenadas de origem ou destino inválidas.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean isInvalidCoordinates() {
        return origemLatLng == null || destinoLatLng == null ||
                (origemLatLng.latitude == 0 && origemLatLng.longitude == 0 && (origemEndereco == null || origemEndereco.isEmpty())) ||
                (destinoLatLng.latitude == 0 && destinoLatLng.longitude == 0 && (destinoEndereco == null || destinoEndereco.isEmpty()));
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o fragmento do mapa.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("womenOnlyMode", womenOnlyMode);
        outState.putString("currentTripState", currentTripState.name());
    }

    private void iniciarProcessoDeViagem(String tipoUber, String preco) {
        if (currentTripState != TripState.IDLE) {
            Toast.makeText(this, "Aguarde a finalização da solicitação atual.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentTripState = TripState.REQUESTING;
        updateUiForCurrentTripState();
        String message = tipoUber + " confirmado! Valor: R$ " + preco;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Iniciando simulação para encontrar motorista...");
        simulationHandler.postDelayed(this::simularMotoristaEncontradoDialog, 3000 + randomGenerator.nextInt(4000));
    }

    private void simularMotoristaEncontradoDialog() {
        if (currentTripState != TripState.REQUESTING || isFinishing() || isDestroyed()) {
            Log.d(TAG, "simularMotoristaEncontradoDialog: Estado inválido ou Activity finalizando. Estado: " + currentTripState);
            return;
        }

        String driverName;
        if (womenOnlyMode && usuarioAtual != null && "Feminino".equalsIgnoreCase(usuarioAtual.getGenero())) {
            driverName = FEMALE_DRIVER_NAMES[randomGenerator.nextInt(FEMALE_DRIVER_NAMES.length)];
        } else {
            driverName = MALE_DRIVER_NAMES[randomGenerator.nextInt(MALE_DRIVER_NAMES.length)];
        }
        String carModel = CAR_MODELS[randomGenerator.nextInt(CAR_MODELS.length)];
        String carColor = CAR_COLORS[randomGenerator.nextInt(CAR_COLORS.length)];
        String licensePlate = String.format("%C%C%C-%d%C%d%d", (char)('A' + randomGenerator.nextInt(26)), (char)('A' + randomGenerator.nextInt(26)), (char)('A' + randomGenerator.nextInt(26)), randomGenerator.nextInt(10), (char)('A' + randomGenerator.nextInt(26)), randomGenerator.nextInt(10), randomGenerator.nextInt(10));
        int etaMinutes = 2 + randomGenerator.nextInt(5);

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_driver_found, null);

        TextView tvDriverName = dialogView.findViewById(R.id.driver_name_textview);
        TextView tvCarDetails = dialogView.findViewById(R.id.car_details_textview);
        TextView tvLicensePlate = dialogView.findViewById(R.id.license_plate_textview);
        TextView tvEta = dialogView.findViewById(R.id.eta_textview);
        MaterialButton btnAccept = dialogView.findViewById(R.id.accept_button);
        MaterialButton btnDecline = dialogView.findViewById(R.id.decline_button);

        tvDriverName.setText("Motorista: " + driverName);
        tvCarDetails.setText("Carro: " + carModel + " " + carColor);
        tvLicensePlate.setText("Placa: " + licensePlate);
        tvEta.setText(String.format("Chega em aprox. %d min", etaMinutes));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        if (driverFoundDialog != null && driverFoundDialog.isShowing()) {
            driverFoundDialog.dismiss();
        }
        driverFoundDialog = builder.create();

        btnAccept.setOnClickListener(v -> {
            if (driverFoundDialog != null) driverFoundDialog.dismiss();
            Log.d(TAG, "Passageiro aceitou o motorista: " + driverName);
            currentTripState = TripState.DRIVER_ASSIGNED;
            updateUiForCurrentTripState();
            if (tripStatusInfoTextView != null) {
                tripStatusInfoTextView.setText(String.format("Motorista %s a caminho. Chega em ~%d min.", driverName, etaMinutes));
            }
            if (origemLatLng == null) {
                Log.e(TAG, "OrigemLatLng é nulo ao aceitar motorista.");
                cancelTripSimulation("Erro: Local de origem não definido.");
                return;
            }
            double latOffset = (randomGenerator.nextDouble() * 0.02) - 0.01;
            double lngOffset = (randomGenerator.nextDouble() * 0.02) - 0.01;
            driverSimulatedLocation = new LatLng(origemLatLng.latitude + latOffset, origemLatLng.longitude + lngOffset);
            if (mMap != null) {
                if (driverCarMarker != null) driverCarMarker.remove();
                driverCarMarker = mMap.addMarker(new MarkerOptions()
                        .position(driverSimulatedLocation)
                        .title("Motorista: " + driverName)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_driver_car)));
            }
            simularMovimentoMotoristaParaEmbarque(etaMinutes);
        });

        btnDecline.setOnClickListener(v -> {
            if (driverFoundDialog != null) driverFoundDialog.dismiss();
            Log.d(TAG, "Passageiro recusou o motorista. Procurando outro...");
            currentTripState = TripState.REQUESTING;
            if (tripStatusInfoTextView != null) {
                tripStatusInfoTextView.setText("Procurando outro motorista...");
            }
            simulationHandler.postDelayed(this::simularMotoristaEncontradoDialog, 2000 + randomGenerator.nextInt(3000));
        });

        if (!isFinishing() && !isDestroyed()) {
            driverFoundDialog.show();
        } else {
            Log.w(TAG, "Activity finalizando, não foi possível mostrar o diálogo do motorista.");
        }
    }


    private void simularMovimentoMotoristaParaEmbarque(final int totalEtaMinutes) {
        if (currentTripState != TripState.DRIVER_ASSIGNED || isFinishing() || isDestroyed() || origemLatLng == null) return;

        final int totalSteps = Math.max(1, totalEtaMinutes);
        final long stepDurationMillis = 3000;
        final int[] currentStep = {0};

        if (driverEnRouteRunnable != null) {
            simulationHandler.removeCallbacks(driverEnRouteRunnable);
        }

        driverEnRouteRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTripState != TripState.DRIVER_ASSIGNED || isFinishing() || isDestroyed() || origemLatLng == null || driverSimulatedLocation == null) {
                    return;
                }
                currentStep[0]++;
                if (currentStep[0] >= totalSteps) {
                    simularMotoristaChegou();
                    return;
                }
                double progressFraction = 1.0 / totalSteps;
                double newLat = driverSimulatedLocation.latitude + (origemLatLng.latitude - driverSimulatedLocation.latitude) * progressFraction;
                double newLng = driverSimulatedLocation.longitude + (origemLatLng.longitude - driverSimulatedLocation.longitude) * progressFraction;

                driverSimulatedLocation = new LatLng(newLat, newLng);

                if (driverCarMarker != null) driverCarMarker.setPosition(driverSimulatedLocation);
                if (tripStatusInfoTextView != null) {
                    int remainingMinutes = Math.max(0, (int)Math.ceil((double)(totalSteps - currentStep[0]) * stepDurationMillis / 60000.0) );
                    tripStatusInfoTextView.setText(String.format("Motorista a caminho... Chega em ~%d min.", remainingMinutes));
                }
                simulationHandler.postDelayed(this, stepDurationMillis);
            }
        };
        simulationHandler.postDelayed(driverEnRouteRunnable, stepDurationMillis);
    }


    private void simularMotoristaChegou() {
        if (driverEnRouteRunnable != null) {
            simulationHandler.removeCallbacks(driverEnRouteRunnable);
            driverEnRouteRunnable = null;
        }
        if (currentTripState != TripState.DRIVER_ASSIGNED || isFinishing() || isDestroyed()) return;
        currentTripState = TripState.DRIVER_ARRIVED;
        updateUiForCurrentTripState();
        if (driverCarMarker != null && origemLatLng != null) {
            driverCarMarker.setPosition(origemLatLng);
        }
    }

    private void iniciarViagemSimulada() {
        if (driverEnRouteRunnable != null) {
            simulationHandler.removeCallbacks(driverEnRouteRunnable);
            driverEnRouteRunnable = null;
        }
        if (currentTripState != TripState.DRIVER_ARRIVED || isFinishing() || isDestroyed()) return;
        currentTripState = TripState.EN_ROUTE_TO_DESTINATION;
        updateUiForCurrentTripState();
        if (currentRoutePoints == null || currentRoutePoints.isEmpty()) {
            Log.e(TAG, "Pontos da rota não disponíveis para iniciar simulação da viagem.");
            Toast.makeText(this, "Erro: Rota não carregada para simulação.", Toast.LENGTH_SHORT).show();
            cancelTripSimulation("Rota não disponível.");
            return;
        }
        currentRoutePointIndex = 0;
        simularMovimentoParaDestino();
    }

    private void simularMovimentoParaDestino() {
        if (currentTripState != TripState.EN_ROUTE_TO_DESTINATION || isFinishing() || isDestroyed()) return;
        if (currentRoutePoints == null || currentRoutePointIndex >= currentRoutePoints.size()) {
            simularViagemConcluida();
            return;
        }

        if (tripProgressRunnable != null) {
            simulationHandler.removeCallbacks(tripProgressRunnable);
        }
        tripProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTripState != TripState.EN_ROUTE_TO_DESTINATION || isFinishing() || isDestroyed() || currentRoutePoints == null) return;
                if (currentRoutePointIndex >= currentRoutePoints.size()) {
                    simularViagemConcluida();
                    return;
                }
                driverSimulatedLocation = currentRoutePoints.get(currentRoutePointIndex);
                if (driverCarMarker != null) driverCarMarker.setPosition(driverSimulatedLocation);
                if (mMap != null && !isUserInteracting.get() && driverSimulatedLocation != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(driverSimulatedLocation), 700, null);
                }
                currentRoutePointIndex++;
                simulationHandler.postDelayed(this, 700);
            }
        };
        simulationHandler.postDelayed(tripProgressRunnable, 700);
    }

    private void simularViagemConcluida() {
        if (tripProgressRunnable != null) {
            simulationHandler.removeCallbacks(tripProgressRunnable);
            tripProgressRunnable = null;
        }
        if (isFinishing() || isDestroyed()) return;
        currentTripState = TripState.TRIP_COMPLETED;
        updateUiForCurrentTripState();
    }

    private void cancelTripSimulation(String reason) {
        Log.w(TAG, "Simulação de viagem cancelada: " + reason);
        if (driverEnRouteRunnable != null) simulationHandler.removeCallbacks(driverEnRouteRunnable);
        if (tripProgressRunnable != null) simulationHandler.removeCallbacks(tripProgressRunnable);
        currentTripState = TripState.TRIP_CANCELLED;
        updateUiForCurrentTripState();
        if (driverCarMarker != null) {
            driverCarMarker.remove();
            driverCarMarker = null;
        }
        Toast.makeText(this, "Viagem cancelada: " + reason, Toast.LENGTH_LONG).show();
        simulationHandler.postDelayed(()-> {
            if(!isFinishing() && !isDestroyed() && currentTripState == TripState.TRIP_CANCELLED){
                currentTripState = TripState.IDLE;
                updateUiForCurrentTripState();
            }
        }, 3000);
    }

    private void updateUiForCurrentTripState() {
        if (isFinishing() || isDestroyed() || tripStatusInfoTextView == null || rideSelectionLayout == null) {
            return;
        }
        tripStatusInfoTextView.setOnClickListener(null);

        switch (currentTripState) {
            case IDLE:
                rideSelectionLayout.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setVisibility(View.GONE);
                if (driverCarMarker != null) { driverCarMarker.remove(); driverCarMarker = null; }
                enableRideOptions();
                break;
            case REQUESTING:
                rideSelectionLayout.setVisibility(View.GONE);
                tripStatusInfoTextView.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setText("Procurando motorista...");
                disableRideOptions();
                break;
            case DRIVER_ASSIGNED:
                rideSelectionLayout.setVisibility(View.GONE);
                tripStatusInfoTextView.setVisibility(View.VISIBLE);
                // O texto detalhado é atualizado em simularMovimentoMotoristaParaEmbarque
                disableRideOptions();
                break;
            case DRIVER_ARRIVED:
                rideSelectionLayout.setVisibility(View.GONE);
                tripStatusInfoTextView.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setText("Seu motorista chegou!\nToque aqui para iniciar a viagem.");
                tripStatusInfoTextView.setOnClickListener(v -> iniciarViagemSimulada());
                disableRideOptions();
                break;
            case EN_ROUTE_TO_DESTINATION:
                rideSelectionLayout.setVisibility(View.GONE);
                tripStatusInfoTextView.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setText("Em viagem para " + (destinoEndereco != null ? destinoEndereco.split(",")[0] : "seu destino") + "...");
                disableRideOptions();
                break;
            case TRIP_COMPLETED:
                rideSelectionLayout.setVisibility(View.GONE);
                tripStatusInfoTextView.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setText("Viagem Concluída! Obrigado.");
                simulationHandler.postDelayed(() -> {
                    if (currentTripState == TripState.TRIP_COMPLETED) {
                        currentTripState = TripState.IDLE;
                        updateUiForCurrentTripState();
                    }
                }, 5000);
                break;
            case TRIP_CANCELLED:
                rideSelectionLayout.setVisibility(View.VISIBLE);
                tripStatusInfoTextView.setVisibility(View.GONE);
                if (driverCarMarker != null) { driverCarMarker.remove(); driverCarMarker = null; }
                enableRideOptions();
                break;
        }
        if (currentTripState != TripState.IDLE && currentTripState != TripState.TRIP_CANCELLED) {
            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            Toast.makeText(this, "Falha ao inicializar o mapa.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupMapFeatures();
        addMarkers();

        if (origemLatLng != null && destinoLatLng != null) {
            if (isNetworkAvailable()) {
                new DirectionsTask().execute();
            } else {
                Toast.makeText(this, "Sem conexão com a internet. Não foi possível traçar a rota.", Toast.LENGTH_LONG).show();
                adjustCamera(); // Ajusta para os marcadores se não houver rota
            }
        } else {
            Toast.makeText(this, "Coordenadas de origem ou destino não definidas para traçar rota.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setupMapFeatures() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException em setMyLocationEnabled", e);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void addMarkers() {
        if (mMap == null) return;
        if (origemLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(origemLatLng).title("Origem: " + (origemEndereco != null ? origemEndereco : "")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        if (destinoLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(destinoLatLng).title("Destino: " + (destinoEndereco != null ? destinoEndereco : "")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void adjustCameraToShowRoute(List<LatLng> routePoints) {
        if (mMap == null || routePoints == null || routePoints.isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        if (origemLatLng != null) builder.include(origemLatLng);
        if (destinoLatLng != null) builder.include(destinoLatLng);
        try {
            LatLngBounds bounds = builder.build();
            int padding = 150;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Erro ao ajustar câmera para rota: " + e.getMessage());
            if(origemLatLng != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 12));
        }
    }

    private void adjustCamera() {
        if (mMap == null || origemLatLng == null || destinoLatLng == null) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origemLatLng);
        builder.include(destinoLatLng);
        try {
            LatLngBounds bounds = builder.build();
            int padding = 120;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Erro ao ajustar câmera para marcadores: " + e.getMessage());
            if(origemLatLng != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 12));
        }
    }

    private static class RouteInfo {
        List<LatLng> path;
        double distanceKm;
        String distanceText;
        String durationText;
        String errorMessage;
        String apiStatus;
    }

    private class DirectionsTask extends AsyncTask<Void, Void, MapsActivity.RouteInfo> {
        @Override
        protected MapsActivity.RouteInfo doInBackground(Void... voids) {
            MapsActivity.RouteInfo routeInfo = new MapsActivity.RouteInfo();
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String jsonData = null;

            if (origemLatLng == null || destinoLatLng == null) {
                routeInfo.errorMessage = "Coordenadas de origem ou destino não definidas.";
                return routeInfo;
            }
            try {
                String urlStr = getDirectionsUrl(origemLatLng, destinoLatLng);
                if (urlStr == null) {
                    routeInfo.errorMessage = "Chave da API não configurada para rota.";
                    return routeInfo;
                }
                URL url = new URL(urlStr);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream errorStream = urlConnection.getErrorStream();
                    StringBuilder errorBuffer = new StringBuilder();
                    if (errorStream != null) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) errorBuffer.append(errorLine);
                        errorReader.close();
                    }
                    routeInfo.errorMessage = "Erro HTTP: " + responseCode + (errorBuffer.length() > 0 ? ". Detalhes: " + errorBuffer.toString() : "");
                    return routeInfo;
                }
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) { routeInfo.errorMessage = "Resposta nula."; return routeInfo;}
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line).append("\n");
                if (buffer.length() == 0) {routeInfo.errorMessage = "Resposta vazia."; return routeInfo;}
                jsonData = buffer.toString();

                JSONObject jsonObject = new JSONObject(jsonData);
                routeInfo.apiStatus = jsonObject.optString("status", "UNKNOWN_STATUS");
                if (!"OK".equals(routeInfo.apiStatus)) {
                    routeInfo.errorMessage = jsonObject.optString("error_message", "Erro API: " + routeInfo.apiStatus);
                    return routeInfo;
                }
                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() == 0) {
                    routeInfo.errorMessage = "Nenhuma rota encontrada.";
                    return routeInfo;
                }
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                routeInfo.path = decodePoly(overviewPolyline.getString("points"));

                JSONArray legs = route.getJSONArray("legs");
                if (legs.length() > 0) {
                    JSONObject leg = legs.getJSONObject(0);
                    routeInfo.distanceKm = leg.getJSONObject("distance").getInt("value") / 1000.0;
                    routeInfo.distanceText = leg.getJSONObject("distance").getString("text");
                    routeInfo.durationText = leg.getJSONObject("duration").getString("text");
                }
                return routeInfo;
            } catch (Exception e) {
                Log.e(TAG, "Erro em DirectionsTask.doInBackground", e);
                routeInfo.errorMessage = "Falha ao processar rota: " + e.getMessage();
                return routeInfo;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) try { reader.close(); } catch (IOException e) { Log.e(TAG, "Erro ao fechar reader", e); }
            }
        }

        @Override
        protected void onPostExecute(MapsActivity.RouteInfo routeInfo) {
            if (mMap == null || isFinishing() || isDestroyed()) return;
            if (currentDrawnPolyline != null) {
                currentDrawnPolyline.remove();
            }
            if (routeInfo != null && routeInfo.path != null && !routeInfo.path.isEmpty() && "OK".equals(routeInfo.apiStatus)) {
                currentRoutePoints = routeInfo.path;
                PolylineOptions lineOptions = new PolylineOptions();
                lineOptions.addAll(currentRoutePoints);
                lineOptions.width(15);
                lineOptions.color(Color.parseColor("#1976D2")); // Azul para a rota do passageiro
                lineOptions.geodesic(true);
                currentDrawnPolyline = mMap.addPolyline(lineOptions);
                adjustCameraToShowRoute(currentRoutePoints);
                distanciaEmKm = routeInfo.distanceKm;
                atualizarPrecosNaUI();
            } else {
                String error = "Erro ao obter direções.";
                if(routeInfo != null && routeInfo.errorMessage != null) error = routeInfo.errorMessage;
                Toast.makeText(MapsActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Erro direções: " + error + (routeInfo != null ? " (Status: " + routeInfo.apiStatus + ")" : ""));
                adjustCamera();
                distanciaEmKm = 0;
                atualizarPrecosNaUI();
            }
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String apiKey = getString(R.string.Maps_key); // Adapte para Maps_key se for o caso
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("SUA_CHAVE_API_AQUI")) {
            Log.e(TAG, "Chave da API do Google Maps não configurada em strings.xml!");
            Toast.makeText(this, "Chave da API não configurada!", Toast.LENGTH_LONG).show();
            return null;
        }
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String mode = "mode=driving";
        String language = "language=pt-BR";
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + language + "&key=" + apiKey;
        return "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                if (index >= len) break;
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                if (index >= len) break;
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new LatLng((((double) lat / 1E5)), (((double) lng / 1E5))));
        }
        return poly;
    }

    private String calcularPrecoUberX() {
        double precoTotal = UBERX_PRECO_BASE + (UBERX_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) precoTotal *= WOMENS_MODE_TAXA;
        return formatarPreco(precoTotal);
    }
    private String calcularPrecoUberBlack() {
        double precoTotal = UBERBLACK_PRECO_BASE + (UBERBLACK_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) precoTotal *= WOMENS_MODE_TAXA;
        return formatarPreco(precoTotal);
    }
    private String calcularPrecoUberGreen() {
        double precoTotal = UBERGREEN_PRECO_BASE + (UBERGREEN_PRECO_KM * distanciaEmKm);
        if (womenOnlyMode) precoTotal *= WOMENS_MODE_TAXA;
        return formatarPreco(precoTotal);
    }
    private String formatarPreco(double preco) {
        return new DecimalFormat("0.00").format(Math.max(0, preco));
    }
    private void atualizarPrecosNaUI() {
        if (distanciaEmKm <= 0) {
            Log.w(TAG, "Distância inválida para cálculo de preço: " + distanciaEmKm);
            uberXPrice.setText("R$ --,--");
            uberBlackPrice.setText("R$ --,--");
            uberGreenPrice.setText("R$ --,--");
            return;
        }
        try {
            uberXPrice.setText("R$ " + calcularPrecoUberX().replace(".", ","));
            uberBlackPrice.setText("R$ " + calcularPrecoUberBlack().replace(".", ","));
            uberGreenPrice.setText("R$ " + calcularPrecoUberGreen().replace(".", ","));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao atualizar preços na UI", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) { Log.e(TAG, "SecurityException em setMyLocationEnabled", e); }
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentTripState != TripState.IDLE && currentTripState != TripState.TRIP_COMPLETED && currentTripState != TripState.TRIP_CANCELLED) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar Viagem?")
                    .setMessage("Sua viagem está em andamento ou sendo solicitada. Deseja realmente cancelar?")
                    .setPositiveButton("Sim, Cancelar", (dialog, which) -> {
                        cancelTripSimulation("Cancelado pelo usuário via botão Voltar.");
                    })
                    .setNegativeButton("Não", null)
                    .show();
        } else if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && currentTripState == TripState.IDLE) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (simulationHandler != null) {
            simulationHandler.removeCallbacksAndMessages(null);
        }
        if (driverCarMarker != null) {
            driverCarMarker.remove();
        }
    }

    public static class Usuario implements java.io.Serializable {
        private String nome, sobrenome, email, cpf, tipoConta, genero;
        public Usuario(String nome, String sobrenome, String email, String cpf) {
            this.nome = nome; this.sobrenome = sobrenome; this.email = email; this.cpf = cpf;
        }
        public String getGenero() { return genero; }
        public void setGenero(String genero) { this.genero = genero; }
        public String getCpf() { return cpf; }
        public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }
    }
}