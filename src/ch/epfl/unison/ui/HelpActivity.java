package ch.epfl.unison.ui;
import android.os.Bundle;
import android.webkit.WebView;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockActivity;

public class HelpActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.help);

        WebView webView = (WebView) this.findViewById(R.id.webview);
        webView.loadUrl("file:///android_asset/help.html");
    }
}
