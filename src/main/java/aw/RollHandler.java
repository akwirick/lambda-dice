package aw;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Roll dice in slack from AWS lambda
 */
public class RollHandler implements RequestHandler<SlackRequest, SlackResponse> {
    public static String DECRYPTED_KEY = decryptKey();
    private static String HELP_TEXT = "To use this command, specify a roll that you would like to make by " +
            "using 'times d base' notation.  For example, to roll a six-sided die one time `\\roll 1d6`." +
            "\nYou can optionally specify a 'bonus' to apply to the roll which will increase or decrease the " +
            "total.  For example, `\\roll 3d8 + 10` would roll three, eight-sided dice and add 10 to the total." +
            "\nYou may also optionally add a descriptor at the end that will show up in the channel.";

    private static String decryptKey() {
        System.out.println("Decrypting key");
        String token = System.getenv("TOKEN");
        if (token == null || token.isEmpty()) {
            token = System.getProperty("TOKEN");
        }

        if (token.equals("IGNORE")) {
            return token;
        }

        byte[] encryptedKey = Base64.decode(token);

        AWSKMS client = AWSKMSClientBuilder.defaultClient();

        DecryptRequest request = new DecryptRequest()
                .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));

        ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
        return new String(plainTextKey.array(), Charset.forName("UTF-8"));
    }


    /**
     * Handle a request from slack
     */
    public SlackResponse handleRequest(SlackRequest slackRequest, Context context) {
        LambdaLogger logger = context.getLogger();

        // Validate the token
        String token = slackRequest.getToken();
        if (!token.equals(DECRYPTED_KEY)) {
            return null;
        }

        final String text = slackRequest.getText();
        logger.log("-----------------------------------------------");
        logger.log("Received a request - " + slackRequest.toString());
        logger.log("-----------------------------------------------");

        final String userName = slackRequest.getUser_name();

        final Optional<Roll> opt;
        if (text == null || text.isEmpty()) {
            opt = parseRoll("1d20");
        } else if (text.contains("help")) {
            // Treat it like a bad request
            opt = Optional.empty();
        } else {
            opt = parseRoll(text);
        }

        if (!opt.isPresent()) {
            return SlackResponse.builder()
                    .text(HELP_TEXT)
                    .response_type("ephemeral")
                    .build();
        }

        Roll roll = opt.get();
        String responseText = buildResponse(roll, userName);
        logger.log("Responding with " + responseText);
        return SlackResponse.builder()
                .text(responseText)
                .response_type("in_channel")
                .build();
    }

    /**
     * Parse a single roll
     */
    public Optional<Roll> parseRoll(String text) {
        String timesPattern = "(\\d{0,3})";    // Digit up to three (999)
        String dee = "(d)";    // d or D
        String basePattern = "(\\d{0,3})";    // Digit up to three (999)
        String bonusPattern = "(\\s*[-+]\\s*\\d{0,5})?";    // Signed int
        String restPattern = "(.*)";

        String regex = timesPattern + dee + basePattern + bonusPattern + restPattern;

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String matchedTimes = matcher.group(1);
            if (matchedTimes == null || matchedTimes.isEmpty()) {
                matchedTimes = "1";
            }

            int times = Integer.parseInt(matchedTimes);

            int base = Integer.parseInt(matcher.group(3));
            String rawBonus = matcher.group(4);
            int bonus = 0;
            if (rawBonus != null && !rawBonus.isEmpty()) {
                rawBonus = rawBonus.replaceAll("\\s+", "");
                bonus = Integer.parseInt(rawBonus);
            }

            // Unused.
//            String rest = matcher.group(5);
//            if (rest != null && !rest.isEmpty()) {
//                rest = rest.replaceFirst("\\s+", "");
//            }

            Roll roll = Roll.roll(times, base, bonus);
            return Optional.of(roll);
        } else {
            return Optional.empty();
        }


    }


    /**
     * Format a response for slack
     */
    public static String buildResponse(Roll roll, String userName) {
        int total = roll.getSum() + roll.getBonus();
        StringJoiner joiner = new StringJoiner(" + ", "(", ")");
        roll.getRolls().forEach(i -> joiner.add(String.valueOf(i)));
        return "@" + userName + " rolled " + roll.getDescription() +  " for *" + total + "*\n"
                + joiner.toString() + addBonus(roll.getBonus()) + " = " + total;

    }

    /**
     * Append the value of the bonus if non-zero
     */
    public static String addBonus(int bonus) {
        if (bonus > 0) {
            return " + " + bonus;
        } else if (bonus < 0) {
            return " - " + bonus;
        }
        return "";
    }

}
