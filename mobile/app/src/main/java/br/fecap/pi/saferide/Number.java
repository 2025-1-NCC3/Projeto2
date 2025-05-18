package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.fecap.pi.saferide.security.CryptoUtils;
import br.fecap.pi.saferide.R;

public class Number extends AppCompatActivity {

    private Spinner spinnerDDI;
    private EditText editPhone;
    private Button buttonNext, buttonBack;
    private String selectedDDI;
    private Usuario usuario;
    private LinkedHashMap<String, String> ddiMap;
    private Map<String, PhoneFormatInfo> phoneFormats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number);

        // Ajustando padding para evitar sobreposição com barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade Email
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Ligando os elementos do XML às variáveis do Java
        spinnerDDI = findViewById(R.id.spinnerDDI);
        editPhone = findViewById(R.id.editPhone);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // Inicializa os mapas de países e formatos
        initDDIMap();
        initPhoneFormats();

        // Criando um array com os códigos DDI para mostrar no spinner
        List<String> ddiArray = new ArrayList<>(ddiMap.keySet());

        // Criando um Adapter para o Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ddiArray);
        spinnerDDI.setAdapter(adapter);

        // Configurando o Brasil (+55) como padrão
        int defaultPosition = ddiArray.indexOf("+55");
        if (defaultPosition >= 0) {
            spinnerDDI.setSelection(defaultPosition);
        }

        // Listener para capturar o DDI selecionado
        spinnerDDI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String ddiCode = (String) parent.getItemAtPosition(position);
                selectedDDI = ddiMap.get(ddiCode);

                // Altera a dica do EditText conforme o país selecionado
                PhoneFormatInfo formatInfo = phoneFormats.get(selectedDDI);
                if (formatInfo != null) {
                    editPhone.setHint(formatInfo.getExample());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDDI = "55"; // Padrão: Brasil
            }
        });

        // Listener para o botão "Próximo"
        buttonNext.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();

            // Remove formatação para validação
            String phoneClean = phone.replaceAll("[^0-9]", "");

            if (phone.isEmpty()) {
                Toast.makeText(Number.this, "Digite o número de telefone!", Toast.LENGTH_SHORT).show();
            } else {
                // Formata o telefone de acordo com o país selecionado
                String formattedPhone = formatPhoneNumber(phoneClean, selectedDDI);
                String fullPhoneNumber = "+" + selectedDDI + phoneClean;

                if (!isValidPhoneNumber(phoneClean, selectedDDI)) {
                    Toast.makeText(Number.this, "Número de telefone inválido para o país selecionado!", Toast.LENGTH_SHORT).show();
                } else {
                    // Atualiza o objeto Usuario com o número de telefone formatado
                    usuario.setNumber(fullPhoneNumber);

                    // Criptografa o número de telefone formatado
                    String numeroCriptografado = CryptoUtils.encrypt(formattedPhone);

                    // Monta o JSON
                    JSONObject json = new JSONObject();
                    try{
                        json.put("id", getId());
                        json.put("numero", numeroCriptografado);
                    }  catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // Envia para o servidor
                    enviarParaServidor(json.toString());

                    // Passa o objeto Usuario para a próxima atividade (Password)
                    Intent intent = new Intent(Number.this, Password.class);
                    intent.putExtra("usuario", usuario);
                    startActivity(intent);
                }
            }
        });

        // Listener para o botão "Voltar"
        buttonBack.setOnClickListener(v -> {
            // Volta para a tela de Email
            Intent intent = new Intent(Number.this, Email.class);
            startActivity(intent);
            finish(); // Finaliza a atividade atual para liberar memória
        });
    }

    // Método para inicializar os códigos de país com apenas o DDI no spinner
    private void initDDIMap() {
        ddiMap = new LinkedHashMap<>();

        // América do Sul
        ddiMap.put("+55", "55");
        ddiMap.put("+54", "54");
        ddiMap.put("+591", "591");
        ddiMap.put("+56", "56");
        ddiMap.put("+57", "57");
        ddiMap.put("+593", "593");
        ddiMap.put("+592", "592");
        ddiMap.put("+595", "595");
        ddiMap.put("+51", "51");
        ddiMap.put("+597", "597");
        ddiMap.put("+598", "598");
        ddiMap.put("+58", "58");

        // América do Norte
        ddiMap.put("+1", "1");
        ddiMap.put("+52", "52");

        // América Central e Caribe (parte da América do Norte)
        ddiMap.put("+506", "506");
        ddiMap.put("+503", "503");
        ddiMap.put("+502", "502");
        ddiMap.put("+504", "504");
        ddiMap.put("+505", "505");
        ddiMap.put("+507", "507");
        ddiMap.put("+53", "53");
        ddiMap.put("+1876", "1876");
        ddiMap.put("+1809", "1809");

        // Europa
        ddiMap.put("+49", "49");
        ddiMap.put("+43", "43");
        ddiMap.put("+32", "32");
        ddiMap.put("+359", "359");
        ddiMap.put("+385", "385");
        ddiMap.put("+45", "45");
        ddiMap.put("+421", "421");
        ddiMap.put("+386", "386");
        ddiMap.put("+34", "34");
        ddiMap.put("+372", "372");
        ddiMap.put("+358", "358");
        ddiMap.put("+33", "33");
        ddiMap.put("+30", "30");
        ddiMap.put("+36", "36");
        ddiMap.put("+353", "353");
        ddiMap.put("+39", "39");
        ddiMap.put("+371", "371");
        ddiMap.put("+370", "370");
        ddiMap.put("+352", "352");
        ddiMap.put("+356", "356");
        ddiMap.put("+31", "31");
        ddiMap.put("+48", "48");
        ddiMap.put("+351", "351");
        ddiMap.put("+44", "44");
        ddiMap.put("+420", "420");
        ddiMap.put("+40", "40");
        ddiMap.put("+46", "46");
        ddiMap.put("+41", "41");
    }

    // Método para inicializar os formatos de telefone de cada país
    private void initPhoneFormats() {
        phoneFormats = new HashMap<>();

        // América do Sul
        phoneFormats.put("55", new PhoneFormatInfo(11, "XX XXXXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("54", new PhoneFormatInfo(10, "XX XXXX-XXXX", "XX XXXX-XXXX"));
        phoneFormats.put("591", new PhoneFormatInfo(8, "X XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("56", new PhoneFormatInfo(9, "X XXXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("57", new PhoneFormatInfo(10, "XXX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("593", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("592", new PhoneFormatInfo(7, "XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("595", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("51", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("597", new PhoneFormatInfo(7, "XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("598", new PhoneFormatInfo(8, "X XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("58", new PhoneFormatInfo(10, "XXX-XXX-XXXX", "XX XXXXX-XXXX"));

        // América do Norte
        phoneFormats.put("1", new PhoneFormatInfo(10, "(XXX) XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("52", new PhoneFormatInfo(10, "XX XXXX-XXXX", "XX XXXXX-XXXX"));

        // Europa
        phoneFormats.put("49", new PhoneFormatInfo(10, "XXX XXXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("43", new PhoneFormatInfo(10, "XXX XXXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("32", new PhoneFormatInfo(9, "XXX XX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("359", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("385", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("45", new PhoneFormatInfo(8, "XX XX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("421", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("386", new PhoneFormatInfo(8, "XX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("34", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("372", new PhoneFormatInfo(8, "XXXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("358", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("33", new PhoneFormatInfo(9, "X XX XX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("30", new PhoneFormatInfo(10, "XXX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("36", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("353", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("39", new PhoneFormatInfo(10, "XXX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("371", new PhoneFormatInfo(8, "XX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("370", new PhoneFormatInfo(8, "XXX-XXXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("352", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("356", new PhoneFormatInfo(8, "XXXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("31", new PhoneFormatInfo(9, "X XX XX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("48", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("351", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("44", new PhoneFormatInfo(10, "XXXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("420", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("40", new PhoneFormatInfo(9, "XXX XXX-XXX", "XX XXXXX-XXXX"));
        phoneFormats.put("46", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));
        phoneFormats.put("41", new PhoneFormatInfo(9, "XX XXX-XXXX", "XX XXXXX-XXXX"));

        // Completar mais formatos de outros países conforme necessário...
    }

    // Método para validar o número de telefone de acordo com o país
    private boolean isValidPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        PhoneFormatInfo formatInfo = phoneFormats.get(countryCode);
        if (formatInfo != null) {
            // Validação básica de comprimento
            return phoneNumber.length() == formatInfo.getLength();
        }

        // Se não temos info específica, usamos validação genérica do Android
        return PhoneNumberUtils.isGlobalPhoneNumber("+" + countryCode + phoneNumber);
    }

    // Método para formatar o número de telefone de acordo com o país
    private String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        PhoneFormatInfo formatInfo = phoneFormats.get(countryCode);
        if (formatInfo != null) {
            return applyFormat(phoneNumber, formatInfo.getFormat());
        }

        // Se não temos formato específico, retorna o número como está
        return phoneNumber;
    }

    // Aplica o formato ao número de telefone
    private String applyFormat(String phoneNumber, String format) {
        if (phoneNumber == null || phoneNumber.isEmpty() || format == null || format.isEmpty()) {
            return phoneNumber;
        }

        StringBuilder result = new StringBuilder();
        int phoneIndex = 0;

        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == 'X') {
                if (phoneIndex < phoneNumber.length()) {
                    result.append(phoneNumber.charAt(phoneIndex));
                    phoneIndex++;
                }
            } else {
                result.append(format.charAt(i));
            }
        }

        return result.toString();
    }

    // Método para obter o ID do usuário
    private int getId() {
        if (usuario != null) {
            return usuario.getId();
        }
        return -1; // Valor padrão caso o usuário não esteja disponível
    }

    private void enviarParaServidor(String dadosCriptografados) {
        String url = ""; // URL da API

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("API", "Resposta: " + response);
                },
                error -> {
                    Log.e("API", "Erro: ", error);
                }
        ) {
            @Override
            public byte[] getBody() {
                return dadosCriptografados.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json"; // Envio do JSON
            }
        };

        queue.add(stringRequest);
    }

    // Classe auxiliar para armazenar informações de formato de telefone
    private static class PhoneFormatInfo {
        private int length;
        private String format;
        private String example;

        public PhoneFormatInfo(int length, String format, String example) {
            this.length = length;
            this.format = format;
            this.example = example;
        }

        public int getLength() {
            return length;
        }

        public String getFormat() {
            return format;
        }

        public String getExample() {
            return example;
        }
    }
}