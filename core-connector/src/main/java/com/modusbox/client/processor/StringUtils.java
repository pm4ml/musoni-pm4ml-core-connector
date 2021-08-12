package com.modusbox.client.processor;

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

}
