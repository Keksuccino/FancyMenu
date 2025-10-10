package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class NumberBaseConvertPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_BASE = 10;
    private static final int MIN_BASE = 2;
    private static final int MAX_BASE = 36;
    private static final int MAX_FRACTION_DIGITS = 16;
    private static final char DECIMAL_SEPARATOR = '.';
    private static final MathContext FRACTION_MATH_CONTEXT = new MathContext(50, RoundingMode.HALF_UP);
    private static final BigDecimal EPSILON = new BigDecimal("1E-30");

    public NumberBaseConvertPlaceholder() {
        super("number_base_convert");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String inputValue = dps.values.get("input");
        if ((inputValue == null) || inputValue.trim().isEmpty()) {
            LOGGER.error("[FANCYMENU] Missing 'input' value for 'Number Base Converter' placeholder: {}", dps.placeholderString);
            return null;
        }

        int fromBase = parseBase(dps.values.get("from_base"), DEFAULT_BASE, dps.placeholderString, "from_base");
        int toBase = parseBase(dps.values.get("to_base"), DEFAULT_BASE, dps.placeholderString, "to_base");

        try {
            ParsedNumber parsedNumber = parseInputNumber(inputValue.trim(), fromBase, dps.placeholderString);
            if (parsedNumber == null) {
                return null;
            }
            BigDecimal absoluteValue = parsedNumber.toBigDecimal();
            String converted = convertAbsoluteValueToBase(absoluteValue, toBase);
            if (parsedNumber.negative && !parsedNumber.isZero()) {
                return "-" + converted;
            }
            return converted;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to convert number base for placeholder: {}", dps.placeholderString, ex);
            return null;
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("input");
        values.add("from_base");
        values.add("to_base");
        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.number_base_convert");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.number_base_convert.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("input", "67");
        values.put("from_base", "10");
        values.put("to_base", "2");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    private int parseBase(@Nullable String value, int fallback, @NotNull String placeholderString, @NotNull String valueName) {
        if ((value == null) || value.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (!MathUtils.isInteger(trimmed)) {
            LOGGER.error("[FANCYMENU] Invalid '{}' value for 'Number Base Converter' placeholder: {} (value: {})", valueName, placeholderString, value);
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(trimmed);
            if ((parsed < MIN_BASE) || (parsed > MAX_BASE)) {
                LOGGER.error("[FANCYMENU] '{}' value out of range ({}, {}) for 'Number Base Converter' placeholder: {} (value: {})", valueName, MIN_BASE, MAX_BASE, placeholderString, value);
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            LOGGER.error("[FANCYMENU] Failed to parse '{}' value for 'Number Base Converter' placeholder: {}", valueName, placeholderString, ex);
            return fallback;
        }
    }

    @Nullable
    private ParsedNumber parseInputNumber(@NotNull String input, int base, @NotNull String placeholderString) {
        boolean negative = false;
        String working = input;
        if (working.startsWith("-")) {
            negative = true;
            working = working.substring(1);
        } else if (working.startsWith("+")) {
            working = working.substring(1);
        }
        working = working.trim();
        if (working.isEmpty()) {
            LOGGER.error("[FANCYMENU] Empty 'input' value for 'Number Base Converter' placeholder: {}", placeholderString);
            return null;
        }

        int separatorIndex = working.indexOf(DECIMAL_SEPARATOR);
        if ((separatorIndex != -1) && (working.indexOf(DECIMAL_SEPARATOR, separatorIndex + 1) != -1)) {
            LOGGER.error("[FANCYMENU] Invalid 'input' value (multiple decimal separators) for 'Number Base Converter' placeholder: {}", placeholderString);
            return null;
        }

        String integerPartString = (separatorIndex >= 0) ? working.substring(0, separatorIndex) : working;
        String fractionPartString = (separatorIndex >= 0) ? working.substring(separatorIndex + 1) : "";

        BigInteger integerPart = parseIntegerPart(integerPartString, base, placeholderString);
        if (integerPart == null) {
            return null;
        }
        BigDecimal fractionPart = parseFractionPart(fractionPartString, base, placeholderString);
        if (fractionPart == null) {
            return null;
        }
        boolean isZero = (integerPart.signum() == 0) && (fractionPart.compareTo(BigDecimal.ZERO) == 0);
        return new ParsedNumber(integerPart, fractionPart, negative && !isZero);
    }

    @Nullable
    private BigInteger parseIntegerPart(@NotNull String part, int base, @NotNull String placeholderString) {
        if (part.isEmpty()) {
            return BigInteger.ZERO;
        }
        BigInteger result = BigInteger.ZERO;
        BigInteger baseBig = BigInteger.valueOf(base);
        for (char c : part.toCharArray()) {
            int digit = decodeDigit(c, base, placeholderString);
            if (digit < 0) {
                return null;
            }
            result = result.multiply(baseBig).add(BigInteger.valueOf(digit));
        }
        return result;
    }

    @Nullable
    private BigDecimal parseFractionPart(@NotNull String part, int base, @NotNull String placeholderString) {
        if (part.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseDecimal = BigDecimal.valueOf(base);
        BigDecimal divisor = baseDecimal;
        BigDecimal fraction = BigDecimal.ZERO;
        for (char c : part.toCharArray()) {
            int digit = decodeDigit(c, base, placeholderString);
            if (digit < 0) {
                return null;
            }
            if (digit != 0) {
                BigDecimal digitDecimal = BigDecimal.valueOf(digit);
                fraction = fraction.add(digitDecimal.divide(divisor, FRACTION_MATH_CONTEXT), FRACTION_MATH_CONTEXT);
            }
            divisor = divisor.multiply(baseDecimal, FRACTION_MATH_CONTEXT);
        }
        return fraction.stripTrailingZeros();
    }

    private int decodeDigit(char character, int base, @NotNull String placeholderString) {
        int digit = Character.digit(character, base);
        if (digit < 0) {
            LOGGER.error("[FANCYMENU] Invalid character '{}' for base {} in 'Number Base Converter' placeholder: {}", character, base, placeholderString);
        }
        return digit;
    }

    @NotNull
    private String convertAbsoluteValueToBase(@NotNull BigDecimal value, int base) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        BigInteger integerPart = value.toBigInteger();
        BigDecimal fractionalPart = value.subtract(new BigDecimal(integerPart));

        String integerString = integerPart.toString(base).toUpperCase(Locale.ROOT);
        if (fractionalPart.compareTo(BigDecimal.ZERO) == 0) {
            return integerString;
        }

        String fractionString = convertFractionalPart(fractionalPart, base);
        if (fractionString.isEmpty()) {
            return integerString;
        }
        return integerString + DECIMAL_SEPARATOR + fractionString;
    }

    @NotNull
    private String convertFractionalPart(@NotNull BigDecimal fraction, int base) {
        StringBuilder builder = new StringBuilder();
        BigDecimal baseDecimal = BigDecimal.valueOf(base);
        BigDecimal current = fraction;
        for (int i = 0; (i < MAX_FRACTION_DIGITS) && (current.compareTo(BigDecimal.ZERO) != 0); i++) {
            current = current.multiply(baseDecimal, FRACTION_MATH_CONTEXT);
            int digit = current.intValue();
            if (digit >= base) {
                digit = base - 1;
            } else if (digit < 0) {
                digit = 0;
            }
            builder.append(Character.toUpperCase(Character.forDigit(digit, base)));
            current = current.subtract(BigDecimal.valueOf(digit), FRACTION_MATH_CONTEXT);
            if (current.compareTo(BigDecimal.ZERO) < 0) {
                current = BigDecimal.ZERO;
            } else if (current.compareTo(BigDecimal.ONE) >= 0) {
                current = current.remainder(BigDecimal.ONE);
            }
            if (current.abs().compareTo(EPSILON) < 0) {
                current = BigDecimal.ZERO;
                break;
            }
        }
        return builder.toString();
    }

    private static final class ParsedNumber {

        private final BigInteger integerPart;
        private final BigDecimal fractionPart;
        private final boolean negative;

        private ParsedNumber(@NotNull BigInteger integerPart, @NotNull BigDecimal fractionPart, boolean negative) {
            this.integerPart = integerPart;
            this.fractionPart = fractionPart;
            this.negative = negative;
        }

        private boolean isZero() {
            return (this.integerPart.signum() == 0) && (this.fractionPart.compareTo(BigDecimal.ZERO) == 0);
        }

        @NotNull
        private BigDecimal toBigDecimal() {
            BigDecimal integerAsDecimal = new BigDecimal(this.integerPart);
            return integerAsDecimal.add(this.fractionPart);
        }

    }

}
