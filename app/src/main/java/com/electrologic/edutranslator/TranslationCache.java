package com.electrologic.edutranslator;

import java.util.ArrayList;

/**
 * Класс реализует пул фиксированной длины из элементов, которые представляют собой экземпляры
 * класса TranslationCacheEntry. Максимальный размер пула устанавливается при его инициализации и
 * в дальнейшем не меняется. При добавлении в пул нового элемента, он (элемент) становится на 0-ю
 * позицию, а все прежние элементы пула сдвигаются на 1 позицию (1 -> 2, 2 -> 3 и т.д.) После
 * заполнения пула полностью, по прежнему можно добавлять в него новые элементы, но при этом
 * последний сдвигаемый элемент покидает пул.
 *
 * @author  Sergei Gasanov
 * @version 0.1
 * @email   gasanov.sergei@gmail.com
 * @date    23.04.2017
 */
public class TranslationCache
{
    private ArrayList<TranslationCacheEntry> pool;
    private int maxLength;

    public TranslationCache(int maxLength)
    {
        if (maxLength > 0)
            this.maxLength = maxLength;
        else
            this.maxLength = 16;

        pool = new ArrayList<>(this.maxLength);
    }

    public void add(TranslationCacheEntry entry)
    {
        if (pool.size() < maxLength)
        {
            if (pool.size() > 0)
            {
                pool.add(pool.get(pool.size() - 1));

                if (pool.size() > 2)
                {
                    for (int i = pool.size() - 2; i > 0; i--)
                        pool.set(i, pool.get(i - 1));
                }

                pool.set(0, entry);

            } else
                pool.add(entry);

        } else
        if (pool.size() == maxLength)
        {
            for (int i = pool.size() - 1; i > 0; i--)
                pool.set(i, pool.get(i - 1));

            pool.set(0, entry);
        }
    }

    TranslationCacheEntry get(int i)
    {
        if (i >= 0 && i < pool.size())
            return pool.get(i);
        else
            return new TranslationCacheEntry("", "", "", "");
    }

    public int getSize()
    {
        return pool.size();
    }

    public int getMaxSize()
    {
        return maxLength;
    }

    public void remove(int i)
    {
        if (i >= 0 && i < pool.size())
            pool.remove(i);
    }
}

