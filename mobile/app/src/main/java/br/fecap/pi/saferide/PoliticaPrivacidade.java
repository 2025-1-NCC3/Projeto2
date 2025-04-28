package br.fecap.pi.saferide;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import br.fecap.pi.saferide.R;

public class PoliticaPrivacidade extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_politica_privacidade);

        WebView webView = findViewById(R.id.webViewPolitica);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Carregar um arquivo local (coloque "politica_privacidade.html" na pasta assets)
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/politica_privacidade.html");
    }
}