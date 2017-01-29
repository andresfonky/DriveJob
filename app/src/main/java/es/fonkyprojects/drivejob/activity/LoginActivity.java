package es.fonkyprojects.drivejob.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    private FirebaseAuth mAuth;

    //Views
    @Bind(R.id.input_email) EditText emailText;
    @Bind(R.id.input_password) EditText passwordText;
    @Bind(R.id.btn_login) Button btnLogin;
    @Bind(R.id.link_signup) TextView btnSignUp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        //Firebase
        mAuth = FirebaseAuth.getInstance();

        // Click listeners
        btnLogin.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
    }


    @Override
    public void onStart() {
        super.onStart();

        // Check auth on Activity start
        if (mAuth.getCurrentUser() != null) {
            onLoginSuccess(mAuth.getCurrentUser());
        }
    }

    private void login() {
        Log.d(TAG, "Login");

        //Check form
        if (!validateForm()) {
            return;
        }

        btnLogin.setEnabled(false);

        //Show progress bar
        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        //Get email and password
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        // Auth user and email
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signIn:onComplete:" + task.isSuccessful());
                        progressDialog.dismiss();
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            onLoginSuccess(task.getResult().getUser());
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //Go to SignUp Activity
    private void signUp() {
        startActivity(new Intent(LoginActivity.this, LoginSignupActivity.class));
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    //Login Success
    private void onLoginSuccess(FirebaseUser user) {
        // Go to MainActivity
        Log.e(TAG, "LOGIN");
        startActivity(new Intent(LoginActivity.this, MenuActivity.class));
        finish();
    }

    //Validate Form
    private boolean validateForm() {
        boolean valid = true;

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        //Email not empty and correct
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            emailText.setError(null);
        }

        //Password not empty and correct
        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            passwordText.setError("Between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            passwordText.setError(null);
        }

        //Return boolean
        return valid;
    }

    //OnClick
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_login) {
            login();
        } else if (i == R.id.link_signup) {
            signUp();
        }
    }
}
