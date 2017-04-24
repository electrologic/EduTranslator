package com.electrologic.edutranslator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.widget.ListView;
import java.util.ArrayList;


public class HistoryFavoritesActivity extends AppCompatActivity
{
    private TranslationCache history; // пул истории переводов
    ListView historyListView;
    ArrayList<HistoryListEntry> entries;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_favorites);

        Button buttonHistoryFavorites = (Button) findViewById(R.id.btnReturnToTranslate);

        buttonHistoryFavorites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                finish();
/*
                Intent intent = new Intent(HistoryFavoritesActivity.this, TranslationActivity.class);
                startActivity(intent);
*/
            }
        });


        // получаем ссылку на intent, сформированный при переключении на данную activity
        // (история/избранное) из основной activity (перевод)
        Intent intent = getIntent();

        // максимальное кол-во элементов в истории переводов
        int historyMaxSize = Integer.parseInt(intent.getStringExtra("historyMaxSize"));

        // кол-во элементов в истории переводов
        int historySize = Integer.parseInt(intent.getStringExtra("historySize"));


        Log.d("info", "max history size = " + Integer.toString(historyMaxSize));
        Log.d("info", "history size = " + Integer.toString(historySize));


        if (historySize > 0) // если элементы в истории переводов присутствуют, извлекаем их из Extra
        {
            history = new TranslationCache(historyMaxSize);

            for (int i = 0; i < historySize; i++)
            {
                // начинаем заполнять пул с последнего переданного через Extra перевода, т.к.
                // добавление в пул производится всегда на нулевую позицию
                String strIndex = Integer.toString(historySize - i - 1);

                // заполняем пул истории просмотров для данной activity
                history.add(new TranslationCacheEntry(
                        intent.getStringExtra("text_" + strIndex),
                        intent.getStringExtra("langPare_" + strIndex),
                        intent.getStringExtra("translatorText_" + strIndex),
                        intent.getStringExtra("dictionaryText_" + strIndex)));
            }


            // получаем экземпляр элемента ListView
            historyListView = (ListView)findViewById(R.id.lvHistory);
            entries = new ArrayList<>();

            for (int i = 0; i < history.getSize(); i++)
            {
                Log.d("info", history.get(i).translatorJsonResult);

//                String translation = JsonConverter.prepareTranslatorText(history.get(i).translatorJsonResult);

                entries.add(new HistoryListEntry(history.get(i).text, history.get(i).translatorJsonResult, history.get(i).langPare));
            }

            HistoryListAdapter historyListAdapter = new HistoryListAdapter(this, entries);
            historyListView.setAdapter(historyListAdapter);

        }
    }
}
