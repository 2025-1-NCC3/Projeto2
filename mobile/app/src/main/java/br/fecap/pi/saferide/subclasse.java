package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import br.fecap.pi.saferide.R;
import br.fecap.pi.saferide.security.CryptoUtils;

public class subclasse extends AppCompatActivity {

    private EditText editCNH, editCPF;
    private RadioGroup radioGroup;
    private RadioButton radioPassenger, radioRider;
    private MaterialButton nextButton, backButton;
    private Usuario usuario;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subclasse);

        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        editCNH = findViewById(R.id.editCNH);
        editCPF = findViewById(R.id.editCPF);
        radioGroup = findViewById(R.id.genderRadioGroup);
        radioPassenger = findViewById(R.id.radioPassenger);
        radioRider = findViewById(R.id.radioRider);
        nextButton = findViewById(R.id.next);
        backButton = findViewById(R.id.back);

        editCNH.setVisibility(View.GONE);
        editCPF.setVisibility(View.GONE);

        // Adicionar TextWatcher para formatar o CPF em tempo real
        editCPF.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Não é necessário implementar
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Não é necessário implementar
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }

                String str = s.toString().replaceAll("[^0-9]", "");

                // Limitar a 11 dígitos
                if (str.length() > 11) {
                    str = str.substring(0, 11);
                }

                // Formatar CPF
                StringBuilder formatted = new StringBuilder();
                if (str.length() > 0) {
                    formatted.append(str.substring(0, Math.min(3, str.length())));
                    if (str.length() > 3) {
                        formatted.append(".");
                        formatted.append(str.substring(3, Math.min(6, str.length())));
                    }
                    if (str.length() > 6) {
                        formatted.append(".");
                        formatted.append(str.substring(6, Math.min(9, str.length())));
                    }
                    if (str.length() > 9) {
                        formatted.append("-");
                        formatted.append(str.substring(9, Math.min(11, str.length())));
                    }
                }

                isUpdating = true;
                editCPF.setText(formatted.toString());
                editCPF.setSelection(formatted.length());
            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPassenger) {
                editCNH.setVisibility(View.VISIBLE);
                editCPF.setVisibility(View.GONE);
                usuario.setTipoConta("Motorista");
            } else if (checkedId == R.id.radioRider) {
                editCPF.setVisibility(View.VISIBLE);
                editCNH.setVisibility(View.GONE);
                usuario.setTipoConta("Passageiro");
            }
        });

        nextButton.setOnClickListener(v -> {
            if ("Motorista".equals(usuario.getTipoConta())) {
                String cnh = editCNH.getText().toString().trim();
                if (cnh.isEmpty()) {
                    Toast.makeText(this, "Digite a CNH!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formata a CNH antes de salvar no objeto usuário
                String cnhFormatada = formatarCNH(cnh);
                usuario.setCnh(cnhFormatada);

                // Aplica a criptografia para a CNH formatada
                String encriptedCnh = CryptoUtils.encrypt(cnhFormatada);

                // Monta o JSON
                JSONObject json = new JSONObject();
                try{
                    json.put("id", getId());
                    json.put("cnh", encriptedCnh);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Envia para o servidor
                enviarParaServidor(json.toString());

            } else if ("Passageiro".equals(usuario.getTipoConta())) {
                String cpf = editCPF.getText().toString().trim();
                if (cpf.isEmpty()) {
                    Toast.makeText(this, "Digite o CPF!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Remove a formatação para salvar apenas os números
                String cpfSemFormatacao = cpf.replaceAll("[^0-9]", "");
                usuario.setCpf(cpfSemFormatacao);

                // Aplica a criptografia para o CPF
                String encriptedCpf = CryptoUtils.encrypt(cpfSemFormatacao);

                // Monta o JSON
                JSONObject json = new JSONObject();
                try{
                    json.put("id", getId());
                    json.put("cpf", encriptedCpf);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Envia para o servidor
                enviarParaServidor(json.toString());

            } else {
                Toast.makeText(this, "Selecione Motorista ou Passageiro", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continua o fluxo de cadastro para todos
            Intent intent = new Intent(subclasse.this, Genero.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Formata a CNH para o padrão correto
     * @param cnh CNH sem formatação
     * @return CNH formatada
     */
    private String formatarCNH(String cnh) {
        // Remove todos os caracteres não numéricos
        cnh = cnh.replaceAll("[^0-9]", "");

        // CNH tem 11 dígitos, mas não usa pontuação no padrão oficial
        // Apenas retorna os dígitos sem formatação adicional
        return cnh;
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
}