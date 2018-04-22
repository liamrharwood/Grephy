package grephy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NFA {
    private int stateSet;

    private List<Map<Character, Integer>> deltaFunction;

    public void reset() {
        stateSet = 1 << 0;
    }

    public void process (String inputStr) {
        for (int i = 0; i < inputStr.length(); i++) {
            char c = inputStr.charAt(i);
            int nextStateSet = 0;
            for (int state = 0; state < deltaFunction.size(); state++) {
                if ((stateSet & (1 << state)) != 0) {
                    Optional<Integer> maybeState = Optional.of(deltaFunction.get(state).get(c));
                    if (maybeState.isPresent()) {
                        nextStateSet |= maybeState.get();
                    } else {
                        System.out.println("Invalid alphabet symbol.");
                    }
                }
            }
            stateSet = nextStateSet;
        }
    }
}
