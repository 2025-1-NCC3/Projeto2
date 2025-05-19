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
// Removido TypeFilter para buscar todos os tipos, ou pode ser trocado por ESTABLISHMENT, GEOCODE, etc.
// import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Não precisa de "import br.fecap.pi.saferide.R;" se o package da classe é o mesmo do R.

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
    private boolean isOrigemFocused = true; // Para saber qual campo está editando
    private ExecutorService executorService;
    private Handler mainHandler;
    private String apiKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Carrega a chave da API uma vez
        apiKey = getString(R.string.Maps_key); // Ou R.string.Maps_key, conforme o seu strings.xml
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("SUA_CHAVE_API_AQUI")) {
            Log.e(TAG, "Chave da API do Google Maps não configurada corretamente em strings.xml!");
            Toast.makeText(this, "Erro crítico: Chave da API não configurada.", Toast.LENGTH_LONG).show();
            // Considerar finalizar a activity ou desabilitar funcionalidades se a chave não estiver presente
            finish();
            return;
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance(); // Crie um novo token para cada sessão de autocomplete

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
        progressBar = findViewById(R.id.progress_bar); // Certifique-se que este ID existe no seu XML

        if (progressBar == null) {
            Log.w(TAG, "ProgressBar com ID 'progress_bar' não encontrado no layout activity_confirmar_viagem.xml.");
        }

        btnConfirmarViagem.setEnabled(false); // Desabilitado até ter origem e destino válidos
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnAdicionarDestino.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidade de múltiplos destinos em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        // Listener para quando o campo de origem ganha foco
        inputOrigem.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isOrigemFocused = true;
                // Opcional: recriar sessionToken se desejar uma nova sessão a cada foco
                // sessionToken = AutocompleteSessionToken.newInstance();
                if (inputOrigem.getText().length() >= 2) { // Dispara busca se já houver texto
                    searchPlaces(inputOrigem.getText().toString());
                }
            } else {
                // Quando perde o foco, pode esconder sugestões se não clicou em uma
                // mainHandler.postDelayed(() -> suggestionsList.setVisibility(View.GONE), 200);
            }
        });

        // Listener para quando o campo de destino ganha foco
        inputDestino.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isOrigemFocused = false;
                // sessionToken = AutocompleteSessionToken.newInstance();
                if (inputDestino.getText().length() >= 2) {
                    searchPlaces(inputDestino.getText().toString());
                }
            } else {
                // mainHandler.postDelayed(() -> suggestionsList.setVisibility(View.GONE), 200);
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (getCurrentFocus() == inputOrigem || getCurrentFocus() == inputDestino) {
                    if (s.length() >= 2) { // Começa a buscar com 2 ou mais caracteres
                        searchPlaces(s.toString());
                    } else {
                        suggestionsList.setVisibility(View.GONE);
                        suggestionsList.removeAllViews(); // Limpa sugestões antigas
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        inputOrigem.addTextChangedListener(textWatcher);
        inputDestino.addTextChangedListener(textWatcher);

        btnConfirmarViagem.setOnClickListener(v -> {
            if (origemLatLng != null && destinoLatLng != null && origemEndereco != null && destinoEndereco != null) {
                showLoading(true); // Mostrar progresso
                Intent intent = new Intent(ConfirmarViagem.this, MapsActivity.class);
                intent.putExtra("ORIGEM_LAT", origemLatLng.latitude);
                intent.putExtra("ORIGEM_LNG", origemLatLng.longitude);
                intent.putExtra("DESTINO_LAT", destinoLatLng.latitude);
                intent.putExtra("DESTINO_LNG", destinoLatLng.longitude);
                intent.putExtra("ORIGEM_ENDERECO", origemEndereco);
                intent.putExtra("DESTINO_ENDERECO", destinoEndereco);
                startActivity(intent);
                // Não finalizar esta activity aqui se você quiser poder voltar para ela
                // e os campos estiverem preenchidos. Se quiser que ela saia da pilha, use finish().
                // finish();
            } else {
                Toast.makeText(this, "Por favor, selecione uma origem e um destino válidos da lista.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchPlaces(final String query) {
        if (query == null || query.trim().isEmpty()) {
            suggestionsList.setVisibility(View.GONE);
            return;
        }
        showLoading(true);

        // É uma boa prática criar um novo token para cada "sessão" de autocomplete,
        // especialmente se o usuário alterna entre os campos ou demora para digitar.
        // Mas se você já criou um no onCreate e ele é válido, pode reutilizar.
        // Para este exemplo, vamos criar um novo a cada busca para garantir.
        final AutocompleteSessionToken currentSessionToken = AutocompleteSessionToken.newInstance();

        executorService.execute(() -> {
            FindAutocompletePredictionsRequest.Builder requestBuilder =
                    FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(currentSessionToken) // Use o token da sessão atual
                            .setQuery(query)
                            .setCountries("BR"); // Restringe ao Brasil

            // MODIFICAÇÃO: Removido o setTypeFilter para buscar todos os tipos de lugares
            // Isso permitirá encontrar "Allianz Parque" e outros estabelecimentos.
            // Se quisesse apenas estabelecimentos: .setTypeFilter(TypeFilter.ESTABLISHMENT)
            // Se quisesse apenas endereços: .setTypeFilter(TypeFilter.ADDRESS)
            // requestBuilder.setTypeFilter(TypeFilter.ADDRESS); // Linha original comentada/removida

            FindAutocompletePredictionsRequest predictionsRequest = requestBuilder.build();

            placesClient.findAutocompletePredictions(predictionsRequest)
                    .addOnSuccessListener(response -> {
                        mainHandler.post(() -> {
                            showLoading(false);
                            updateSuggestionsList(response.getAutocompletePredictions());
                        });
                    })
                    .addOnFailureListener(exception -> {
                        mainHandler.post(() -> {
                            showLoading(false);
                            suggestionsList.setVisibility(View.GONE); // Esconde em caso de erro
                            if (exception instanceof ApiException) {
                                ApiException apiException = (ApiException) exception;
                                Log.e(TAG, "Place not found or API error: " + apiException.getStatusCode() + " " + apiException.getMessage());
                                Toast.makeText(ConfirmarViagem.this, "Erro ("+ apiException.getStatusCode() + ") ao buscar: " + apiException.getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Erro desconhecido na busca de lugares: ", exception);
                                Toast.makeText(ConfirmarViagem.this, "Erro ao buscar sugestões.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });
    }

    private void updateSuggestionsList(List<AutocompletePrediction> predictions) {
        suggestionsList.removeAllViews(); // Limpa sugestões anteriores

        if (predictions == null || predictions.isEmpty()) {
            suggestionsList.setVisibility(View.GONE);
            return;
        }

        suggestionsList.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AutocompletePrediction prediction : predictions) {
            View suggestionView = inflater.inflate(R.layout.item_place_suggestion, suggestionsList, false);
            TextView primaryText = suggestionView.findViewById(R.id.place_primary_text);
            TextView secondaryText = suggestionView.findViewById(R.id.place_secondary_text);

            primaryText.setText(prediction.getPrimaryText(null)); // Nome do lugar ou parte principal do endereço
            secondaryText.setText(prediction.getSecondaryText(null)); // Detalhes adicionais (cidade, estado, etc.)

            suggestionView.setOnClickListener(v -> {
                // Quando uma sugestão é clicada, esconde o teclado e busca detalhes do lugar
                Utils.hideKeyboard(ConfirmarViagem.this, v); // Método utilitário para esconder teclado
                handlePlaceSelection(prediction);
            });
            suggestionsList.addView(suggestionView);
        }
        // As opções customizadas foram removidas para simplificar,
        // já que a funcionalidade delas não estava implementada.
        // Se precisar delas, adicione-as de volta com a lógica apropriada.
    }


    private void handlePlaceSelection(AutocompletePrediction prediction) {
        String placeId = prediction.getPlaceId();
        // Usar getFullText para o campo de texto é geralmente melhor para o usuário ver o que selecionou.
        String address = prediction.getFullText(null).toString();

        AutoCompleteTextView currentInput;
        if (isOrigemFocused) {
            currentInput = inputOrigem;
            origemEndereco = address; // Armazena o endereço completo
        } else {
            currentInput = inputDestino;
            destinoEndereco = address; // Armazena o endereço completo
        }

        currentInput.setText(address); // Preenche o campo
        currentInput.clearFocus(); // Remove o foco para evitar reabrir sugestões
        suggestionsList.setVisibility(View.GONE); // Esconde a lista

        fetchPlaceDetails(placeId, isOrigemFocused);
    }

    private void fetchPlaceDetails(String placeId, final boolean isForOrigem) {
        showLoading(true);
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken) // Reutilize o token da sessão ou crie um novo se necessário
                .build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener((response) -> {
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    String fetchedAddress = place.getAddress(); // Endereço formatado
                    String placeName = place.getName();         // Nome do lugar

                    Log.i(TAG, "Lugar encontrado: " + placeName + ", Endereço: " + fetchedAddress + ", LatLng: " + latLng);

                    if (latLng != null) {
                        if (isForOrigem) {
                            origemLatLng = latLng;
                            // Atualiza o texto do inputOrigem com o endereço mais completo se desejado,
                            // ou mantém o que o usuário selecionou do autocomplete.
                            // inputOrigem.setText(fetchedAddress != null ? fetchedAddress : placeName);
                            // origemEndereco já foi setado em handlePlaceSelection
                        } else {
                            destinoLatLng = latLng;
                            // inputDestino.setText(fetchedAddress != null ? fetchedAddress : placeName);
                            // destinoEndereco já foi setado em handlePlaceSelection
                        }
                    } else {
                        Log.w(TAG, "LatLng não encontrado para o lugar: " + placeId);
                        Toast.makeText(ConfirmarViagem.this, "Coordenadas não encontradas para este local.", Toast.LENGTH_SHORT).show();
                    }
                    // Recria o token para a próxima sessão/requisição de autocomplete,
                    // pois o token de fetchPlace é diferente (ou pode ser nulo).
                    sessionToken = AutocompleteSessionToken.newInstance();
                    checkIfReadyToConfirm();
                    showLoading(false);
                })
                .addOnFailureListener((exception) -> {
                    showLoading(false);
                    sessionToken = AutocompleteSessionToken.newInstance(); // Recria em caso de falha também
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e(TAG, "Erro API Places (fetchDetails): " + apiException.getStatusCode() + ": " + apiException.getMessage(), apiException);
                        Toast.makeText(this, "Erro ao obter detalhes do lugar ("+ apiException.getStatusCode() +")", Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Erro desconhecido ao obter detalhes: ", exception);
                        Toast.makeText(this, "Erro ao obter detalhes do lugar.", Toast.LENGTH_SHORT).show();
                    }
                    checkIfReadyToConfirm(); // Verifica mesmo em caso de falha para desabilitar o botão se necessário
                });
    }

    private void checkIfReadyToConfirm() {
        btnConfirmarViagem.setEnabled(origemLatLng != null && destinoLatLng != null);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        } else {
            Log.w(TAG, "ProgressBar é nulo, não pode mostrar/esconder o carregamento.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Tenta parar as tarefas imediatamente
        }
    }
}