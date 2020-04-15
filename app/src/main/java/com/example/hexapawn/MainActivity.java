package com.example.hexapawn;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    MyView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(R.id.myView);
    }
    public void onNewGameClick(View v) {
        view.newGame(); // запустить игру заново
        setContentView(R.layout.activity_main);
    }
}
