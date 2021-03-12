package com.monst.bankingplugin.banking.bank.configuration;

import com.monst.bankingplugin.exceptions.DoubleParseException;
import com.monst.bankingplugin.utils.Utils;

import javax.annotation.Nonnull;

abstract class DoubleConfigurationOption extends ConfigurationOption<Double> {

    protected DoubleConfigurationOption() {
        super();
    }

    protected DoubleConfigurationOption(Double value) {
        super(value);
    }

    @Override
    public String getFormatted() {
        return Utils.format(get());
    }

    @Override
    protected Double parse(@Nonnull String input) throws DoubleParseException {
        try {
            return Utils.scale(parseDouble(input));
        } catch (NumberFormatException e) {
            throw new DoubleParseException(input);
        }
    }

    static double parseDouble(@Nonnull String input) {
        return Math.abs(Double.parseDouble(Utils.removePunctuation(input, '.')));
    }

}