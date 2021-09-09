package com.modusbox.client.customexception;

import com.modusbox.client.enums.ErrorCode;

public class CCCustomException extends Exception {

    public CCCustomException(String message) {
        super(message);
    }
}