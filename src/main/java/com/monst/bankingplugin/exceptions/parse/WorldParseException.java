package com.monst.bankingplugin.exceptions.parse;

import com.monst.bankingplugin.lang.Message;

public class WorldParseException extends ArgumentParseException {

    public WorldParseException(String input) {
        super(Message.NOT_A_WORLD, input);
    }

}
