package ch.epfl.unison.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import ch.epfl.unison.AppData;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class SignupActivity extends SherlockActivity {

    private static final String TAG = "ch.epfl.unison.SignupActivity";

    private TextView errors;
    private Button submitBtn;
    private TextView email;
    private TextView password;
    private TextView password2;
    private CheckBox tou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.signup);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.errors = (TextView) this.findViewById(R.id.errors);

        this.submitBtn = (Button) this.findViewById(R.id.submitBtn);
        this.submitBtn.setOnClickListener(new SubmitListener());

        this.email = (TextView) this.findViewById(R.id.email);
        this.password = (TextView) this.findViewById(R.id.password);
        this.password2 = (TextView) this.findViewById(R.id.password2);
        this.tou = (CheckBox) this.findViewById(R.id.touCbox);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // app icon in Action Bar clicked; go home
            startActivity(new Intent(this, LoginActivity.class)
                    .setAction(RoomsActivity.ACTION_LEAVE_ROOM)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        return true;
    }

    public void signup(final String email, final String password) {
        final ProgressDialog dialog = ProgressDialog.show(
                SignupActivity.this, null, "Signing up...");
        this.submitBtn.setEnabled(false);

        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.createUser(email, password, new UnisonAPI.Handler<JsonStruct.User>() {

            public void onError(UnisonAPI.Error error) {
                handleError(error);
                submitBtn.setEnabled(true);
                dialog.dismiss();
            }

            public void callback(JsonStruct.User struct) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)
                        .putExtra("email", email).putExtra("password", password));
                dialog.dismiss();
            }
        });
    }

    public void handleError(UnisonAPI.Error error) {
        if (error.hasJsonError()) {
            if (UnisonAPI.ErrorCodes.MISSING_FIELD == error.jsonError.error) {
                this.errors.setText("Fields are missing.");

            } else if (UnisonAPI.ErrorCodes.EXISTING_USER == error.jsonError.error) {
                this.errors.setText("User already exists (e-mail address in use).");

            } else if (UnisonAPI.ErrorCodes.INVALID_EMAIL == error.jsonError.error) {
                this.errors.setText("E-mail address is invalid.");

            } else if (UnisonAPI.ErrorCodes.INVALID_PASSWORD == error.jsonError.error) {
                this.errors.setText("Password is invalid.");
            }
            return;
        }
        // Last resort.
        this.errors.setText("Unable to create new user, please try again.");
    }

    private class SubmitListener implements OnClickListener {

        public void onClick(View v) {
            if (TextUtils.isEmpty(email.getText())
                    || TextUtils.isEmpty(password.getText())
                    || TextUtils.isEmpty(password2.getText())) {
                errors.setText("Please fill out all the fields.");
                errors.setVisibility(View.VISIBLE);
            } else if (!password.getText().toString().equals(password2.getText().toString())) {
                Log.d(TAG, password.getText() + " " + password2.getText());
                errors.setText("Passwords don't match.");
                errors.setVisibility(View.VISIBLE);
            } else if (password.length() < 6) {
                errors.setText("Password is too short.");
                errors.setVisibility(View.VISIBLE);
            } else if (!tou.isChecked()) {
                errors.setText("You need to agree to the terms of use.");
            } else {
                signup(email.getText().toString(), password.getText().toString());
            }
        }

    }

}
