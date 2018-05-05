package grephy;

import org.apache.log4j.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Grep {
    private static final Logger LOGGER = Logger.getLogger(Grep.class);

    private static final String USAGE_MESSAGE = "Usage: java grephy.Grep [-n NFA-FILE] [-d DFA-FILE] REGEX FILE";

    private static String nfaFile = "";
    private static String dfaFile = "";

    private static String regexString;
    private static String inputFile;

    private static List<String> inputFileLines;
    private static List<Character> alphabetList;

    public static void main (String[] args) {
        configureLogger();

        if (args.length < 2 || args.length > 6) {
            System.out.println(USAGE_MESSAGE);
            System.exit(1);
        }

        // Handle optional arguments specifying NFA and DFA output files
        int i;
        for (i = 0; i < 4 && args[i].charAt(0) == '-'; i++) {
            switch (args[i].charAt(1)) {
                case 'n':
                    if (i + 1 < args.length) {
                        nfaFile = args[++i];
                        LOGGER.info("NFA File: " + nfaFile);
                    } else {
                        LOGGER.error("No NFA file specified.");
                        System.out.println(USAGE_MESSAGE);
                        System.exit(1);
                    }
                    break;
                case 'd':
                    if (i + 1 < args.length) {
                        dfaFile = args[++i];
                        LOGGER.info("DFA File: " + dfaFile);
                    } else {
                        LOGGER.error("No DFA file specified.");
                        System.out.println(USAGE_MESSAGE);
                        System.exit(1);
                    }
                    break;
                default:
                    System.out.println(USAGE_MESSAGE);
                    System.exit(1);
            }
        }

        // Handle regex and input file arguments
        if (i < args.length) {
            regexString = args[i++];
            LOGGER.info("Regex: " + regexString);
        }
        if (i < args.length) {
            inputFile = args[i];
            LOGGER.info("Alphabet File: " + inputFile);
        } else {
            System.out.println(USAGE_MESSAGE);
            System.exit(1);
        }

        try {
            inputFileLines = Files.readAllLines(Paths.get(inputFile));
        } catch (IOException e) {
            LOGGER.error(e);
            System.exit(1);
        }

        alphabetList = new ArrayList();
        for (String line : inputFileLines) {
            for (char symbol : line.toCharArray()) {
                if (!alphabetList.contains(symbol)) {
                    alphabetList.add(symbol);
                }
            }
        }

        NFA nfa = RegexConverter.nfaFromRegex(regexString, alphabetList);
        nfa.removeEpsilons();

        for (String line : inputFileLines) {
            if (nfa.accepts(line)) {
                System.out.println(line);
            }
        }

        if (nfaFile.length() > 0) {
            try {
                PrintWriter nfaOut = new PrintWriter(nfaFile);
                for (String line : nfa.toDotFile()) {
                    nfaOut.println(line);
                }
                nfaOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void configureLogger() {
        ConsoleAppender console = new ConsoleAppender();

        // Configure the appender
        String PATTERN = "%d [%p] [%c] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.ALL);
        console.activateOptions();

        LOGGER.addAppender(console);
    }
}
