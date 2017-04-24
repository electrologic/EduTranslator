/**
 * Класс, содержащий методы для преобразования принятых данных в виде JSON строки в строку с
 * вариантами переводов и проч. с тегами html для последующего отображения с помощью TextView.
 * Методы работают с данными, полученными от Yandex словаря и от Yandex переводчика
 *
 * @author  Sergei Gasanov
 * @version 0.1
 * @email   gasanov.sergei@gmail.com
 * @date    21.04.2017
 */
package com.electrologic.edutranslator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;


/**
 * Класс, содержащий методы для преобразования принятых данных в виде JSON строки в строку с
 * вариантами переводов и проч. с тегами html для последующего отображения с помощью TextView
 */
public class JsonConverter
{
    private static JsonConverter instance; // ссылка для экземпляра класса
    private static Map<String, String> speechPartMap; // таблица (ключ-значение) с обозначениями частей речи
    private static StringBuffer dictionaryBuffer; // буфер с переводом Yandex словаря (+ html тэги)
    private static StringBuffer translatorBuffer; // буфер с переводом Yandex переводчика (+ html тэги)
    private static String firstTranslationText; // строка с вариантом текста от переводчика (без тэгов)


    /** Функция однократной инициализация по принципу singleton
     * @param initialDictionaryBufferSize - изначальный размер буфера, в который будет помещен
     *                                      результат парсинга данных, полученных от словаря
     * @param initialTranslatorBufferSize - изначальный размер буфера, в который будет помещен
     *                                      результат парсинга данных, полученных от переводчика
     * @return ссылка на экземпляр JsonConverter
     */
    public static synchronized JsonConverter init(int initialDictionaryBufferSize,
                                                  int initialTranslatorBufferSize)
    {
        if (instance == null)
        {
            instance = new JsonConverter();

            // инициализируем и заполняем таблицу с обозначениями частей речи (ключ - токен,
            // соответствующий типу (англ.), значение - обозначение части речи на русском)
            speechPartMap = new HashMap<>();
            speechPartMap.put("noun","сущ.");
            speechPartMap.put("verb","гл.");
            speechPartMap.put("adjective","прил.");
            speechPartMap.put("participle","прич.");
            speechPartMap.put("adverb","нареч.");
            speechPartMap.put("interjection","межд.");
            speechPartMap.put("pronoun","мест.");
            speechPartMap.put("conjunction","союз");
            speechPartMap.put("number","числ.");
            speechPartMap.put("article"," ");

            firstTranslationText = " ";

            // инициализация буфера словаря, в который будет помещаться результат парсинга JSON
            if (initialDictionaryBufferSize > 0)
                dictionaryBuffer = new StringBuffer(initialDictionaryBufferSize);
            else // иначе: если задана некорректая величина начальной емкости буфера
                dictionaryBuffer = new StringBuffer(2048);

            // инициализация буфера переводчика, в который будет помещаться результат парсинга JSON
            if (initialTranslatorBufferSize > 0)
                translatorBuffer = new StringBuffer(initialTranslatorBufferSize);
            else // иначе: если задана некорректая величина начальной емкости буфера
                translatorBuffer = new StringBuffer(2048);
        }

        return instance;
    }


    /**
     * Метод преобразования JSON строки, полученной от Yandex словаря в строку с html тэгами для
     * последующего её отображения в виджете TextView
     * @param inputText - JSON строка
     * @return 1) в случае корретной отработки: варианты перевода в виде строки с html тэгами
     *         2) в случае возникновения ошибки:
     *            строка "no articles" - словарные статьи не обнаружены
     *            строка "JSON error" - ошибка обнаружения JSON объекта
     */
    public static String prepareDictionaryText(String inputText)
    {
        JSONObject dataJsonObj;

        // очистка буфера, на случай, если функция уже вызывалась
        dictionaryBuffer.setLength(0);

        try // пробуем извлечь JSON массив словарных статей
        {
            dataJsonObj = new JSONObject(inputText);
            JSONArray def = dataJsonObj.getJSONArray("def");

            // если не обнаружено данных по словарным статям
            if (def.length() == 0)
                return "no articles";

            // в цикле перебираем словарные статьи
            for (int articleNumber = 0; articleNumber < def.length(); articleNumber++)
            {
                JSONObject dictArticle = def.getJSONObject(articleNumber); // извлекаем очередную статью

                // извлекаем английское название части речи (поле pos)
                String speechPart = dictArticle.getString("pos");

                // если данное значение присутствует в таблице speechPartMap в качестве ключа,
                // то помещаем русское название части речи (значение, соответствующее ключу)
                if (speechPartMap.containsKey(speechPart))
                {
                    // устанавливаем цвет текста для вывода названия части речи
                    dictionaryBuffer.append("<font color=\"blue\">");

                    // обрамляем название части речи тэгом <i></i> для отображения вывода курсивом
                    dictionaryBuffer.append("<i>");
                    // непосредственно название части речи получаем по ключу из speechPartMap
                    dictionaryBuffer.append(speechPartMap.get(speechPart));
                    // закрываем тэги для установки стиля отображения названия части речи
                    dictionaryBuffer.append("</i></font><br>");
                }


                JSONArray translations; // ссылка на JSON массив переводов

                try // пробуем извлечь JSON массив переводов
                {
                    translations = dictArticle.getJSONArray("tr");
                } catch (JSONException e)
                {
                    // JSON массив переводов не удалось извлечь (м/б отсутствует)
                    e.printStackTrace();
                    continue; // переход к следующей словарной статье (след. итерация цикла)
                }


                // если в JSON массиве отсутствуют элементы (т.е. вариантов перевода не
                // обнаружено - переход к следующей словарной статье
                if (translations.length() == 0)
                    continue; // переход к следующей словарной статье (след. итерация цикла)

                // в цикле перебираем варианты перевода для текущей словарной статьи
                for (int translationNumber = 0; translationNumber < translations.length(); translationNumber++)
                {
                    if (translations.length() > 1) // если кол-во переводов больше одного
                    {
                        // перед каждым переводом помещаем его номер
                        dictionaryBuffer.append(String.format(Locale.getDefault(), "%d. ", translationNumber + 1));

                    } else // иначе: если только один вариант перевода
                    {
                        // номер ему можно не добавлять
                        dictionaryBuffer.append("   ");
                    }

                    dictionaryBuffer.append("<font color=\"teal\">");

                    // выбираем элемент номер j в JSON массиве значений перевода (translations),
                    // чтобы из данного элемента извлеч поле text
                    dictionaryBuffer.append(translations.getJSONObject(translationNumber).getString("text"));


                    try // пробуем извлечь JSON массив синонимов для текущего перевода
                    {
                        JSONArray synonyms = translations.getJSONObject(translationNumber).getJSONArray("syn");

                        // если массив синонимов не пустой, т.е. синонимы для слова присутствуют
                        if (synonyms.length() > 0)
                        {
                            // последовательно печатаем синонимы следом за переводом
                            for (int synonymNumber = 0; synonymNumber < synonyms.length(); synonymNumber++)
                            {
                                dictionaryBuffer.append(", ");
                                dictionaryBuffer.append(synonyms.getJSONObject(synonymNumber).getString("text"));
                            }
                        }

                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }

                    dictionaryBuffer.append("</font><br>"); // новая строка


                    try // пробуем извлечь JSON массив значений (типа синонимиов, но на языке оригинала)
                    {
                        JSONArray means = translations.getJSONObject(translationNumber).getJSONArray("mean");

                        // если массив значений не пустой, т.е. значения для слова присутствуют
                        if (means.length() > 0)
                        {
                            // перечисление значений вместе со скобками выделено коричневым цветом
                            dictionaryBuffer.append("<font color=\"maroon\">(");

                            // последовательно печатаем значения
                            for (int k = 0; k < means.length(); k++)
                            {
                                if (k > 0) // перед 0-ым значением нет запятой (т.е. есть скобка)
                                    dictionaryBuffer.append(", ");

                                dictionaryBuffer.append(means.getJSONObject(k).getString("text"));
                            }

                            dictionaryBuffer.append(")</font>");
                        }

                        dictionaryBuffer.append("<br>"); // новая строка

                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }


                    try // пробуем извлечь JSON массив с примерами употребления (и их переводами)
                    {
                        JSONArray examples = translations.getJSONObject(translationNumber).getJSONArray("ex");

                        // если массив примеров не пустой, т.е. примеры употребления присутствуют
                        if (examples.length() > 0)
                        {
                            // перечисление значений вместе со скобками выделено сине-зеленым цветом
                            dictionaryBuffer.append("<font color=\"gray\"><i>");

                            // проходим циклом по массиву примеров употребления
                            for (int exampleNumber = 0; exampleNumber < examples.length(); exampleNumber++)
                            {
                                // текст очередного примера употребления
                                dictionaryBuffer.append(examples.getJSONObject(exampleNumber).getString("text"));

                                try // пробуем извлечь массив переводов для данного примера употребления
                                {
                                    JSONArray exampleTranslates = examples.getJSONObject(exampleNumber).getJSONArray("tr");

                                    // если данный момент не пустой (т.е. содержит элементы)
                                    if (exampleTranslates.length() > 0)
                                    {
                                        dictionaryBuffer.append(" - ");

                                        // на случай, если для данного примера употребления слова
                                        // имеется несколько переводов, то выводим их в цикле
                                        for (int i = 0; i < exampleTranslates.length(); i++)
                                        {
                                            if (i > 0) // запятую ставим начиная с 1-го эл-та (а не с 0-го)
                                                dictionaryBuffer.append(" ,");

                                            dictionaryBuffer.append(exampleTranslates.getJSONObject(i).getString("text"));
                                        }

                                        dictionaryBuffer.append("<br>"); // новая строка
                                    }

                                } catch (JSONException e)
                                {
                                    e.printStackTrace();
                                }

                            } // окончание итерации цикла перебора массива с примерами употребления

                            dictionaryBuffer.append("</i></font>");
                        }

                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }

                dictionaryBuffer.append("<br>"); // новая строка (разделитель)

            } // окончание итерации цикла обработки словарных статей

        } catch (JSONException e)
        {
            e.printStackTrace();
            return "JSON error";
        }

        return dictionaryBuffer.toString();
    }


    /**
     * Метод преобразования JSON строки, полученной от Yandex переводчика в строку с html тэгами для
     * последующего её отображения в виджете TextView
     * @param inputText - JSON строка
     * @return 1) в случае корретной отработки: варианты перевода в виде строки с html тэгами
     *         2) в случае возникновения ошибки:
     *            строка "error code" - принят код ошибки (т.е. код не равен 200)
     *            строка "no translations" - варианты перевода отсутствуют
     *            строка "JSON error" - ошибка обнаружения JSON объекта
     *
     */
    public static String prepareTranslatorText(String inputText)
    {
        JSONObject dataJsonObj;

        // очистка буфера, на случай, если функция уже вызывалась
        translatorBuffer.setLength(0);

        try // пробуем извлечь JSON массив с переводом
        {
            dataJsonObj = new JSONObject(inputText);

            // если код в структуре ответа не равен коду 200, значит имеет место быть ошибка
            if (!((dataJsonObj.getString("code")).equals("200")))
                return "error code";

            // извлекаем JSON массив переводов text
            JSONArray transText = dataJsonObj.getJSONArray("text");

            // если не обнаружено переводов (т.е. если их число нулевое)
            if (transText.length() == 0)
                return "no translations";

            // цикл перебора вариантов перевода (поскольку поле text - это массив строк)
            for (int i = 0; i < transText.length(); i++)
            {
                // проставляем индекс перевода, только если их (переводов) число > 1
                if (transText.length() > 1)
                {
                    translatorBuffer.append(Integer.toString(i));
                    translatorBuffer.append(" ");
                }

                translatorBuffer.append("<font color=\"black\">"); // тэг: текст перевода - черный
                translatorBuffer.append(transText.getString(i)); // помещаем текст перевода
                translatorBuffer.append("</font>");

                // переход на новую строку только если кол-во переводов больше одного и текущий (i)
                // перевод не последний из всех
                if (transText.length() > 1 && i < (transText.length() - 1))
                    translatorBuffer.append("<br>"); // переход на новую строку
            }

            // сохраняем самый первый (или, как правило, единственный) вариант перевода в чистом
            // виде (без html тэгов) т.к. он может потребоваться для ф-ии переключения языков
            firstTranslationText = transText.getString(0);

        } catch (JSONException e)
        {
            e.printStackTrace();
            return "JSON error";
        }

        return translatorBuffer.toString();
    }


    /**
     * Геттер для получения варианта перевода от сервиса Yandex переводчик в виде строки без
     * HTML тэгов
     * @return строка без HTML тэгов
     */
    public static String getFirstTranslation()
    {
        return firstTranslationText;
    }
}



