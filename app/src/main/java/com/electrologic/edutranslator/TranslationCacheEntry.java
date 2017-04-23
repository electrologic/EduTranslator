package com.electrologic.edutranslator;


public class TranslationCacheEntry
{
    public String text;
    public String langPare;
    public String translatorJsonResult;
    public String dictionaryJsonResult;

    public TranslationCacheEntry(String text, String langPare, String translationJsonResult, String dictionaryJsonResult)
    {
        this.text = text;
        this.langPare = langPare;
        this.translatorJsonResult = translationJsonResult;
        this.dictionaryJsonResult = dictionaryJsonResult;
    }
}
