package com.electrologic.edutranslator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

//import android.widget.TextView;


public class TranslationActivity extends AppCompatActivity
{
    String[] langStrings = {"Английский", "Немецкий", "Русский", "Французский"};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation);


        Spinner langSelectSpinner = (Spinner) findViewById(R.id.spinnerLangSelect);
        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langStrings);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        langSelectSpinner.setAdapter(adapter);

        Spinner langTranslateSelectSpinner = (Spinner) findViewById(R.id.spinnerTranslateLangSelect);
        // Применяем адаптер к элементу spinner
        langTranslateSelectSpinner.setAdapter(adapter);

        //TextView textViewLangSwap = (TextView) findViewById(R.id.textViewLangSwap);



    }
}
