package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void seguinte(View view){
        // Instanciando o objeto que receberá e armazenará as respostas
        RespostasFormulario respostas = new RespostasFormulario();

        // Referenciando os campos de resposta
        EditText campoResposta1 = findViewById(R.id.textResposta1);
        EditText campoResposta2 = findViewById(R.id.textResposta2);
        EditText campoResposta3 = findViewById(R.id.textResposta3);
        EditText campoResposta4 = findViewById(R.id.textResposta4);

        // Atribuindo os valores inseridos e passando para LowerCase
        String resp1 = campoResposta1.getText().toString().toLowerCase();
        String resp2 = campoResposta2.getText().toString().toLowerCase();
        String resp3 = campoResposta3.getText().toString().toLowerCase();
        String resp4 = campoResposta4.getText().toString().toLowerCase();

        // Associando as respostas à categoria e adicionando o valor inserido pelo objeto
        respostas.adicionarResposta(resp1, "extroversao");
        respostas.adicionarResposta(resp2, "extroversao");
        respostas.adicionarResposta(resp3, "extroversao");
        respostas.adicionarResposta(resp4, "extroversao");

        // Condição para que a pessoa não consiga analisar com resposta vazia
        if((resp1.isEmpty()) || (resp2.isEmpty()) || (resp3.isEmpty()) || (resp4.isEmpty())){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erro")
                    .setMessage("Todos os Campos Devem Ser Preenchidos")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {

            // Eviar os valores para a próxima tela e mudar de tela
            Intent mudarTela = new Intent(getApplicationContext(), Pagina2.class);
            mudarTela.putExtra("objeto", respostas);
            startActivity(mudarTela);
        }
    }
}