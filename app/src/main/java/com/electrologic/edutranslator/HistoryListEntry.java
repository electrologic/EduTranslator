package com.electrologic.edutranslator;


public class HistoryListEntry
{
    public String text;
    public String langPare;
    public String translatorText;
    public boolean isFavorites;
    public boolean isHistory;

    public HistoryListEntry(String text, String translatorText, String langPare)
    {
        this.text = text;
        this.langPare = langPare;
        this.translatorText = translatorText;

        isFavorites = false;
        isHistory = false;
    }
}
