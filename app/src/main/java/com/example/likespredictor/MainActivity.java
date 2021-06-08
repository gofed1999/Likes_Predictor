package com.example.likespredictor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.exceptions.IGLoginException;
import com.github.instagram4j.instagram4j.models.user.Profile;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity
{
    private EditText editText;
    private EditText editText2;
    private Button button;
    List<Profile> result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.editTextTextEmailAddress);
        editText2 = (EditText) findViewById(R.id.editTextTextPassword);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this::onClick);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(View v)
    {

            try {
                IGClient client = IGClient.builder()
                        .username(String.valueOf(editText.getText()))
                        .password(String.valueOf(editText2.getText()))
                        .login();

                result = client.actions().users().findByUsername(String.valueOf(editText.getText()))
                        .thenApply(userAction -> userAction.followersFeed().stream()
                                .flatMap(feedUsersResponse -> feedUsersResponse.getUsers().stream()).collect(Collectors.toList())
                        ).join();

            } catch (IGLoginException e) {
                e.printStackTrace();
            }


        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("sub", result.size());

        startActivity(intent);
    }
}