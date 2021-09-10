package com.modusbox.client.common;

public class PhoneNumberUtils {

    public static String stripCode(String number) {

        if (number.startsWith("+")) {
            number = number.substring(1);
        }

        if (number.startsWith("95")) {
            number = number.substring(2);
        }

        if (number.startsWith("9")) {
            number = "0" + number;
        }
        return number;
    }

}
