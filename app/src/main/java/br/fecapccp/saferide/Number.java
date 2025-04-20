package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
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

import java.util.HashMap;

import br.fecapccp.saferide.security.CryptoUtils;

public class Number extends AppCompatActivity {

    private Spinner spinnerDDI;
    private EditText editPhone;
    private Button buttonNext, buttonBack;
    private String selectedDDI;
    private Usuario usuario;

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

        // Mapa com DDIs dos países
        final HashMap<String, String> countryCodes = new HashMap<>();
        countryCodes.put("BRA (+55)", "55");
        countryCodes.put("EUA (+1)", "1");
        countryCodes.put("ARG (+54)", "54");
        countryCodes.put("PT (+351)", "351");
        countryCodes.put("UK (+44)", "44");

        // Criando um array com os nomes dos países
        String[] countryArray = countryCodes.keySet().toArray(new String[0]);

        // Criando um Adapter para o Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countryArray);
        spinnerDDI.setAdapter(adapter);

        // Listener para capturar o DDI selecionado
        spinnerDDI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String country = (String) parent.getItemAtPosition(position);
                selectedDDI = countryCodes.get(country);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDDI = "55"; // Padrão: Brasil
            }
        });

        // Listener para o botão "Próximo"
        buttonNext.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            String fullPhoneNumber = "+" + selectedDDI + phone;

            if (phone.isEmpty()) {
                Toast.makeText(Number.this, "Digite o número de telefone!", Toast.LENGTH_SHORT).show();
            } else if (!PhoneNumberUtils.isGlobalPhoneNumber(fullPhoneNumber)) {
                Toast.makeText(Number.this, "Número de telefone inválido!", Toast.LENGTH_SHORT).show();
            } else {
                // Atualiza o objeto Usuario com o número de telefone
                usuario.setNumber(fullPhoneNumber);

                // Criptografa o objeto Number
                String numeroCriptografado = CryptoUtils.encrypt(phone);

                // Passa o objeto Usuario para a próxima atividade (Password)
                Intent intent = new Intent(Number.this, Password.class);
                intent.putExtra("usuario", usuario);
                startActivity(intent);
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
}