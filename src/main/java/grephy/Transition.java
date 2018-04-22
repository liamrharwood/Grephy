package grephy;

import java.util.Optional;

public class Transition {
    public int stateFrom;
    public int stateTo;
    public Optional<Character> symbol;

    public Transition(int stateFrom, int stateTo, Character symbol) {
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.symbol = Optional.of(symbol);
    }
}