package com.example.webshopfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG=RegisterActivity.class.getName();
    private FirebaseAuth Auth;
    EditText userNameET;
    EditText userEmailET;
    EditText passwordET;
    EditText passwordConfirmET;
    Button signupButton;
    TextView loginRedirect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int secret_key = getIntent().getIntExtra("SECRET_KEY",0);

        if(secret_key!=88){
            finish();
        }
        Auth=FirebaseAuth.getInstance();

        userNameET=findViewById(R.id.registerUserNameET);
        userEmailET=findViewById(R.id.editTextEmail);
        passwordET=findViewById(R.id.editTextPassword);
        passwordConfirmET=findViewById(R.id.registerPasswordAgainET);
        signupButton=findViewById(R.id.loginButton);
        loginRedirect = findViewById(R.id.loginRedirect);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName=userNameET.getText().toString().trim();
                String email=userEmailET.getText().toString().trim();
                String password=passwordET.getText().toString().trim();
                String passwordConf=passwordConfirmET.getText().toString().trim();

                if (userName.isEmpty()){
                    userNameET.setError("Töltse ki a felhasználónév mezőt!");
                }
                if (email.isEmpty()){
                    userEmailET.setError("Töltse ki az e-mail mezőt!");
                }
                if( !Patterns.EMAIL_ADDRESS.matcher(email).matches()&& !email.isEmpty()){
                    userEmailET.setError("Nem megfelelő formátumú e-mail.");
                }

                if (password.isEmpty()){
                    passwordET.setError("Adjon meg jelszót!");
                }
                if(password.length()<6&& !password.isEmpty()){
                    passwordET.setError("Legalább 6 karakter hosszú jelszó szükséges!");
                }
                if(!passwordConf.equals(password)){
                    passwordConfirmET.setError("A két jelszó nem egyezik.");
                }else if(!userName.isEmpty()&&!email.isEmpty()&&!password.isEmpty()&&password.length()>=6&&Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this,"Sikeres regisztráció!",Toast.LENGTH_SHORT).show();
                                FirebaseUser user = Auth.getCurrentUser();
                                if (user != null) {
                                    FirestoreDao.getInstance().createUserDocument(
                                        user,
                                        userNameET.getText().toString().trim(),
                                        aVoid -> {
                                            // User document created
                                        },
                                        e -> {
                                            // Handle error
                                        }
                                    );
                                }
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            }else{
                                Toast.makeText(RegisterActivity.this,"Sikertelen regisztráció!"+ Objects.requireNonNull(task.getException()).getMessage(),Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                }

            }
        });
        loginRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this,MainActivity.class));
            }
        });






    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG,"onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG,"onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG,"onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG,"onResume");
    }
}