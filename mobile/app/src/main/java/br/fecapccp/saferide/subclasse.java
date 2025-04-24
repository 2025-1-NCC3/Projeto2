package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class subclasse extends AppCompatActivity {

    private EditText editCNH, editCPF;
    private RadioGroup radioGroup;
    private RadioButton radioPassenger, radioRider;
    private MaterialButton nextButton, backButton;
    private Usuario usuario;

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
                usuario.setCnh(cnh);
            } else if ("Passageiro".equals(usuario.getTipoConta())) {
                String cpf = editCPF.getText().toString().trim();
                if (cpf.isEmpty()) {
                    Toast.makeText(this, "Digite o CPF!", Toast.LENGTH_SHORT).show();
                    return;
                }
                usuario.setCpf(cpf);
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
}