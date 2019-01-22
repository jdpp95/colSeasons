package com.example.android.colseasons;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CenterActivity extends AppCompatActivity implements View.OnClickListener{

    Button submit;
    EditText temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(this);
        temp = (EditText) findViewById(R.id.centerT);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.submit:
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("CENTER_TEMPERATURE", Double.parseDouble(temp.getText().toString()));
                startActivity(intent);
                break;
        }
    }
}
