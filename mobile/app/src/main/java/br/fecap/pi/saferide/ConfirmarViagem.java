package br.fecap.pi.saferide;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.fecap.pi.saferide.R;

public class ConfirmarViagem extends AppCompatActivity {

    private static final String TAG = "ConfirmarViagem";
    private AutoCompleteTextView inputOrigem, inputDestino;
    private LinearLayout suggestionsList;
    private ImageButton btnBack, btnAdicionarDestino;
    private MaterialButton btnConfirmarViagem;
    private ProgressBar progressBar;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private LatLng origemLatLng, destinoLatLng;
    private String origemEndereco, destinoEndereco;
    private boolean isOrigemFocused = true;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem);

        // Inicializa executor service para operações em background
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Inicializa a API Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        // Inicializa os componentes da UI
        initializeUI();
        setupListeners();
    }

    private void initializeUI() {
        inputOrigem = findViewById(R.id.input_origem);
        inputDestino = findViewById(R.id.input_destino);
        suggestionsList = findViewById(R.id.suggestions_list);
        btnBack = findViewById(R.id.btn_back);
        btnAdicionarDestino = findViewById(R.id.btn_adicionar_destino);
        btnConfirmarViagem = findViewById(R.id.btn_confirmar_viagem);

        // Inicialmente o botão deve estar desabilitado até que origem e destino sejam selecionados
        btnConfirmarViagem.setEnabled(false);

        // Adicione o ProgressBar ao layout se ainda não existe
        progressBar = findViewById(R.id.progress_bar);
        if (progressBar == null) {
            Log.w(TAG, "ProgressBar não encontrado no layout. Adicione-o ao seu XML.");
        }
    }

    private void setupListeners() {
        // Configurar botão voltar
        btnBack.setOnClickListener(v -> finish());

        // Configurar botão adicionar destino
        btnAdicionarDestino.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidade de múltiplos destinos em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        // Configurar eventos de foco
        inputOrigem.setOnFocusChangeListener((v, hasFocus) -> {
            isOrigemFocused = hasFocus;
            if (hasFocus) {
                if (inputOrigem.getText().length() >= 2) {
                    searchPlaces(inputOrigem.getText().toString());
                }
            }
        });

        inputDestino.setOnFocusChangeListener((v, hasFocus) -> {
            isOrigemFocused = !hasFocus;
            if (hasFocus) {
                if (inputDestino.getText().length() >= 2) {
                    searchPlaces(inputDestino.getText().toString());
                }
            }
        });

        // Configurar TextWatcher para busca de endereços
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchPlaces(s.toString());
                } else {
                    suggestionsList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        inputOrigem.addTextChangedListener(textWatcher);
        inputDestino.addTextChangedListener(textWatcher);

        // Botão confirmar viagem
        btnConfirmarViagem.setOnClickListener(v -> {
            if (origemLatLng != null && destinoLatLng != null) {
                // Mostrar ProgressBar enquanto prepara para iniciar a próxima activity
                showLoading(true);

                // Inicia a MapsActivity passando as coordenadas
                Intent intent = new Intent(ConfirmarViagem.this, MapsActivity.class);
                intent.putExtra("ORIGEM_LAT", origemLatLng.latitude);
                intent.putExtra("ORIGEM_LNG", origemLatLng.longitude);
                intent.putExtra("DESTINO_LAT", destinoLatLng.latitude);
                intent.putExtra("DESTINO_LNG", destinoLatLng.longitude);
                intent.putExtra("ORIGEM_ENDERECO", origemEndereco);
                intent.putExtra("DESTINO_ENDERECO", destinoEndereco);

                // Inicia a activity e finaliza a atual para evitar que fique no backstack
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Por favor, selecione origem e destino", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlaces(final String query) {
        // Mostra o indicador de carregamento
        showLoading(true);

        // Executa a pesquisa em uma thread separada
        executorService.execute(() -> {
            try {
                // Cria a requisição para o autocomplete
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(sessionToken)
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setQuery(query)
                        .setCountries("BR") // Adiciona restrição apenas para Brasil
                        .build();

                // Envia a requisição
                placesClient.findAutocompletePredictions(predictionsRequest)
                        .addOnSuccessListener(response -> {
                            mainHandler.post(() -> {
                                showLoading(false);
                                updateSuggestionsList(response.getAutocompletePredictions(), query);
                            });
                        })
                        .addOnFailureListener(exception -> {
                            mainHandler.post(() -> {
                                showLoading(false);
                                suggestionsList.setVisibility(View.GONE);
                                Toast.makeText(ConfirmarViagem.this,
                                        "Erro ao buscar sugestões: " + exception.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Erro na busca de lugares", exception);
                            });
                        });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Log.e(TAG, "Exceção na busca de lugares", e);
                    Toast.makeText(ConfirmarViagem.this,
                            "Erro na busca: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateSuggestionsList(List<AutocompletePrediction> predictions, String query) {
        suggestionsList.removeAllViews();

        if (predictions.size() > 0) {
            suggestionsList.setVisibility(View.VISIBLE);

            // Infla e adiciona cada sugestão usando o layout personalizado
            for (AutocompletePrediction prediction : predictions) {
                addPlaceItemToList(prediction);
            }

            // Adiciona opção para pesquisar em outra cidade
            addCustomOptionToList("Pesquisar em uma cidade diferente", null);

            // Adiciona opção para obter mais resultados
            addCustomOptionToList("Obtenha mais resultados para " + query, null);

            // Adiciona opção para definir localização no mapa
            addCustomOptionToList("Defina a localização no mapa", null);
        } else {
            suggestionsList.setVisibility(View.GONE);
        }
    }

    private void addPlaceItemToList(AutocompletePrediction prediction) {
        View suggestionView = LayoutInflater.from(this).inflate(R.layout.item_place_suggestion, suggestionsList, false);

        TextView primaryText = suggestionView.findViewById(R.id.place_primary_text);
        TextView secondaryText = suggestionView.findViewById(R.id.place_secondary_text);

        // Divide o texto completo em partes primária e secundária
        String fullText = prediction.getFullText(null).toString();
        String primaryPart;
        String secondaryPart;

        if (fullText.contains(",")) {
            String[] parts = fullText.split(",", 2);
            primaryPart = parts[0].trim();
            secondaryPart = parts[1].trim();
        } else {
            primaryPart = fullText;
            secondaryPart = prediction.getSecondaryText(null).toString();
        }

        primaryText.setText(primaryPart);
        secondaryText.setText(secondaryPart);

        suggestionView.setOnClickListener(v -> handlePlaceSelection(prediction));

        suggestionsList.addView(suggestionView);
    }

    private void addCustomOptionToList(String text, String iconResource) {
        View optionView = LayoutInflater.from(this).inflate(R.layout.item_place_suggestion, suggestionsList, false);

        TextView primaryText = optionView.findViewById(R.id.place_primary_text);
        TextView secondaryText = optionView.findViewById(R.id.place_secondary_text);

        primaryText.setText(text);
        secondaryText.setVisibility(View.GONE);

        optionView.setOnClickListener(v -> {
            Toast.makeText(ConfirmarViagem.this, text, Toast.LENGTH_SHORT).show();
            suggestionsList.setVisibility(View.GONE);
        });

        suggestionsList.addView(optionView);
    }

    private void handlePlaceSelection(AutocompletePrediction prediction) {
        String placeId = prediction.getPlaceId();
        String address = prediction.getFullText(null).toString();

        // Define qual campo está recebendo o endereço
        if (isOrigemFocused) {
            inputOrigem.setText(address);
            origemEndereco = address;
            fetchPlaceDetails(placeId, true);
        } else {
            inputDestino.setText(address);
            destinoEndereco = address;
            fetchPlaceDetails(placeId, false);
        }

        // Esconde a lista de sugestões
        suggestionsList.setVisibility(View.GONE);
    }

    private void fetchPlaceDetails(String placeId, boolean isOrigem) {
        // Mostra o indicador de carregamento
        showLoading(true);

        // Define quais campos queremos obter do lugar
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);

        // Cria a requisição
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        // Envia a requisição
        placesClient.fetchPlace(request)
                .addOnSuccessListener((response) -> {
                    showLoading(false);
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();

                    if (isOrigem) {
                        origemLatLng = latLng;
                    } else {
                        destinoLatLng = latLng;
                    }

                    // Verifica se podemos habilitar o botão de confirmar viagem
                    btnConfirmarViagem.setEnabled(origemLatLng != null && destinoLatLng != null);
                })
                .addOnFailureListener((exception) -> {
                    showLoading(false);
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Toast.makeText(this, "Erro ao obter detalhes do lugar: " +
                                apiException.getStatusCode(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erro API Places: " + apiException.getMessage(), apiException);
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Encerra o executor service ao destruir a activity
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}