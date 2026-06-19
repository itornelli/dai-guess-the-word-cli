package main.java;

import java.util.ArrayList;
// Games are a list of passes or fails 
public record LeaderboardEntry(String name, ArrayList<Boolean> games) {

}
