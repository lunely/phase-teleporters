package example.phaseteleporters.energy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Shared energy amount formatting for GUI bars and item tooltips. */
public final class PEEnergyFormat {
    private PEEnergyFormat() {}

    public static String format(long amount) {
        if (amount >= 1_000_000) return compact(amount, 6, "MJ");
        if (amount >= 1_000) return compact(amount, 3, "kJ");
        return String.format(Locale.GERMANY, "%,d J", amount);
    }

    private static String compact(long amount, int places, String unit) {
        return BigDecimal.valueOf(amount).movePointLeft(places)
                .setScale(1, RoundingMode.DOWN).toPlainString() + " " + unit;
    }
}
