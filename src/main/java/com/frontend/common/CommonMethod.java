package com.frontend.common;

import org.springframework.stereotype.Service;

@Service
public class CommonMethod {

    // check string is integer? if yes then return the integer value
    public Integer checkStringIsInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
