import java.util.Arrays;

import javax.management.RuntimeErrorException;

public class CommandLineParser {
    
    public enum actions { REGISTER, NEW_GAME, GUESS, LEADERBOARD };

    public void Parse(String playerInput){
        // Check if inpiut is empty
        if (playerInput.isEmpty()){
            throw new IllegalArgumentException("Invalid command: " + playerInput);
        }

        // Normalize the player input
        var normInput = playerInput.strip().split(" ");
        // Extract the command
        var command = normInput[0].toUpperCase();
        
        // Short Circuit if command not in actions
        boolean commandExists = Arrays.stream(actions.values()).anyMatch(action -> action.name().equals(command));
        if (!commandExists) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        
        switch (actions.valueOf(command)) {
            case REGISTER:
                System.out.println("Called Register");
                break;
            case NEW_GAME:
                System.out.println("Called New_Game");
                break;
            case GUESS:
                System.out.println("Called Guess");
                break;
            case LEADERBOARD:
                System.out.println("Called Leaderboard");
                break;
        
            default:
                break;
        }

        // Short Circuit if cannot form a valid command
        if(normInput.length < 1){
            throw new IllegalArgumentException("Invalid Arguments:" + normInput.toString());
        } 



    }
}
