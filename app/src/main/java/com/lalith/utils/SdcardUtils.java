package com.lalith.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by LXB8673 on 4/1/2016.
 */
public class SdcardUtils {

    public final static int
            KILOBYTE = 1024,
            MEGABYTE = KILOBYTE * 1024,
            GIGABYTE = MEGABYTE * 1024;

    public static String getStoragePath() {
        String storagePath;
        if (Environment.getExternalStorageDirectory().list() != null)
            storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        else
            storagePath = new File("/").getAbsolutePath();

        return storagePath;
    }

    public static final FileFilter DEFAULT_FILE_FILTER = new FileFilter()
    {

        @Override
        public boolean accept(File pathname)
        {
            return pathname.isHidden() == false;
        }
    };

    public static int totalFiles(File root)
    {
        if (root.isDirectory() == false) return 1;
        File[] files = root.listFiles();
        if (files == null) return 0;
        int n = 0;
        for (File file : files)
        {
            if (file.isDirectory())
                n += totalFiles(file);
            else
                n ++;
        }
        return n;
    }

    public static Map<String, Long> sortByComparator(Map<String, Long> unsortMap, final boolean order) {

        List<Map.Entry<String, Long>> list = new LinkedList<Map.Entry<String, Long>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> o1,
                               Map.Entry<String, Long> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Long> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static double average(List<Long> list) {
        if (list == null || list.isEmpty())
            return 0.0;
        // Calculate the summation of the elements in the list
        long sum = 0;
        int n = list.size();
        for (int i = 0; i < n; i++)
            sum += list.get(i);
        return ((double) sum) / n;
    }

    public static String calculateLength(double lengthOfFile) {
        if(lengthOfFile / KILOBYTE > 1) {
            if(lengthOfFile / MEGABYTE > 1) {
                if(lengthOfFile / GIGABYTE > 1) {
                    return  String.format(Locale.ENGLISH, "%.2f gb" ,(double)(lengthOfFile /GIGABYTE));
                }else {
                    return String.format(Locale.ENGLISH, "%.2f mb" ,(double)(lengthOfFile /MEGABYTE));
                }
            }else {
                return String.format(Locale.ENGLISH, "%.2f kb" ,(double)(lengthOfFile /KILOBYTE));
            }
        }else {
            return  String.format(Locale.ENGLISH, "%.2f bytes" ,lengthOfFile);
        }
    }

    /**
     * Gets extension of the file name excluding the . character
     */
    public static String getFileExtension(String fileName)
    {
        if (fileName.contains("."))
            return fileName.substring(fileName.lastIndexOf('.')+1);
        else
            return "";
    }
}
