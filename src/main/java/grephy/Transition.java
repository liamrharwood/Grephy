package grephy;

import java.util.Optional;

/**
 * Transition.java - Represents state transitions in an NFA / DFA.
 */
public class Transition {
    public int stateFrom;
    public int stateTo;
    public Optional<Character> symbol; // Can be empty in the case of epsilon transitions

    public Transition(int stateFrom, int stateTo, Optional<Character> symbol) {
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.symbol = symbol;
    }
}
