package com.modusbox.client.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils  {

    public static String trimMfiCode(String str, int index)
    {
        String mfiCode = "";
        if (str != null  || str.length() >= 3)
        {
             mfiCode = str.substring(0,index);
        }
        return mfiCode;
    }
    public static String trimIdValue(String str, int index)
    {
        String idValue = "";
        if (str != null  || str.length() > 3)
        {
            idValue = str.substring(index);
        }
        return idValue;
    }
    public static boolean isOnlyDigits (String str)
    {
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.matches();
    }
}
