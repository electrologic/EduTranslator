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


import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
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
import android.widget.Toast;

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
    final static String[] langStrings = {"Английский", "Немецкий", "Русский", "Французский"};
    final static String[] langTags = {"en", "de", "ru", "fr"};
    int savedSelectedLang;
    int savedSelectedLangTranslate;
    String translatorText = " ";
    String dictionaryText = " ";
    HttpPostTask httpPostTask;
    TranslationCache cache; // пул для кэширования результатов HTTP POST запросов
    final int TRANSLATION_CACHE_POOL_SIZE = 20; // максимальное кол-во кэшируемых результатов запросов

    TranslationCache history; // пул для сохранения истории переводов (истории HTTP POST запросов)
    final int HISTORY_POOL_SIZE = 20; // максимальное кол-во сохраненных в истории переводов

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
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation);

        // иницализируем парсер JSON, который будем получать в результате HTTP POST запросов
        jsonConverter = JsonConverter.init(1024, 2048);

        // инициализируем пул для кэширования результатов HTTP POST запросов
        cache = new TranslationCache(TRANSLATION_CACHE_POOL_SIZE);

        // инициализируем пул для сохранения истории переводов (истории HTTP POST запросов)
        history = new TranslationCache(HISTORY_POOL_SIZE);

        // инициализация выпадающего перечня (Spinner) для выбора языка вводимого текста
        //
        langSelectSpinner = (Spinner) findViewById(R.id.spinnerLangSelect);
        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        selectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langStrings);
        // Определяем разметку для использования при выборе элемента
        selectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        langSelectSpinner.setAdapter(selectAdapter);

        // обрботчик выбора (из выпадающего списка) языка вводимого текста
        langSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // если выбранный элемент перечня для языка вводимого текста не соответствует
                // тому, который был выбран до этого (т.е. если произошла смена)
                if (position != savedSelectedLang) {
                    // тэг для указания в запросе языка текста и языка перевода (например, "ru-en")
                    langPare = langTags[position] + "-" +
                               langTags[langTranslateSelectSpinner.getSelectedItemPosition()];

                    savedSelectedLang = position; // сохраняем выбор для следующей смены

                    // При смене языка вводимого текста HTTP post запрос не формируется, поскольку,
                    // если уже был введен какой-то текст, то все равно потребуется его ввести
                    // заново уже на новом выбранном языке
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // инициализация выпадающего перечня (Spinner) для выбора языка перевода вводимого текста
        //
        langTranslateSelectSpinner = (Spinner) findViewById(R.id.spinnerTranslateLangSelect);
        // Применяем адаптер к элементу spinner
        langTranslateSelectSpinner.setAdapter(selectAdapter);

        // обрботчик выбора (из выпадающего списка) языка перевода введенного текста
        langTranslateSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // если выбранный элемент перечня для языка перевода вводимого текста не
                // соответствует тому, который был выбран до этого (т.е. если произошла смена)
                if (position != savedSelectedLangTranslate)
                {
                    savedSelectedLangTranslate = position; // сохраняем выбор для следующей смены языка

                    // запускаем фоновую задачу с HTTP POST запросами для получения перевода, либо
                    // извлекаем данные из кэша, если аналогичные запросы уже имели место быть
                    runQueryOrGetCache();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // TextView со значком взаимного переключения языков: язык вводимого текста усанавливается
        // в соответствии с тем, языком который выбран для перевода, а язык перевода, наоборот, на
        // тот, который был выбран для ввода текста
        TextView textViewLangSwap = (TextView) findViewById(R.id.textViewLangSwap);
        // обработка нажатия для данного элемента
        textViewLangSwap.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // если выбранный язык вводимого текста и выбранный язык перевода не совпадают
                if (savedSelectedLang != savedSelectedLangTranslate)
                {
                    // устанавливаем выбранный элемент в выпадающем меню языков для вводимого текста
                    // таким, который был выбран в меню языков перевода, а выбранный элемент в
                    // выпадающем меню языков для перевода - наоборот
                    langSelectSpinner.setSelection(savedSelectedLangTranslate);
                    langTranslateSelectSpinner.setSelection(savedSelectedLang);

                    // сохраняем выбор
                    savedSelectedLang = langSelectSpinner.getSelectedItemPosition();
                    savedSelectedLangTranslate = langTranslateSelectSpinner.getSelectedItemPosition();

                    // тэг для указания в запросе языка текста и языка перевода (например, "ru-en")
                    langPare = langTags[savedSelectedLang] + "-" + langTags[savedSelectedLangTranslate];

                    Log.d("info", langPare);

                    // после смены языков, в область ввода теста помещаем текст перевода, который
                    // был получен от сервиса Yandex переводчик (или "пробел", если перевода нет)
                    editText.setText(JsonConverter.getFirstTranslation());
                }
            }
        });



        textViewTranslator = (TextView) findViewById(R.id.tvTranslator);
        textViewDictionary = (TextView) findViewById(R.id.tvDictionary);

        editText = (EditText) findViewById(R.id.editText);
        // обработчик изменения текста в EditText
        editText.addTextChangedListener(new TextWatcher()
        {
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
                // запускаем фоновую задачу с HTTP POST запросами для получения перевода, либо
                // извлекаем данные из кэша, если аналогичные запросы уже имели место быть
                runQueryOrGetCache();
            }
        });
        // подключаем к EditText обработчик для определения события появления или скрытия клавиатуры
        editText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if (isKeyboardShown(editText.getRootView())) // если клавиатуры выведена
                {
                    // установка для EditText рамки голубого цвета и толщиной линии в 2dp
                    editText.setBackgroundResource(R.drawable.border_selected);

                } else // иначе: если клавиатура скрыта
                {
                    // установка для EditText рамки серого цвета и толщиной линии в 1dp
                    editText.setBackgroundResource(R.drawable.border);

                    // ссылка на строку вводимого в TextEdit текста
                    inputText = editText.getText().toString();

                    // если введенный текст имеет место быть (т.е. содержит символы)
                    if (!(inputText.isEmpty()))
                    {
                        // тэг для указания в запросе языка текста и языка перевода (например, "ru-en")
                        langPare = langTags[langSelectSpinner.getSelectedItemPosition()] + "-" +
                                   langTags[langTranslateSelectSpinner.getSelectedItemPosition()];

                        // проходимся циклом по всем элементам истории переводов и проверяем был
                        // ли в ней уже такой перевод
                        for (int i = 0; i < history.getSize(); i++)
                        {
                            // если перевод с таким же, как сейчас вводимым текстом и языковой парой обнаружен
                            if (inputText.equals(history.get(i).text) && langPare.equals(history.get(i).langPare))
                            {
                                history.remove(i); // удаляем его из истории и ...
                                // ... добавляем новый в самое начало (на 0-ю позицию) истории
                                history.add(new TranslationCacheEntry(inputText, langPare,
                                                                      translatorText, dictionaryText));
                                return; // выходим из обработчика
                            }
                        }

                        // сюда доходим, если пул истории переводов не содержит аналогичный перевод
                        // добавляем новый перевод в историю
                        history.add(new TranslationCacheEntry(inputText, langPare,
                                                              translatorText, dictionaryText));
                    }
                }
            }
        });


        // кнопка "история/избранное". Обработчик нажатия.
        Button buttonHistoryFavorites = (Button) findViewById(R.id.btnHistoryFavorites);
        buttonHistoryFavorites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(TranslationActivity.this, HistoryFavoritesActivity.class);

                // передаем максимально допустимое кол-во в истории переводов
                intent.putExtra("historyMaxSize", Integer.toString(history.getMaxSize()));

                // передаем кол-во элементов в истории переводов
                intent.putExtra("historySize", Integer.toString(history.getSize()));

                // если в истории переводов присутствуют элементы, то требуется произвести их
                // передачу во 2-ю activity с помощью метода putExtra()
                if (history.getSize() != 0)
                {
                    // для каждого из переводов передаем: введенный текст, языковую пару, ответ
                    // Yandex переводчика и ответ Yandex словаря
                    for (int i = 0; i < history.getSize(); i++)
                    {
                        intent.putExtra("text_" + Integer.toString(i), history.get(i).text);
                        intent.putExtra("langPare_" + Integer.toString(i), history.get(i).langPare);
                        intent.putExtra("translatorText_" + Integer.toString(i), history.get(i).translatorJsonResult);
                        intent.putExtra("dictionaryText_" + Integer.toString(i), history.get(i).dictionaryJsonResult);

                        Log.d("info", history.get(i).text);
                        Log.d("info", history.get(i).langPare);
                        Log.d("info", history.get(i).translatorJsonResult);
                        Log.d("info", history.get(i).dictionaryJsonResult);
                    }
                }

                startActivity(intent);
            }
        });


        // устанавливаем выбор языка текста и языка перевода текста по-умолчанию:
        if (savedInstanceState == null)
        {
            langSelectSpinner.setSelection(0);
            savedSelectedLang = 0; // сохраняем индекс выбранного языка вводимого текста
            langTranslateSelectSpinner.setSelection(2);
            savedSelectedLangTranslate = 2; // сохраняем индекс выбранного языка перевода
        }

        // кнопка "о приложении"
        Button buttonAbout = (Button) findViewById(R.id.btnAbout);
        buttonAbout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // по нажатию на кнопку "о приложении", переключаемся на activity с информацией
                Intent intent = new Intent(TranslationActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

    }


    /**
     * Метод для определения выведена ли клавиатура на экран или нет. Основан на измерении области
     * вывода (которая меньше, когда клавиатура выводится).
     * @param rootView - ссылка на родительский элемент по отношению к EditText, в который будет
     *                   осуществляться ввод с помощью программной клавиатуры
     * @return false - клавиатура скрыта; true - клавиатура выведена
     */
    private boolean isKeyboardShown(View rootView)
    {
        final int softKeyboardHeight = 100; // минимальная высота клавиатуры (в DP)
        Rect r = new Rect();

        rootView.getWindowVisibleDisplayFrame(r); // в r размеры родительского (для EditText) эл-та
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        // получаем разницу (по высоте) родительского (для EditText) элемента и видимой части
        int heightDiff = rootView.getBottom() - r.bottom;

        // если разница превышает минимальную высоту клавиатуры (в DP), то значит клавиатуры
        // выводится - возвращаем true; если не превышает - клавиатура скрыта - возвращаем false
        return heightDiff > softKeyboardHeight * dm.density;
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

        // вывод сохраненных полей для обработанных ответов переводчика и словаря (с HTML тэгами) в
        // соответствующие TextView
        printQueryResults(savedInstanceState.getString(TRANSLATOR_TEXT_KEY),
                          savedInstanceState.getString(DICTIONARY_TEXT_KEY));
    }


    /**
     * Метод, запускающий асинхронно (в фоне) задачу с HTTP POST запросами к Yandex переводчику и
     * Yandex словарю, либо извлекающий данные из кэша, если аналогичные запросы ранее уже имели
     * место быть (чтобы повторно запросы не выполнять)
     */
    private void runQueryOrGetCache()
    {
        boolean isCacheFound = false;

        // если поле ввода текста для перевода не пустое
        if (!(editText.getText().toString().isEmpty()))
        {
            // тэг для указания в запросе языка текста и языка перевода (например, "ru-en")
            langPare = langTags[langSelectSpinner.getSelectedItemPosition()] + "-" +
                       langTags[langTranslateSelectSpinner.getSelectedItemPosition()];
            // непосредственно текст для перевода (содержимое TextView)
            inputText = editText.getText().toString();

            // ищем совпадение вводимого текста (и выбранной пары языков) с ранее
            // кэшированным результатом запросов к Yandex словарю и Yandex переводчику,
            // чтобы не делать лишние запросы, а достать результат перевода из кэша
            for (int i = 0; i < cache.getSize(); i++)
            {
                // если совпадение в кэше обнаружено (т.е. в кэше обнаружена информация по
                // проведенному ранее HTTP POST запросу, у которого входные параметры
                // (введенный текст для перевода и тэг с парой языков) совпадают с теми,
                // которые в данный момент имеют место быть)
                if (cache.get(i).text.equals(inputText) && cache.get(i).langPare.equals(langPare)) {
                    Log.d("info", "cache found");

                    // извлекаем из кэша результат запроса к Yandex переводчику в виде JSON
                    // строки и конвертируем в текст с HTML тэгами для отображения в TextView
                    String translatorText = JsonConverter.prepareTranslatorText(cache.get(i).translatorJsonResult);

                    // извлекаем из кэша результат запроса к Yandex словарю в виде JSON
                    // строки и конвертируем в текст с HTML тэгами для отображения в TextView
                    String dictionaryText = JsonConverter.prepareDictionaryText(cache.get(i).dictionaryJsonResult);

                    Log.d("info", cache.get(i).translatorJsonResult);
                    Log.d("info", cache.get(i).dictionaryJsonResult);

                    // вывод обработанного ответа переводчика и словаря (с HTML тэгами) в
                    // соответствующие TextView
                    printQueryResults(translatorText, dictionaryText);

                    // устанавливаем флаг, означающий, что инфорация из кэша извлечена и не
                    // потребуется делать новый HTTP POST запрос
                    isCacheFound = true;

                    break; // выходим из цикла сканирования, сохраненных в кэше, запросов
                }
            }

            // если данные из кэша не извлечены, запускаем Task для HTTP POST запроса
            if (!isCacheFound)
            {
                // если еще идет процесс http запроса для перевода с прошлого ввода символа
                if (httpPostTask != null)
                    httpPostTask.cancel(true); // отмена процесса http запроса

                // запустить новый процесс http запроса
                httpPostTask = new HttpPostTask();
                httpPostTask.execute();
            }

        } else // иначе: если символов в поле ввода текста нет
        {
            textViewTranslator.setText(" ");
            textViewDictionary.setText(" ");
        }
    }


    /**
     * Функция вывода результатов обработки ответа от Yandex словаря и Yandex переводчика в
     * соответствующие TextView
     *
     * @param translatorText - строка с результатом работы Yandex переводчика, который был преобразован
     *                         из JSON в текст с тэгами HTML, для дальнейшего отображения в TextView
     * @param dictionaryText - строка с результатом работы Yandex словаря, который был преобразован
     *                         из JSON в текст с тэгами HTML, для дальнейшего отображения в TextView
     */
    private void printQueryResults(String translatorText, String dictionaryText)
    {
        switch (translatorText)
        {
            case "JSON error":
            case "error code":
            case "no translations":
                textViewTranslator.setText(R.string.translatorDataError);
                break;

            default:

                // проверка версии SDK, для того, чтобы не вызывать устаревший (deprecated) метод
                if (Build.VERSION.SDK_INT > 24)
                    textViewTranslator.setText(Html.fromHtml(translatorText, Html.FROM_HTML_MODE_LEGACY));
                else
                    textViewTranslator.setText(Html.fromHtml(translatorText));

                break;
        }

        switch (dictionaryText)
        {
            case "no articles":
                textViewDictionary.setText(" ");
                break;

            case "JSON error":
                textViewDictionary.setText(R.string.dictionaryDataError);
                break;

            default:
                // проверка версии SDK, для того, чтобы не вызывать устаревший (deprecated) метод
                if (Build.VERSION.SDK_INT > 24)
                    textViewDictionary.setText(Html.fromHtml(dictionaryText, Html.FROM_HTML_MODE_LEGACY));
                else
                    textViewDictionary.setText(Html.fromHtml(dictionaryText));

                break;
        }
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

/*
                    Log.d("info", queryString);
                    Log.d("info", "&lang=" + langPare + "&text=" + URLEncoder.encode(inputText, "UTF-8"));
*/
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

                            // если это 2-я итерация цикла (т.е. 2-й по счету HTTP POST запрос из пары,
                            // т.к. 1-й запрос к Yandex словарю уже был, то проводим кэшировние результатов
                            if (i == 1)
                            {
                                // проводим кэширование - сохраняем результат данной пары HTTP POST запросов
                                // inputText - вводимый для перевода текст,
                                // langPare - тэг для указания языка текста и языка перевода (типа en-ru),
                                // httpJsonResult[0] - ответ от Yandex словаря,
                                // httpJsonResult[1] - ответ от Yandex переводчика
                                cache.add(new TranslationCacheEntry(inputText, langPare,
                                                                    httpJsonResult[1], httpJsonResult[0]));
                            }

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

            // конвертируем, принятую в результате HTTP POST запроса, JSON строку с ответом от
            // Yandex словаря в строку с HTML тэгами, пригодную для отображения с помощью TextView
            dictionaryText = JsonConverter.prepareDictionaryText(result[0]);

            printQueryResults(translatorText, dictionaryText);
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


