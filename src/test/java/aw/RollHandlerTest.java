package aw;

import static aw.RollHandler.buildResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Random;

/**
 * Tests for {@link aw.RollHandler}
 */
public class RollHandlerTest {
    private static final Random RNG = new Random();

    @BeforeClass
    public void setup() {
        System.setProperty("TOKEN", "IGNORE");
    }

    @Test
    public void testNormalRolls() {
        for (int i = 0; i < 50; i++) {
            int select = RNG.nextInt(7);
            final int base;
            switch (select) {
                case 0:
                    base = 2;
                    break;
                case 1:
                    base = 4;
                    break;
                case 2:
                    base = 6;
                    break;
                case 3:
                    base = 8;
                    break;
                case 4:
                    base = 10;
                    break;
                case 5:
                    base = 12;
                    break;
                case 6:
                    base = 20;
                    break;
                case 7:
                    base = 100;
                    break;
                default:
                    base = 0;
            }
            int times = RNG.nextInt(6) + 1;
            int bonus = RNG.nextInt(10);
            String input = times + "d" + base;
            if (bonus != 0) {
                input = input + String.format("+%d", bonus);
            }

            if (RNG.nextBoolean()) {
                input = input + " for foo";
            }

            System.out.println("INPUT:" + input);
            testIndividual(input);
        }

        testIndividual("3d6 + 10");
        testIndividual("3d6 + 10000000000000000000");
        testIndividual("0d100");
        testIndividual("-1d100");
        testIndividual("999d999");
    }

    private void testIndividual(String testString) {
        RollHandler rollHandler = new RollHandler();
        Optional<Roll> opt = rollHandler.parseRoll(testString);
        assertThat(opt).isPresent();
        Roll roll = opt.get();
        String testOut = buildResponse(roll, "test");
        assertThat(testOut).isNotNull();
        System.err.println("OUTPUT:" + testOut);
    }
}
