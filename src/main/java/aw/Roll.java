package aw;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Individual roll run
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Roll {
    public static final Random RNG = new SecureRandom();

    private String description;
    private List<Integer> rolls;
    private Integer sum;
    private Integer bonus;

    /**
     * Create a roll
     */
    public static Roll roll(int times, int base, int bonus) {
        final String description = times + "d" + base + RollHandler.addBonus(bonus);
        final List<Integer> rolls = new ArrayList<>();
        int sum = 0;

        for (int i = 0; i < times; i++) {
            // Zero based, so add 1
            int roll = RNG.nextInt(base) + 1;
            sum = sum + roll;
            rolls.add(roll);
        }

        return new Roll(description, rolls, sum, bonus);
    }
}
