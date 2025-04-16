package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ConfirmarViagem extends AppCompatActivity {
    private static final String TAG = "ConfirmarViagem";
    private PlacesClient placesClient;
    private AutoCompleteTextView origemInput, destinoInput;
    private ImageButton btnAdicionarDestino, btnBack;
    private Button btnConfirmarViagem; // Novo botão para confirmar viagem
    private AutocompleteSessionToken sessionToken;
    private LinearLayout suggestionsList;

    // Variável para rastrear qual campo está sendo preenchido
    private boolean preenchendoOrigem = false;

    // Para armazenar os dados dos locais selecionados
    private String origemPlaceId, destinoPlaceId;
    private double origemLat, origemLng, destinoLat, destinoLng;
    private String origemEndereco, destinoEndereco;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_viagem);

        // Inicializar Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAonm1HAxkQGlm5vpUsNmTQZ0w4HTtsHkU", Locale.getDefault());
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        // Inicializar componentes de UI
        origemInput = findViewById(R.id.input_origem);
        destinoInput = findViewById(R.id.input_destino);
        btnAdicionarDestino = findViewById(R.id.btn_adicionar_destino);
        btnBack = findViewById(R.id.btn_back);
        suggestionsList = findViewById(R.id.suggestions_list);

        // Adicionar botão para confirmar viagem - você precisará adicionar este botão ao layout
        btnConfirmarViagem = findViewById(R.id.btn_confirmar_viagem);

        // Configurar listeners para os campos de texto
        setupTextChangeListener(origemInput);
        setupTextChangeListener(destinoInput);

        // Configurar listener para botão voltar
        btnBack.setOnClickListener(v -> finish());

        // Configurar listener para botão adicionar destino
        btnAdicionarDestino.setOnClickListener(v -> {
            Toast.makeText(this, "Adicionar novo destino", Toast.LENGTH_SHORT).show();
        });

        // Configurar listener para o botão confirmar viagem
        if (btnConfirmarViagem != null) {
            btnConfirmarViagem.setOnClickListener(v -> {
                if (validarCampos()) {
                    iniciarViagem();
                }
            });
        }
    }

    private boolean validarCampos() {
        if (origemEndereco == null || origemEndereco.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione o ponto de partida", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (destinoEndereco == null || destinoEndereco.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione o destino", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void iniciarViagem() {
        // Criar Intent para abrir a MapaActivity
        Intent intent = new Intent(ConfirmarViagem.this, MapsActivity.class);

        // Passar os dados como extras
        intent.putExtra("origem_lat", origemLat);
        intent.putExtra("origem_lng", origemLng);
        intent.putExtra("origem_endereco", origemEndereco);

        intent.putExtra("destino_lat", destinoLat);
        intent.putExtra("destino_lng", destinoLng);
        intent.putExtra("destino_endereco", destinoEndereco);

        startActivity(intent);
    }

    private void setupTextChangeListener(final AutoCompleteTextView textView) {
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    fetchPlacePredictions(s.toString());
                } else {
                    suggestionsList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchPlacePredictions(String query) {
        // Criar a requisição para buscar previsões de lugares
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(sessionToken)
                .setQuery(query)
                .setCountries("BR") // Limitar a resultados do Brasil
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> displayPredictions(response))
                .addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.e(TAG, "Place API error: " + apiException.getStatusCode() + " " + apiException.getMessage());
                    } else {
                        Log.e(TAG, "Unknown error: ", e);
                    }
                });
    }

    private void displayPredictions(FindAutocompletePredictionsResponse response) {
        // Limpar a lista de sugestões
        suggestionsList.removeAllViews();

        // Se não há sugestões, ocultar a lista
        if (response.getAutocompletePredictions().isEmpty()) {
            suggestionsList.setVisibility(View.GONE);
            return;
        }

        // Exibir cada sugestão como um item na lista
        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
            // Criar uma nova view para cada sugestão
            View suggestionItem = getLayoutInflater().inflate(R.layout.item_place_suggestion, suggestionsList, false);

            // Configurar o texto principal (nome da rua/lugar)
            android.widget.TextView primaryText = suggestionItem.findViewById(R.id.place_primary_text);
            primaryText.setText(prediction.getPrimaryText(null));

            // Configurar o texto secundário (bairro/cidade)
            android.widget.TextView secondaryText = suggestionItem.findViewById(R.id.place_secondary_text);
            secondaryText.setText(prediction.getSecondaryText(null));

            // Configurar o click listener
            final String placeId = prediction.getPlaceId();
            final String fullAddress = prediction.getPrimaryText(null) + ", " + prediction.getSecondaryText(null);

            suggestionItem.setOnClickListener(v -> {
                // Definir a flag para rastrear qual campo está sendo preenchido
                preenchendoOrigem = origemInput.hasFocus();

                // Buscar detalhes do lugar selecionado (incluindo coordenadas)
                fetchPlaceDetails(placeId, fullAddress);

                // Definir o endereço selecionado no campo de texto
                if (origemInput.hasFocus()) {
                    origemInput.setText(fullAddress);
                    origemInput.clearFocus();
                } else if (destinoInput.hasFocus()) {
                    destinoInput.setText(fullAddress);
                    destinoInput.clearFocus();
                }

                // Ocultar a lista de sugestões
                suggestionsList.setVisibility(View.GONE);
            });

            // Adicionar o item à lista de sugestões
            suggestionsList.addView(suggestionItem);
        }

        // Exibir a lista de sugestões
        suggestionsList.setVisibility(View.VISIBLE);
    }

    private void fetchPlaceDetails(String placeId, String endereco) {
        // Definir os campos que queremos recuperar
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG);

        // Criar a requisição
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build();

        // Executar a requisição
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place lugar = response.getPlace();

                    // Usar a variável de rastreamento em vez de verificar o foco
                    if (preenchendoOrigem) {
                        origemPlaceId = placeId;
                        origemEndereco = endereco;
                        if (lugar.getLatLng() != null) {
                            origemLat = lugar.getLatLng().latitude;
                            origemLng = lugar.getLatLng().longitude;
                            Log.d(TAG, "Origem: " + origemLat + ", " + origemLng);
                        }
                    } else {
                        destinoPlaceId = placeId;
                        destinoEndereco = endereco;
                        if (lugar.getLatLng() != null) {
                            destinoLat = lugar.getLatLng().latitude;
                            destinoLng = lugar.getLatLng().longitude;
                            Log.d(TAG, "Destino: " + destinoLat + ", " + destinoLng);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.e(TAG, "Place details fetch error: " + apiException.getStatusCode() + " " + apiException.getMessage());
                    } else {
                        Log.e(TAG, "Unknown error on place details: ", e);
                    }
                });
    }
}