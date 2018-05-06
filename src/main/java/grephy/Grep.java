package grephy;

import org.apache.log4j.*;

import javax.xml.bind.ValidationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Grep.java - The main class.
 */
public class Grep {
    private static final Logger LOGGER = Logger.getLogger(Grep.class);

    private static final String USAGE_MESSAGE = "Usage: java grephy.Grep [-n NFA-FILE] [-d DFA-FILE] REGEX FILE";

    private static String nfaFile = "";
    private static String dfaFile = "";

    private static String regexString;
    private static String inputFile;

    private static List<String> inputFileLines;
    private static List<Character> alphabetList;

    /**
     * Usage: java grephy.Grep [-n NFA-FILE] [-d DFA-FILE] REGEX FILE
     * Handles argument parsing and program functionality.
     * Generates an NFA from REGEX, then converts it to a minimized DFA. Prints accepted strings (lines) from specified
     * FILE at the end.
     *
     * @param args Program arguments
     */
    public static void main (String[] args) {
        // Logger setup
        configureLogger();
        LOGGER.setLevel(Level.OFF);

        // There must be at least a regex and input file, and at most those two plus optional args.
        if (args.length < 2 || args.length > 6) {
            System.out.println(USAGE_MESSAGE);
            System.exit(1);
        }

        // Handle optional arguments specifying NFA and DFA output files (order does not matter)
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

        learnAlphabet();

        // Create a simplified NFA from the regex
        NFA nfa = null;
        try {
            nfa = RegexConverter.nfaFromRegex(regexString, alphabetList);
        } catch (ValidationException e) {
            LOGGER.error(e);
            System.out.println("Invalid regex: " + e.getMessage());
            System.exit(1);
        }
        nfa.removeEpsilons();

        outputDotFile(nfa, nfaFile);

        DFA dfa = new DFA(nfa, alphabetList);

        outputDotFile(dfa, dfaFile);

        // Output matching lines from the input file
        for (String line : inputFileLines) {
            if (nfa.accepts(line)) {
                System.out.println(line);
            }
        }


    }

    /**
     * Learns the alphabet from the input file.
     */
    private static void learnAlphabet() {
        // Attempt to read lines from the input file
        try {
            inputFileLines = Files.readAllLines(Paths.get(inputFile));
        } catch (IOException e) {
            LOGGER.error(e);
            System.out.println("Unable to read file " + inputFile + ".");
            System.exit(1);
        }

        // Get all unique characters from the file
        alphabetList = new ArrayList();
        for (String line : inputFileLines) {
            for (char symbol : line.toCharArray()) {
                if (!alphabetList.contains(symbol)) {
                    alphabetList.add(symbol);
                }
            }
        }
    }

    /**
     * Outputs a DOT language format file to the specified filename.
     *
     * @param nfa NFA (or DFA) to generate
     * @param file Output filename
     */
    private static void outputDotFile(NFA nfa, String file) {
        if (file.length() > 0) { // If the filename was specified
            try {
                PrintWriter nfaOut = new PrintWriter(file);
                for (String line : nfa.toDotFile()) {
                    nfaOut.println(line);
                }
                nfaOut.close();
            } catch (FileNotFoundException e) {
                LOGGER.error(e);
                System.out.println("Unable to write to file " + file + ".");
                System.exit(1);
            }
        }
    }

    /**
     * Set up the Log4j logger.
     */
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
