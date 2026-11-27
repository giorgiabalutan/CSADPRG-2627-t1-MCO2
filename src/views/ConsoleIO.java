/********************
 * Last names: Avila, Balutan, Eborde, Ermitanio
 * Language: Java
 * Paradigm(s): OOP
 ********************/

package views;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper function for reading and writing on the console. */
public final class ConsoleIO {
    /* menu choices start at 1 */
    private static final int MIN_CHOICE = 1;

    /* biggest amount accepted, and how the error message words it */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final String MAX_AMOUNT_LABEL = "1 trillion";

    /* amounts are printed with 2 decimal places, like the spec's sample output */
    private static final int DISPLAY_SCALE = 2;

    /* defines a valid choice (eithere a single number or a number in brackets such as [2]) */
    private static final Pattern CHOICE_PATTERN =
            Pattern.compile("\\[(0|[1-9][0-9]*)\\]|(0|[1-9][0-9]*)");

    /* an amount is plain digits with an optional fractional part: "5", "5.25" */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)?");

    /*
     * a name starts with a letter, then letters, spaces, and . ' - only (no commas, to keep the CSV simple)
     */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("\\p{L}[\\p{L}\\p{M} .'\\u2019-]*"); // \\u2019 is single apostrophe
    private static final int MAX_NAME_LENGTH = 50;

    /**
     * Stops anyone from making an object of this class, since all its methods are static.
     *
     * @param None
     * @return None
     */
    private ConsoleIO() {}

    /**
     * Prints the items as a numbered list: [1] first, [2] second, and so on.
     *
     * @param menus the items to print, in order
     * @return None
     */
    public static void printOptions(String... menus) {
        for (int i = 0; i < menus.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, menus[i]);
        }
    }

    /**
     * Keeps asking until the user enters a menu choice. The input must be a number like 2 or a
     * number in brackets like [2].
     *
     * @param sc the Scanner that reads the console
     * @param prompt the text shown before each try, like "Choice: "
     * @param max the highest choice allowed (the lowest is 1)
     * @return the choice, from 1 to max
     */
    public static int safeGetInt(Scanner sc, String prompt, int max) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            Matcher matcher = CHOICE_PATTERN.matcher(input);
            if (!matcher.matches()) {
                printErrorMessage(ErrorMessage.INVALID_INPUT_TYPE, "number");
                continue;
            }

            /* group 1 is the digits inside the brackets, group 2 the bare digits */
            String digits = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            BigInteger number =
                    new BigInteger(
                            digits); // BigInteger so a huge number is "out of range", not a crash

            if (number.compareTo(BigInteger.valueOf(MIN_CHOICE)) >= 0
                    && number.compareTo(BigInteger.valueOf(max)) <= 0) {
                return number.intValue();
            }
            printErrorMessage(ErrorMessage.INPUT_OUT_OF_RANGE, MIN_CHOICE, max);
        }
    }

    /**
     * Keeps asking until the user types something that is not empty.
     *
     * @param sc the Scanner that reads the console
     * @param prompt the text shown before each try
     * @return the text that was typed
     */
    public static String safeGetString(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }
            printErrorMessage(ErrorMessage.EMPTY_INPUT);
        }
    }

    /**
     * Keeps asking until the name is valid. A name starts with a letter and can only have letters,
     * spaces, periods, apostrophes, and hyphens. Commas are not allowed.
     *
     * @param sc the Scanner that reads the console
     * @param prompt the text shown before each try
     * @return the name
     */
    public static String safeGetName(Scanner sc, String prompt) {
        while (true) {
            String name = safeGetString(sc, prompt); // already handles empty input

            if (name.length() > MAX_NAME_LENGTH) {
                printErrorMessage(ErrorMessage.NAME_TOO_LONG, MAX_NAME_LENGTH);
            } else if (!NAME_PATTERN.matcher(name).matches()) {
                printErrorMessage(ErrorMessage.NAME_INVALID);
            } else {
                return name;
            }
        }
    }

    /**
     * Keeps asking until the user enters a positive amount that has at most maxScale decimal places
     * and is not too large.
     *
     * @param sc the Scanner that reads the console
     * @param prompt the text shown before each try
     * @param maxScale the most decimal places allowed
     * @return the amount
     */
    public static BigDecimal safeGetAmount(Scanner sc, String prompt, int maxScale) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            /* checked before parsing, since BigDecimal also accepts "1e3", "+5", ".5", "5." */
            if (!AMOUNT_PATTERN.matcher(input).matches()) {
                printErrorMessage(ErrorMessage.INVALID_INPUT_TYPE, "amount");
                continue;
            }

            BigDecimal amount = new BigDecimal(input);

            if (amount.signum() <= 0) {
                printErrorMessage(ErrorMessage.NON_POSITIVE_AMOUNT);
            } else if (amount.stripTrailingZeros().scale() > maxScale) {
                printErrorMessage(ErrorMessage.AMOUNT_OUT_OF_SCALE, maxScale);
            } else if (amount.compareTo(MAX_AMOUNT) > 0) {
                printErrorMessage(ErrorMessage.AMOUNT_TOO_LARGE, MAX_AMOUNT_LABEL);
            } else {
                return amount;
            }
        }
    }

    /**
     * Keeps asking until the user types Y or N (any case).
     *
     * @param sc the Scanner that reads the console
     * @param prompt the text shown before each try
     * @return true for Y, false for N
     */
    public static boolean safeGetYesNo(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("Y")) {
                return true;
            }
            if (input.equalsIgnoreCase("N")) {
                return false;
            }
            printErrorMessage(ErrorMessage.INVALID_YES_NO);
        }
    }

    /**
     * Prints an error on its own line.
     *
     * @param error which message to print
     * @param args the values for the %d or %s spots in the message, if any
     * @return None
     */
    public static void printErrorMessage(ErrorMessage error, Object... args) {
        System.out.println("ERROR: " + error.getMessage(args));
    }

    /**
     * Turns an amount into text with exactly 2 decimal places. It rounds half up and never uses
     * scientific notation.
     *
     * @param amount the amount to show
     * @return the amount as text
     */
    public static String formatAmount(BigDecimal amount) {
        return amount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }
}
