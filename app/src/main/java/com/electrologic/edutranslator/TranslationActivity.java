/**
 * Класс основного экрана (activity) приложения online переводчика EduTranslator.
 * Данное приложение задействует сервисы Yandex словаря и Yandex переводчика и выполнено в
 * целях обучения
 *
 * @author  Sergei Gasanov
 * @version 0.1
 * @email   gasanov.sergei@gmail.com
 * @date    21.04.2017
 */
package com.electrologic.edutranslator;


import android.os.Build;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.text.Html;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


/**
 * Класс основного экрана (activity) приложения
 */
public class TranslationActivity extends AppCompatActivity
{
    Spinner langSelectSpinner, langTranslateSelectSpinner;
    ArrayAdapter<String> selectAdapter;
    TextView textViewTranslator, textViewDictionary;
    EditText editText;
    String inputText; // текст, вводимый для перевода
    String langPare; // тэг с парой языков для POST запроса (вида "en-ru", "fr-de" или проч.)
    JsonConverter jsonConverter;
    static String[] langStrings = {"Английский", "Немецкий", "Русский", "Французский"};
    static String[] langTags = {"en", "de", "ru", "fr"};
    String translatorText = " ";
    String dictionaryText = " ";
    HttpPostTask httpPostTask;

    // ключ Yandex API переводчик
    static final String apiKeyTranslator = "trnsl.1.1.20170417T075753Z.bee3a11ff099758f.c21fd8ae398d3ff4a57a478779479fb5f0a91f27";
    // ключ Yandex API словарь
    static final String apiKeyDictionary = "dict.1.1.20170418T123056Z.e4a085bcb3c0610b.a8f05fbcd67b515ec0510500d6b6e17f353fdebd";
    // запрос к сервису Yandex API переводчик (без ключа)
    static final String postQueryTranslator = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=";
    // запрос к сервису Yandex API словарь (без ключа)
    static final String postQueryDictionary = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key=";

    static final String TRANSLATOR_TEXT_KEY = "translator_text";
    static final String DICTIONARY_TEXT_KEY = "dictionary_text";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation);

        // инициализация выпадающего перечня (Spinner) для выбора языка вводимого текста
        //
        langSelectSpinner = (Spinner) findViewById(R.id.spinnerLangSelect);
        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        selectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langStrings);
        // Определяем разметку для использования при выборе элемента
        selectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        langSelectSpinner.setAdapter(selectAdapter);


        // инициализация выпадающего перечня (Spinner) для выбора языка перевода вводимого текста
        //
        langTranslateSelectSpinner = (Spinner) findViewById(R.id.spinnerTranslateLangSelect);
        // Применяем адаптер к элементу spinner
        langTranslateSelectSpinner.setAdapter(selectAdapter);

        //TextView textViewLangSwap = (TextView) findViewById(R.id.textViewLangSwap);

        textViewTranslator = (TextView) findViewById(R.id.tvTranslator);
        textViewDictionary = (TextView) findViewById(R.id.tvDictionary);

        editText = (EditText) findViewById(R.id.editText);
        // обработчик изменения текста в EditText
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            /**
             * Метод обработки факта изменения во вводимом тексте виджета TextView
             * Вызывается автоматически при вводе или стирании очередного символа
             */
            @Override
            public void afterTextChanged(Editable s)
            {
                // если поле ввода текста для перевода не пустое
                if (!(editText.getText().toString().equals("")))
                {
                    // тэг для указания в запросе языка текста и языка перевода (например, "ru-en")
                    langPare = langTags[langSelectSpinner.getSelectedItemPosition()] + "-" +
                            langTags[langTranslateSelectSpinner.getSelectedItemPosition()];
                    // непосредственно текст для перевода (содержимое TextView)
                    inputText = editText.getText().toString();

                    // если еще идет процесс http запроса для перевода с прошлого ввода символа
                    if (httpPostTask != null)
                        httpPostTask.cancel(true); // отмена процесса http запроса

                    // запустить новый процесс http запроса
                    httpPostTask = new HttpPostTask();
                    httpPostTask.execute();

                } else // иначе: если символов в поле ввода текста нет
                {
                    textViewTranslator.setText(" ");
                    textViewDictionary.setText(" ");
                }
            }
        });


        // кнопка "история/избранное"
        Button buttonHistoryFavorites = (Button) findViewById(R.id.btnHistoryFavorites);
        buttonHistoryFavorites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(TranslationActivity.this, HistoryFavoritesActivity.class);
                startActivity(intent);
            }
        });


        if (savedInstanceState == null)
        {
            langSelectSpinner.setSelection(0);
            langTranslateSelectSpinner.setSelection(2);
        }


        // иницализируем парсер JSON, который будем получать в результате HTTP POST запросов
        jsonConverter = JsonConverter.init(1024, 2048);
/*
        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null)
        {
            // Restore value of members from saved state
            langSelectSpinner.setSelection(savedInstanceState.getInt(LANG_SELECT_CHOISE));
            langTranslateSelectSpinner.setSelection(savedInstanceState.getInt(LANG_TRANSLATE_SELECT_CHOISE));

            Log.d("info", "restore (create) instance state");
            Log.d("info", Integer.toString(savedInstanceState.getInt(LANG_SELECT_CHOISE)));
            Log.d("info", Integer.toString(savedInstanceState.getInt(LANG_TRANSLATE_SELECT_CHOISE)));

        } else
        {
            langSelectSpinner.setSelection(0);
            langTranslateSelectSpinner.setSelection(2);
        }
*/
    }


    /**
     * Метод сохранения состояния интерфейса для данной activity при завершении её работы.
     * Производит сохранение состояния выбранного языка текста, выбранного языка, на который текст
     * переводится.
     * @param savedInstanceState - объект Bundle, содержащий состояние пользовательского интерфейса
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putString(TRANSLATOR_TEXT_KEY, translatorText);
        savedInstanceState.putString(DICTIONARY_TEXT_KEY, dictionaryText);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        textViewSetHtmlText(textViewTranslator, savedInstanceState.getString(TRANSLATOR_TEXT_KEY));
        textViewSetHtmlText(textViewDictionary, savedInstanceState.getString(DICTIONARY_TEXT_KEY));
    }


    /**
     * Метод для вывода текста с тэгами HTML в заданный TextView
     * @param textView - виджет TextView, в который требуется вывести строку с HTML тэгами
     * @param htmlText - строка с текстом, содержащим HTML тэги
     */
    private void textViewSetHtmlText(TextView textView, String htmlText)
    {
        // проверка версии SDK, для того, чтобы не вызывать устаревший (deprecated) метод
        if (Build.VERSION.SDK_INT > 24)
            textView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        else
            textView.setText(Html.fromHtml(htmlText));
    }


    /**
     * Класс, реализующий выполнение HTTP POST запроса к удаленному сервису (запрос работает в фоне)
     */
    private class HttpPostTask extends AsyncTask<Void, Void, String[]>
    {
        private String httpJsonResult[];

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            Log.d("info: ", "on pre execute");

            httpJsonResult = new String[2];
        }


        @Override
        protected String[] doInBackground(Void... params)
        {
            HttpURLConnection urlConnection;
            BufferedReader reader;


            // цикл из 2-х итераций: 1-я (т.е. №0) - для Http Post запроса к Yandex словарю,
            // 2-я (т.е. №1) - для Http Post запроса к Yandex переводчику
            for (int i = 0; i < 2; i++)
            {
                // получаем данные с внешнего ресурса
                try
                {
                    String queryString;

                    if (i == 0) // формируем строку запроса для 1-й итерации - Yandex словарь
                        queryString = postQueryDictionary + apiKeyDictionary;
                    else // формируем строку запроса для 2-й итерации - Yandex переводчик
                        queryString = postQueryTranslator + apiKeyTranslator;

                    URL url = new URL(queryString);

                    urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);

                    DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());

                    // добавляем к запросу тэг с языками (текста и его перевода), а также сам текст для перевода
                    dataOutputStream.writeBytes("&lang=" + langPare + "&text=" + URLEncoder.encode(inputText, "UTF-8"));

                    Log.d("info", queryString);
                    Log.d("info", "&lang=" + "en-ru" + "&text=" + URLEncoder.encode(inputText, "UTF-8"));
                    Log.d("info", "response code: " + Integer.toString(urlConnection.getResponseCode()));


                    // разбираем код ответа на HTTP POST запрос
                    switch (urlConnection.getResponseCode()) {
                        case 200: // принят код корректного ответа на HTTP POST запрос

                            InputStream inputStream = urlConnection.getInputStream();
                            StringBuilder buffer = new StringBuilder();

                            reader = new BufferedReader(new InputStreamReader(inputStream));

                            String line;
                            while ((line = reader.readLine()) != null) {
                                buffer.append(line);
                            }

                            httpJsonResult[i] = buffer.toString();
                            break;

                        case 403: // принят код "invalid key" - ошибка ключа, полученного в кабинете разработчика

                            httpJsonResult[i] = "key error";
                            break;

                        default: // принят другой код ошибки доступа к сервису

                            httpJsonResult[i] = "service error";
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    httpJsonResult[i] = "error";
                }
            }

            return httpJsonResult;
        }


        @Override
        protected void onPostExecute(String result[])
        {
            super.onPostExecute(result);

            Log.d("info: ", result[0]);
            Log.d("info: ", result[1]);

            // конвертируем, принятую в результате HTTP POST запроса, JSON строку с ответом от
            // Yandex переводчика в строку с HTML тэгами, пригодную для отображения с помощью TextView
            translatorText = JsonConverter.prepareTranslatorText(result[1]);

            switch (translatorText)
            {
                case "JSON error":
                case "error code":
                case "no translations":
                    textViewTranslator.setText(R.string.translatorDataError);
                    break;

                default:
                    // вывод текста переводчика (с HTML тэгами) в соответствующий TextView
                    textViewSetHtmlText(textViewTranslator, translatorText);

                    break;
            }


            // конвертируем, принятую в результате HTTP POST запроса, JSON строку с ответом от
            // Yandex словаря в строку с HTML тэгами, пригодную для отображения с помощью TextView
            dictionaryText = JsonConverter.prepareDictionaryText(result[0]);

            switch (dictionaryText)
            {
                case "no articles":
                    textViewDictionary.setText(" ");
                    break;

                case "JSON error":
                    textViewDictionary.setText(R.string.dictionaryDataError);
                    break;

                default:
                    // вывод текста словаря (с HTML тэгами) в соответствующий TextView
                    textViewSetHtmlText(textViewDictionary, dictionaryText);
                    break;
            }
        }


        @Override
        protected void onCancelled()
        {
            super.onCancelled();

            //tvInfo.setText("Cancel");
            //Log.d(LOG_TAG, "Cancel");
        }
    }

}


