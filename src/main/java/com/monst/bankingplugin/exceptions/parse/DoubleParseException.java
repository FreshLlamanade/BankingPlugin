package com.monst.bankingplugin.exceptions.parse;

import com.monst.bankingplugin.lang.Message;

public class DoubleParseException extends ArgumentParseException {

    public DoubleParseException(String input) {
        super(Message.NOT_A_NUMBER, input);
    }

}
