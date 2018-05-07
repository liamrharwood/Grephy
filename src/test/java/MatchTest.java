import grephy.NFA;
import grephy.RegexConverter;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;

public class MatchTest {
    private final Character[] alphabetArr = {'t', 'e', 's', 't'};
    private final String[] inputLinesArr = {"test", "tast", "teest", "tost"};

    @Test
    public void itDoesSimpleMatch() throws ValidationException {
        RegexConverter regexConverter = new RegexConverter();
        NFA nfa = regexConverter.nfaFromRegex("test", Arrays.asList(alphabetArr));

        ArrayList<String> acceptedList = new ArrayList();

        for (String line : inputLinesArr) {
            if (nfa.accepts(line)) {
                acceptedList.add(line);
            }
        }

        Assert.assertEquals(acceptedList.size(), 1);
        Assert.assertEquals(acceptedList.get(0), "test");
    }

    @Test
    public void itDoesMultipleMatch() throws ValidationException {
        RegexConverter regexConverter = new RegexConverter();
        NFA nfa = regexConverter.nfaFromRegex("te*st", Arrays.asList(alphabetArr));

        ArrayList<String> acceptedList = new ArrayList();

        for (String line : inputLinesArr) {
            if (nfa.accepts(line)) {
                acceptedList.add(line);
            }
        }

        Assert.assertEquals(acceptedList.size(), 2);
        Assert.assertEquals(acceptedList.get(0), "test");
        Assert.assertEquals(acceptedList.get(1), "teest");
    }

    @Test
    public void itDoesNegativeMatch() throws ValidationException {
        RegexConverter regexConverter = new RegexConverter();
        NFA nfa = regexConverter.nfaFromRegex("tist", Arrays.asList(alphabetArr));

        ArrayList<String> acceptedList = new ArrayList();

        for (String line : inputLinesArr) {
            if (nfa.accepts(line)) {
                acceptedList.add(line);
            }
        }

        Assert.assertEquals(acceptedList.size(), 0);
    }
}
