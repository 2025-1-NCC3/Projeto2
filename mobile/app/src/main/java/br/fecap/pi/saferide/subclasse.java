package br.fecap.pi.saferide;

import android.app.DatePickerDialog; // Para o seletor de data
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
import android.widget.DatePicker; // Para o DatePickerDialog

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat; // Para formatar a data
import java.util.Calendar;    // Para o DatePickerDialog
import java.util.Locale;     // Para formatar a data

import br.fecap.pi.saferide.security.CryptoUtils; // Mantido se você decidir criptografar

public class subclasse extends AppCompatActivity {

    private static final String TAG = "SubclasseActivity";
    private EditText editCNH, editCPF, editBirthday; // Adicionado editBirthday
    private RadioGroup accountTypeRadioGroup; // Renomeado para clareza
    private RadioButton radioMotorista, radioPassageiro; // Renomeados para clareza
    private MaterialButton nextButton, backButton;
    private Usuario usuario;
    private boolean isUpdatingCPF = false;
    private boolean isUpdatingBirthday = false; // Flag para TextWatcher do Birthday

    private Calendar calendarBirthday; // Para armazenar a data selecionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subclasse);

        // Recupera o objeto Usuario
        if (getIntent().hasExtra("usuario_parcial")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario_parcial");
        }

        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados de cadastro incompletos. Reiniciando.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Name.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        editCNH = findViewById(R.id.editCNH);
        editCPF = findViewById(R.id.editCPF);
        editBirthday = findViewById(R.id.editBirthday); // Inicializa o novo EditText
        accountTypeRadioGroup = findViewById(R.id.accountTypeRadioGroup); // Use o ID correto do seu XML
        radioMotorista = findViewById(R.id.radioMotorista); // Use o ID correto do seu XML para "Motorista"
        radioPassageiro = findViewById(R.id.radioPassageiro); // Use o ID correto do seu XML para "Passageiro"
        nextButton = findViewById(R.id.next);
        backButton = findViewById(R.id.back);

        calendarBirthday = Calendar.getInstance(); // Inicializa o calendário

        // Estado inicial
        editCNH.setVisibility(View.GONE);
        editCPF.setVisibility(View.GONE);
        editBirthday.setVisibility(View.GONE); // Birthday também começa escondido
        nextButton.setEnabled(false);

        setupCPFTextWatcher();
        setupCNHTextWatcher();
        setupBirthdayTextWatcher(); // Novo TextWatcher para o campo de aniversário
        setupRadioGroupListener();
        setupClickListeners();
    }

    private void setupCPFTextWatcher() {
        editCPF.addTextChangedListener(new TextWatcher() {
            // ... (TextWatcher do CPF como antes, mas chama checkAllInputsForNextButton) ...
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingCPF) {
                    isUpdatingCPF = false;
                    return;
                }
                String str = s.toString().replaceAll("[^0-9]", "");
                if (str.length() > 11) str = str.substring(0, 11);
                StringBuilder formatted = new StringBuilder();
                if (str.length() > 0) formatted.append(str.substring(0, Math.min(3, str.length())));
                if (str.length() > 3) formatted.append(".").append(str.substring(3, Math.min(6, str.length())));
                if (str.length() > 6) formatted.append(".").append(str.substring(6, Math.min(9, str.length())));
                if (str.length() > 9) formatted.append("-").append(str.substring(9, Math.min(11, str.length())));
                isUpdatingCPF = true;
                editCPF.setText(formatted.toString());
                editCPF.setSelection(formatted.length());

                checkAndShowBirthdayField();
                checkAllInputsForNextButton();
            }
        });
    }

    private void setupCNHTextWatcher() {
        editCNH.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkAndShowBirthdayField();
                checkAllInputsForNextButton();
            }
        });
    }

    private void setupBirthdayTextWatcher() {
        // Formatação DD/MM/AAAA e validação básica
        editBirthday.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingBirthday) {
                    isUpdatingBirthday = false;
                    return;
                }
                String currentText = s.toString();
                String cleanText = currentText.replaceAll("[^0-9]", "");
                String formattedText = cleanText;

                if (cleanText.length() >= 2) {
                    formattedText = cleanText.substring(0, 2) + "/" + cleanText.substring(2);
                }
                if (cleanText.length() >= 4) {
                    formattedText = cleanText.substring(0, 2) + "/" + cleanText.substring(2, 4) + "/" + cleanText.substring(4);
                }
                // Limita ao formato DD/MM/AAAA (10 caracteres)
                if (formattedText.length() > 10) {
                    formattedText = formattedText.substring(0, 10);
                }

                isUpdatingBirthday = true;
                editBirthday.setText(formattedText);
                editBirthday.setSelection(formattedText.length());

                checkAllInputsForNextButton();
            }
        });

        // Abrir DatePickerDialog ao clicar no campo de data
        editBirthday.setOnClickListener(v -> showDatePickerDialog());
        editBirthday.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePickerDialog();
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendarBirthday.set(Calendar.YEAR, year);
            calendarBirthday.set(Calendar.MONTH, monthOfYear);
            calendarBirthday.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateBirthdayEditText();
            checkAllInputsForNextButton(); // Verifica se o botão pode ser habilitado
        };

        new DatePickerDialog(subclasse.this, dateSetListener,
                calendarBirthday.get(Calendar.YEAR),
                calendarBirthday.get(Calendar.MONTH),
                calendarBirthday.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateBirthdayEditText() {
        String myFormat = "dd/MM/yyyy"; // Formato brasileiro
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, new Locale("pt", "BR"));
        isUpdatingBirthday = true; // Previne loop do TextWatcher
        editBirthday.setText(sdf.format(calendarBirthday.getTime()));
        isUpdatingBirthday = false;
    }


    private void setupRadioGroupListener() {
        accountTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            editBirthday.setVisibility(View.GONE); // Esconde birthday ao trocar tipo de conta
            editBirthday.setText("");
            nextButton.setEnabled(false);

            if (checkedId == R.id.radioMotorista) { // Seu ID para Motorista
                editCNH.setVisibility(View.VISIBLE);
                editCPF.setVisibility(View.GONE);
                editCPF.setText("");
                usuario.setTipoConta("Motorista");
            } else if (checkedId == R.id.radioPassageiro) { // Seu ID para Passageiro
                editCPF.setVisibility(View.VISIBLE);
                editCNH.setVisibility(View.GONE);
                editCNH.setText("");
                usuario.setTipoConta("Passageiro");
            }
            checkAllInputsForNextButton();
        });
    }

    private void checkAndShowBirthdayField() {
        String tipoConta = usuario.getTipoConta();
        boolean showBirthday = false;

        if ("Motorista".equals(tipoConta)) {
            String cnh = editCNH.getText().toString().trim().replaceAll("[^0-9]", "");
            // Adicione sua lógica de validação de CNH aqui (ex: tamanho)
            if (!cnh.isEmpty() && cnh.length() >= 9) { // Exemplo: CNH válida se tiver pelo menos 9-11 dígitos
                showBirthday = true;
            }
        } else if ("Passageiro".equals(tipoConta)) {
            String cpf = editCPF.getText().toString().replaceAll("[^0-9]", "");
            if (cpf.length() == 11) { // CPF válido se tiver 11 dígitos
                showBirthday = true;
            }
        }
        editBirthday.setVisibility(showBirthday ? View.VISIBLE : View.GONE);
        if(!showBirthday) editBirthday.setText(""); // Limpa se for escondido
    }


    private void checkAllInputsForNextButton() {
        String tipoConta = usuario.getTipoConta();
        boolean documentValid = false;
        boolean birthdayValid = false;

        if ("Motorista".equals(tipoConta)) {
            String cnh = editCNH.getText().toString().replaceAll("[^0-9]", "");
            // Validação básica de CNH (ex: 9 a 11 dígitos)
            documentValid = !cnh.isEmpty() && (cnh.length() >=9 && cnh.length() <=11) ;
        } else if ("Passageiro".equals(tipoConta)) {
            String cpf = editCPF.getText().toString().replaceAll("[^0-9]", "");
            documentValid = cpf.length() == 11;
        }

        if (editBirthday.getVisibility() == View.VISIBLE) {
            String birthday = editBirthday.getText().toString().trim();
            // Validação básica de data (DD/MM/AAAA = 10 caracteres)
            // Uma validação mais robusta verificaria se a data é realmente válida.
            birthdayValid = birthday.length() == 10 && isValidDate(birthday);
        } else {
            // Se o campo de aniversário não está visível, não precisa ser válido para o botão.
            // Mas só habilitamos o botão se o documento estiver válido E o aniversário também (se visível)
            birthdayValid = !documentValid; // Se o documento não é válido, aniversário não precisa ser
        }

        // O botão "Próximo" só é habilitado se o documento estiver ok E (o aniversário estiver ok OU não for necessário ainda)
        nextButton.setEnabled(documentValid && (editBirthday.getVisibility() == View.GONE || birthdayValid) );
    }

    private boolean isValidDate(String dateStr) {
        if (dateStr == null || !dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false); // Não permite datas inválidas como 31/02/2023
        try {
            sdf.parse(dateStr);
            // TODO: Adicionar verificação se a data é razoável (ex: não no futuro, idade mínima)
            return true;
        } catch (java.text.ParseException e) {
            return false;
        }
    }


    private void setupClickListeners() {
        nextButton.setOnClickListener(v -> {
            String tipoConta = usuario.getTipoConta();
            if (tipoConta == null || tipoConta.isEmpty()){
                Toast.makeText(this, "Por favor, selecione se você é Motorista ou Passageiro.", Toast.LENGTH_SHORT).show();
                return;
            }

            String birthdayString = editBirthday.getText().toString().trim();
            if (editBirthday.getVisibility() == View.VISIBLE && (!isValidDate(birthdayString) || birthdayString.isEmpty())) {
                editBirthday.setError("Data de nascimento inválida.");
                editBirthday.requestFocus();
                return;
            }
            // Formatar para YYYY-MM-DD se o backend esperar assim
            String birthdayForServer = "";
            if (isValidDate(birthdayString)) {
                try {
                    SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    birthdayForServer = formatter.format(parser.parse(birthdayString));
                } catch (java.text.ParseException e) {
                    Log.e(TAG, "Erro ao formatar data de nascimento para o servidor", e);
                    // Usar um formato padrão ou tratar erro
                    birthdayForServer = "1900-01-01"; // Fallback
                }
            }

            usuario.setBirthday(birthdayForServer); // Salva a data formatada para o servidor

            if ("Motorista".equals(tipoConta)) {
                String cnhInput = editCNH.getText().toString().trim();
                String cnhFormatada = formatarCNH(cnhInput); // Apenas números
                if (cnhFormatada.isEmpty() || !(cnhFormatada.length() >= 9 && cnhFormatada.length() <=11)) {
                    editCNH.setError("CNH inválida.");
                    editCNH.requestFocus();
                    return;
                }
                // Decida se vai criptografar. Se sim, o servidor precisa descriptografar.
                // Por ora, vou salvar em texto plano no objeto Usuario.
                // A criptografia final, se necessária, ocorrerá na última tela de cadastro.
                usuario.setCnh(cnhFormatada); // Salva CNH limpa
                usuario.setCpf(null);
            } else if ("Passageiro".equals(tipoConta)) {
                String cpfInput = editCPF.getText().toString().trim();
                String cpfSemFormatacao = cpfInput.replaceAll("[^0-9]", "");
                if (cpfSemFormatacao.isEmpty() || cpfSemFormatacao.length() != 11) {
                    editCPF.setError("CPF inválido.");
                    editCPF.requestFocus();
                    return;
                }
                usuario.setCpf(cpfSemFormatacao); // Salva CPF limpo
                usuario.setCnh(null);
            }

            Intent intent = new Intent(subclasse.this, Genero.class); // Próxima tela é Genero
            intent.putExtra("usuario_parcial", usuario);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    private String formatarCNH(String cnh) {
        return cnh.replaceAll("[^0-9]", "");
    }

    // Método enviarParaServidor e getId() foram removidos
}