package com.electrologic.edutranslator;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView textViewMailTo = (TextView) findViewById(R.id.tvAboutMailTo);

        String mailToString = "<a href=\"mailto:gasanov.sergei@gmail.com\">sergei.gasanov@gmail.com</a>";

        // проверка версии SDK, для того, чтобы не вызывать устаревший (deprecated) метод
        if (Build.VERSION.SDK_INT > 24)
            textViewMailTo.setText(Html.fromHtml(mailToString, Html.FROM_HTML_MODE_LEGACY));
        else
            textViewMailTo.setText(Html.fromHtml(mailToString));

        textViewMailTo.setMovementMethod(LinkMovementMethod.getInstance());

        ///
        TextView textViewCaption = (TextView) findViewById(R.id.tvAboutCaption);
        textViewCaption.setText(R.string.about_caption);

        TextView textView1 = (TextView) findViewById(R.id.tvAbout1);
        textView1.setText(R.string.about_1);

        TextView textView2 = (TextView) findViewById(R.id.tvAbout2);
        textView2.setText(R.string.about_2);

    }
}
