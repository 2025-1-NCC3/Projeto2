package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable; // Para TextWatcher
import android.text.TextWatcher; // Para TextWatcher
// import android.telephony.PhoneNumberUtils; // Pode ser útil, mas sua validação customizada já existe
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button; // Usando android.widget.Button
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Removidos Volley, JSON e CryptoUtils por enquanto
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Number extends AppCompatActivity {

    private static final String TAG = "NumberActivity";
    private Spinner spinnerDDI;
    private EditText editPhone;
    private MaterialButton buttonNext, buttonBack; // Usando MaterialButton conforme seu XML anterior
    private String selectedDDIValue; // Armazena apenas os dígitos do DDI (ex: "55")
    private Usuario usuario;
    private LinkedHashMap<String, String> ddiMap; // Key: "+55 (Brasil)", Value: "55"
    private Map<String, PhoneFormatInfo> phoneFormats;
    private boolean isFormattingPhone = false; // Flag para TextWatcher de formatação

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.number); // Certifique-se que R.layout.number é seu layout correto

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade anterior
        // Usando a chave consistente "usuario_parcial"
        if (getIntent().hasExtra("usuario_parcial")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario_parcial");
        }

        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados de cadastro incompletos. Reiniciando.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Name.class); // Ou sua primeira Activity de cadastro
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        spinnerDDI = findViewById(R.id.spinnerDDI);
        editPhone = findViewById(R.id.editPhone);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        initDDIMap();
        initPhoneFormats();

        List<String> ddiDisplayList = new ArrayList<>(ddiMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ddiDisplayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDDI.setAdapter(adapter);

        buttonNext.setEnabled(false); // Botão começa desabilitado

        int defaultPosition = ddiDisplayList.indexOf("+55 (Brasil)"); // Ajuste se o texto de display for diferente
        if (defaultPosition >= 0) {
            spinnerDDI.setSelection(defaultPosition);
            selectedDDIValue = ddiMap.get(ddiDisplayList.get(defaultPosition)); // Pega o valor "55"
        } else if (!ddiDisplayList.isEmpty()){
            spinnerDDI.setSelection(0); // Seleciona o primeiro se Brasil não for encontrado
            selectedDDIValue = ddiMap.get(ddiDisplayList.get(0));
        }

        spinnerDDI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String ddiDisplayKey = (String) parent.getItemAtPosition(position);
                selectedDDIValue = ddiMap.get(ddiDisplayKey); // Pega o valor numérico do DDI (ex: "55")

                PhoneFormatInfo formatInfo = phoneFormats.get(selectedDDIValue);
                if (formatInfo != null) {
                    editPhone.setHint(formatInfo.getExample());
                } else {
                    editPhone.setHint("Número de telefone");
                }
                // Limpa o campo de telefone e revalida para habilitar/desabilitar o botão
                editPhone.setText("");
                checkInputsForButtonState();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDDIValue = null;
                editPhone.setHint("Número de telefone");
                checkInputsForButtonState();
            }
        });

        editPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // A formatação em tempo real pode ser complexa e variar muito.
                // Por ora, focaremos na validação e formatação ao confirmar.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingPhone) return; // Evita loop infinito se a formatação mudar o texto

                String phoneClean = s.toString().replaceAll("[^0-9]", "");
                PhoneFormatInfo formatInfo = phoneFormats.get(selectedDDIValue);
                String formatted = phoneClean; // Valor padrão se não houver formatação

                if (formatInfo != null) {
                    // Aplica a formatação apenas para exibição, mas valida o número limpo
                    formatted = applyFormat(phoneClean, formatInfo.getFormat());
                }

                isFormattingPhone = true;
                editPhone.setText(formatted);
                editPhone.setSelection(formatted.length());
                isFormattingPhone = false;

                checkInputsForButtonState();
            }
        });


        buttonNext.setOnClickListener(v -> {
            String phoneInput = editPhone.getText().toString();
            String phoneClean = phoneInput.replaceAll("[^0-9]", ""); // Números puros

            if (selectedDDIValue == null || selectedDDIValue.isEmpty()) {
                Toast.makeText(Number.this, "Selecione um DDI.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phoneClean.isEmpty()) {
                editPhone.setError("Digite o número de telefone!");
                editPhone.requestFocus();
                return;
            }

            if (!isValidPhoneNumber(phoneClean, selectedDDIValue)) {
                editPhone.setError("Número de telefone inválido para " + spinnerDDI.getSelectedItem().toString());
                editPhone.requestFocus();
                return;
            }

            // Salva o número de telefone completo no formato E.164 (ex: +5511987654321)
            // O servidor espera um campo "phone"
            String fullPhoneNumberE164 = "+" + selectedDDIValue + phoneClean;
            usuario.setPhone(fullPhoneNumberE164); // Assumindo que você tem setPhone em Usuario.java
            // ou usuario.setNumber(fullPhoneNumberE164);

            // NENHUMA CRIPTOGRAFIA OU CHAMADA AO SERVIDOR AQUI

            // Passa o objeto Usuario atualizado para a próxima atividade (Password.class)
            Intent intent = new Intent(Number.this, Password.class);
            intent.putExtra("usuario_parcial", usuario);
            startActivity(intent);
        });

        buttonBack.setOnClickListener(v -> {
            finish(); // Volta para a Activity anterior (Email.java)
        });
    }

    private void checkInputsForButtonState() {
        String phoneClean = editPhone.getText().toString().replaceAll("[^0-9]", "");
        boolean isDDISelected = selectedDDIValue != null && !selectedDDIValue.isEmpty();

        // Habilita o botão se um DDI estiver selecionado e o telefone tiver uma validação básica
        // (aqui, apenas se não estiver vazio, a validação completa é no clique)
        buttonNext.setEnabled(isDDISelected && !phoneClean.isEmpty());
    }

    private void initDDIMap() {
        ddiMap = new LinkedHashMap<>(); // Usar LinkedHashMap para manter a ordem de inserção
        ddiMap.put("+55", "55");
        ddiMap.put("+1", "1");
        ddiMap.put("+44", "44");
        ddiMap.put("+351", "351");
        ddiMap.put("+34", "34");
        ddiMap.put("+49", "49");
        ddiMap.put("+33", "33");
        ddiMap.put("+39", "39");
        ddiMap.put("+54", "54");
    }

    private void initPhoneFormats() {
        phoneFormats = new HashMap<>();
        // Exemplos: (Comprimento dos dígitos, String de formato para applyFormat, Exemplo para hint)
        phoneFormats.put("55", new PhoneFormatInfo(11, "(XX) XXXXX-XXXX", "(11) 98765-4321")); // Celular SP
        phoneFormats.put("1", new PhoneFormatInfo(10, "(XXX) XXX-XXXX", "(555) 123-4567"));   // EUA
        phoneFormats.put("44", new PhoneFormatInfo(10, "XXXX XXXXXX", "07123 456789")); // UK Mobile
        // Adicione mais formatos conforme sua lista
    }

    private boolean isValidPhoneNumber(String phoneCleanDigits, String ddiValue) {
        if (phoneCleanDigits == null || phoneCleanDigits.isEmpty() || ddiValue == null) {
            return false;
        }
        PhoneFormatInfo formatInfo = phoneFormats.get(ddiValue);
        if (formatInfo != null) {
            // Validação primária pelo comprimento esperado dos dígitos
            return phoneCleanDigits.length() == formatInfo.getLength();
        }
        // Fallback para uma validação genérica se não houver formato específico (pode não ser muito precisa)
        // return PhoneNumberUtils.isGlobalPhoneNumber("+" + ddiValue + phoneCleanDigits);
        return phoneCleanDigits.length() >= 7 && phoneCleanDigits.length() <= 15; // Validação de comprimento genérico
    }

    private String formatPhoneNumber(String phoneCleanDigits, String ddiValue) {
        if (phoneCleanDigits == null || phoneCleanDigits.isEmpty() || ddiValue == null) {
            return phoneCleanDigits;
        }
        PhoneFormatInfo formatInfo = phoneFormats.get(ddiValue);
        if (formatInfo != null) {
            return applyFormat(phoneCleanDigits, formatInfo.getFormat());
        }
        return phoneCleanDigits; // Retorna limpo se não houver formato
    }

    private String applyFormat(String phoneNumberDigits, String formatPattern) {
        if (phoneNumberDigits == null || phoneNumberDigits.isEmpty() || formatPattern == null || formatPattern.isEmpty()) {
            return phoneNumberDigits;
        }
        StringBuilder result = new StringBuilder();
        int phoneIndex = 0;
        for (int i = 0; i < formatPattern.length() && phoneIndex < phoneNumberDigits.length(); i++) {
            char formatChar = formatPattern.charAt(i);
            if (formatChar == 'X') {
                result.append(phoneNumberDigits.charAt(phoneIndex));
                phoneIndex++;
            } else {
                result.append(formatChar);
            }
        }
        // Se sobrourem dígitos no número que não couberam no formato, pode adicionar ao final
        // ou truncar, dependendo do comportamento desejado. Por simplicidade, aqui não adiciona.
        return result.toString();
    }

    // O método getId() não é necessário aqui.
    // O método enviarParaServidor() foi REMOVIDO.

    private static class PhoneFormatInfo {
        private int length; // Comprimento esperado dos DÍGITOS do número (sem DDI)
        private String format; // Padrão de formatação com 'X' para dígitos
        private String example; // Exemplo para o hint

        public PhoneFormatInfo(int length, String format, String example) {
            this.length = length;
            this.format = format;
            this.example = example;
        }
        public int getLength() { return length; }
        public String getFormat() { return format; }
        public String getExample() { return example; }
    }
}