package uk.ac.warwick.cs261.simulation;

import java.util.ArrayList;

/**
 * Holds a list of error messages and indices of JavaFX TextField objects for the UI to display to the user upon a failed validation of input parameters.
 */
public class ValidationResult {
    public ArrayList<String> messages;
    public ArrayList<Integer> textFieldIndex;

    public ValidationResult(ArrayList<String> messages, ArrayList<Integer> index) {
        this.messages = messages;
        this.textFieldIndex = index;
    }
}
