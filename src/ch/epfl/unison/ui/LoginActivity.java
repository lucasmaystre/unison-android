package ch.epfl.unison.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;

public class LoginActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.LoginActivity";

    private Button loginBtn;
    private TextView signupTxt;

    private EditText email;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);

        this.email = (EditText) findViewById(R.id.email);
        this.password = (EditText) findViewById(R.id.password);

        this.signupTxt = (TextView) findViewById(R.id.signupTxt);
        this.signupTxt.setText(Html.fromHtml("New to GroupStreamer? <a href=\"#\">Sign up</a>."));
        this.signupTxt.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://groupstreamer.com/m/signup"));
                startActivity(browserIntent);
            }

        });

        this.loginBtn = (Button) findViewById(R.id.loginBtn);
        this.loginBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final String email = LoginActivity.this.email.getText().toString();
                final String password = LoginActivity.this.password.getText().toString();
                LoginActivity.this.login(email, password);
            }
        });

        // Initialize the AppData instance.
        AppData.getInstance(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.getBoolean("logout")) {
            // Remove e-mail and password from the preferences.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("email");
            editor.remove("password");
            editor.remove("uid");
            editor.remove("lastupdate");
            editor.commit();

            // Truncate the library.
            this.startService(new Intent(LibraryService.ACTION_TRUNCATE));

        } else if (extras!= null) {
            String email = extras.getString("email");
            String password = extras.getString("password");
            if (email != null && password != null) {
                // Login information is in the intent.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("email", email);
                editor.putString("password", password);
                editor.remove("uid");
                editor.remove("lastupdate");
                editor.commit();

                // Truncate the library. You never know.
                this.startService(new Intent(LibraryService.ACTION_TRUNCATE));
            }
        }

        // Try to login from the saved preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString("email", null);
        String password = prefs.getString("password", null);

        if (email != null && password != null) {
            this.login(email, password);
        }

        this.fillEmailPassword();
    }

    private void fillEmailPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.email.setText(prefs.getString("email", null));
        this.password.setText(prefs.getString("password", null));
    }

    private void storeInfo(String email, String password, String nickname, Long uid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("email", email);
        editor.putString("password", password);
        editor.putString("nickname", nickname);
        editor.putLong("uid", uid != null ? uid : -1);
        editor.commit();
    }

    private void nextActivity(JsonStruct.User user) {
        if (user.rid != null) {
            // Directly go into room.
            this.startActivity(new Intent(this, MainActivity.class)
                    .putExtra("rid", user.rid));
        } else {
            // Display list of rooms.
            this.startActivity(new Intent(this, RoomsActivity.class));
        }
    }

    private void login(final String email, final String password) {
        final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, null, "Signing in...");
        UnisonAPI api = new UnisonAPI(email, password);
        api.login(new UnisonAPI.Handler<JsonStruct.User>() {

            public void callback(JsonStruct.User user) {
                LoginActivity.this.storeInfo(email, password, user.nickname, user.uid);
                LoginActivity.this.nextActivity(user);
                dialog.dismiss();
            }

            public void onError(Error error) {
                Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
    }
}
