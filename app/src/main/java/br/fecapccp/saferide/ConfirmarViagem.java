package br.fecapccp.saferide;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

public class ConfirmarViagem extends AppCompatActivity {

    private AutoCompleteTextView inputOrigem, inputDestino;
    private LinearLayout suggestionsList;
    private ImageButton btnBack, btnAdicionarDestino;
    private MaterialButton btnConfirmarViagem;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private LatLng origemLatLng, destinoLatLng;
    private String origemEndereco, destinoEndereco;
    private boolean isOrigemFocused = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem);

        // Inicializa a API Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        // Inicializa os componentes da UI
        inputOrigem = findViewById(R.id.input_origem);
        inputDestino = findViewById(R.id.input_destino);
        suggestionsList = findViewById(R.id.suggestions_list);
        btnBack = findViewById(R.id.btn_back);
        btnAdicionarDestino = findViewById(R.id.btn_adicionar_destino);
        btnConfirmarViagem = findViewById(R.id.btn_confirmar_viagem);

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
                // Inicia a MapsActivity passando as coordenadas
                Intent intent = new Intent(ConfirmarViagem.this, MapsActivity.class);
                intent.putExtra("ORIGEM_LAT", origemLatLng.latitude);
                intent.putExtra("ORIGEM_LNG", origemLatLng.longitude);
                intent.putExtra("DESTINO_LAT", destinoLatLng.latitude);
                intent.putExtra("DESTINO_LNG", destinoLatLng.longitude);
                intent.putExtra("ORIGEM_ENDERECO", origemEndereco);
                intent.putExtra("DESTINO_ENDERECO", destinoEndereco);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Por favor, selecione origem e destino", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlaces(String query) {
        // Cria a requisição para o autocomplete
        FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setTypeFilter(TypeFilter.ADDRESS)
                .setQuery(query)
                .build();

        // Envia a requisição
        placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                suggestionsList.removeAllViews();

                List<AutocompletePrediction> predictions = task.getResult().getAutocompletePredictions();

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
            } else {
                suggestionsList.setVisibility(View.GONE);
                if (task.getException() != null) {
                    task.getException().printStackTrace();
                }
            }
        });
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

        // TODO: Alterar o ícone se necessário
        // Se tiver um ícone personalizado, pode defini-lo aqui

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
        // Define quais campos queremos obter do lugar
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);

        // Cria a requisição
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        // Envia a requisição
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            LatLng latLng = place.getLatLng();

            if (isOrigem) {
                origemLatLng = latLng;
            } else {
                destinoLatLng = latLng;
            }

            // Verifica se podemos habilitar o botão de confirmar viagem
            if (origemLatLng != null && destinoLatLng != null) {
                btnConfirmarViagem.setEnabled(true);
            }
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Toast.makeText(this, "Erro ao obter detalhes do lugar: " +
                        apiException.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}