package com.monst.bankingplugin.exceptions;

import com.monst.bankingplugin.lang.Message;

public class MaterialParseException extends ArgumentParseException {

    public MaterialParseException(String input) {
        super(Message.NOT_A_MATERIAL, input);
    }

}
