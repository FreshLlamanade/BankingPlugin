package com.monst.bankingplugin.banking.bank.configuration;

import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.exceptions.DoubleParseException;
import com.monst.bankingplugin.utils.QuickMath;
import com.monst.bankingplugin.utils.Utils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.NumberFormat;

public class InterestRate extends DoubleConfigurationOption {

    private static final NumberFormat formatter = NumberFormat.getInstance();
    static {
        formatter.setMinimumIntegerDigits(1);
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(2);
    }

    public InterestRate() {
        super();
    }

    public InterestRate(Double value) {
        super(value);
    }

    @Override
    Config.ConfigPair<Double> getConfigPair() {
        return Config.getInterestRate();
    }

    @Override
    public String format(Double value) {
        return formatter.format(value * 100) + "%";
    }

    @Override
    Double parse(@Nonnull String input) throws DoubleParseException {
        BigDecimal bd;
        try {
            bd = new BigDecimal(Utils.removePunctuation(input, '.')).abs();
        } catch (NumberFormatException e) {
            throw new DoubleParseException(input);
        }
        bd = QuickMath.scale(bd, 4);
        if (input.charAt(input.length() - 1) == '%')
            bd = QuickMath.divide(bd, 100);
        return bd.doubleValue();
    }

}
