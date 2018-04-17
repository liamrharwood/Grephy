package grephy;

public class Grep {
    private static final String USAGE_MESSAGE = "Usage: java grephy.Grep [-n NFA-FILE] [-d DFA-FILE] REGEX FILE";

    private static String nfaFile;
    private static String dfaFile;

    public static void main (String[] args) {
        if (args.length < 2) {
            System.out.println(USAGE_MESSAGE);
            System.exit(1);
        }

        for (int i = 0; i < 4 && args[i].charAt(0) == '-'; i++) {
            switch (args[i].charAt(1)) {
                case 'n':
                    if (i + 1 < args.length) {
                        nfaFile = args[++i];
                        System.out.println("NFA File: " + nfaFile);
                    } else {
                        System.out.println("No NFA file specified.");
                        System.out.println(USAGE_MESSAGE);
                        System.exit(1);
                    }
                    break;
                case 'd':
                    if (i + 1 < args.length) {
                        dfaFile = args[++i];
                        System.out.println("DFA File: " + dfaFile);
                    } else {
                        System.out.println("No DFA file specified.");
                        System.out.println(USAGE_MESSAGE);
                        System.exit(1);
                    }
                    break;
                default:
                    System.out.println(USAGE_MESSAGE);
                    System.exit(1);
            }
        }
    }
}
