package com.electrologic.edutranslator;

import java.util.ArrayList;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Кастомный класс адаптера (унаследованный от базового адаптера), отвечающего за заполнение
 * элементов ListView, которые являют собой историю переводов
 */
public class HistoryListAdapter extends BaseAdapter
{
    Context context;
    LayoutInflater lInflater;
    ArrayList<HistoryListEntry> entries;

    public HistoryListAdapter(Context context, ArrayList<HistoryListEntry> entries)
    {
        this.context = context;
        this.entries = entries;
        lInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // кол-во элементов
    @Override
    public int getCount()
    {
        return entries.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position)
    {
        return entries.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position)
    {
        return position;
    }

    // пункт списка
    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        // используем созданные, но не используемые view
        View view = convertView;

        if (view == null)
            view = lInflater.inflate(R.layout.history_list_item, parent, false);

        HistoryListEntry p = getEntry(position);

        // ссылка на TextView для вывода переводимого текста в данном конкретном элементе ListView.
        // Установка самого текста для отображения
        ((TextView) view.findViewById(R.id.tvText)).setText(p.text);

        // ссылка на TextView для вывода перевода в данном конкретном элементе ListView
        TextView textViewTranslator =  ((TextView) view.findViewById(R.id.tvTranslatorText));
        // проверка версии SDK, для того, чтобы не вызывать устаревший (deprecated) метод
        if (Build.VERSION.SDK_INT > 24)
            textViewTranslator.setText(Html.fromHtml(p.translatorText, Html.FROM_HTML_MODE_LEGACY));
        else
            textViewTranslator.setText(Html.fromHtml(p.translatorText));

        // ссылка на TextView для вывода пары языков ("язык текста"-"язык перевода" в данном конкретном
        // элементе ListView (например: "en-ru", или "fr-de" и т.д.)
        ((TextView) view.findViewById(R.id.tvLangPare)).setText(p.langPare);


        ImageView imageView = (ImageView) view.findViewById(R.id.ivHistoryItem);
/*
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (entries.get(position).isFavorites)
                {
                    entries.get(position).isFavorites = false;
                    ((ImageView)v).setImageResource(R.mipmap.ic_launcher_round);

                } else
                {
                    entries.get(position).isFavorites = true;
                    ((ImageView)v).setImageResource(R.mipmap.ic_launcher); // пиктограмма, если добавлен
                }
            }
        });

        // проверка, добавлен ли элемент в "избранное"
        if (p.isFavorites)
            imageView.setImageResource(R.mipmap.ic_launcher); // пиктограмма, если добавлен
        else
            imageView.setImageResource(R.mipmap.ic_launcher_round); // пиктограма, если не добавлен
*/

        return view;
    }

    // товар по позиции
    HistoryListEntry getEntry(int position)
    {
        return ((HistoryListEntry) getItem(position));
    }

/*
    // содержимое корзины
    ArrayList<ListEntry> getBox()
    {
        ArrayList<ListEntry> box = new ArrayList<ListEntry>();

        for (ListEntry p : entries)
        {
            // если в корзине
            if (p.box)
            box.add(p);
        }

        return box;
    }
*/
}

