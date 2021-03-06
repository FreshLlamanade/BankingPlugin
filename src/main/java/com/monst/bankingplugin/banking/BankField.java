package com.monst.bankingplugin.banking;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public enum BankField implements BankingEntityField<Bank> {

    NAME ("Name", Bank::getRawName),
    OWNER ("OwnerUUID", Bank::getOwnerUUID),
    COUNT_INTEREST_DELAY_OFFLINE ("CountInterestDelayOffline"),
    REIMBURSE_ACCOUNT_CREATION ("ReimburseAccountCreation"),
    PAY_ON_LOW_BALANCE ("PayOnLowBalance"),
    INTEREST_RATE ("InterestRate"),
    ACCOUNT_CREATION_PRICE ("AccountCreationPrice"),
    MINIMUM_BALANCE ("MinimumBalance"),
    LOW_BALANCE_FEE ("LowBalanceFee"),
    INITIAL_INTEREST_DELAY ("InitialInterestDelay"),
    ALLOWED_OFFLINE_PAYOUTS ("AllowedOfflinePayouts"),
    OFFLINE_MULTIPLIER_DECREMENT ("OfflineMultiplierDecrement"),
    WITHDRAWAL_MULTIPLIER_DECREMENT ("WithdrawalMultiplierDecrement"),
    PLAYER_BANK_ACCOUNT_LIMIT ("PlayerBankAccountLimit"),
    INTEREST_MULTIPLIERS("Multipliers"),
    INTEREST_PAYOUT_TIMES ("InterestPayoutTimes"),
    WORLD ("World", b -> b.getRegion().getWorld().getName()),
    MIN_X ("MinX", b -> b.getRegion().getMinX()),
    MAX_X ("MaxX", b -> b.getRegion().getMaxX()),
    MIN_Y ("MinY", b -> b.getRegion().getMinY()),
    MAX_Y ("MaxY", b -> b.getRegion().getMaxY()),
    MIN_Z ("MinZ", b -> b.getRegion().getMinZ()),
    MAX_Z ("MaxZ", b -> b.getRegion().getMaxZ()),
    VERTICES ("PolygonVertices", b -> b.getRegion().getVertices()),
    REGION ("", null); // Meant as a placeholder for the previous 7

    private static final BankField[] VALUES = values();
    private static final BankField[] CONFIGURABLE = stream().filter(f -> f.getter == null).toArray(BankField[]::new);

    private final String databaseAttribute;
    private final Function<Bank, Object> getter;
    private String path;

    BankField(String databaseAttribute) {
        this.databaseAttribute = databaseAttribute;
        this.getter = null;
    }

    BankField(String databaseAttribute, Function<Bank, Object> getter) {
        this.databaseAttribute = databaseAttribute;
        this.getter = getter;
    }

    public String getDatabaseAttribute() {
        return databaseAttribute;
    }

    public static BankField getByName(String name) {
        return stream()
                .filter(field -> field.toString().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Object getFrom(Bank bank) {
        if (getter == null)
            return bank.get(this).getCustomValue();
        return getter.apply(bank);
    }

    public static Stream<BankField> stream() {
        return Stream.of(VALUES);
    }

    public static Stream<BankField> streamConfigurable() {
        return Stream.of(CONFIGURABLE);
    }

    @Override
    public String toString() {
        return path != null ? path : (path = name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

}
