package uk.ac.warwick.cs261.cli;

import java.util.List;

import uk.ac.warwick.cs261.simulation.Simulation;
import uk.ac.warwick.cs261.simulation.SimulationParameters;
import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.SynchronisationState;
import uk.ac.warwick.cs261.threading.messages.Message;

public class CommandLineInterface {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_256_RED = "\u001B[38;5;196m"; // Bright red
    private static final String ANSI_256_ORANGE = "\u001B[38;5;208m"; // Orange
    private static final String ANSI_256_WHITE = "\u001B[38;5;255m"; // Bright white

    public static SynchronisationState start(SimulationParameters parameters) {
        Simulation simulation = new Simulation(parameters, true);
        SynchronisationState synchronisation = simulation.getSynchronisation();

        Thread simulationThread = new Thread(simulation);
        simulationThread.start();

        new Thread(() -> run(synchronisation)).start();

        return synchronisation;
    }

    public static void run(SynchronisationState synchronisation) {
        while (!synchronisation.getIsSimulationDone().get()) {
            try {
                synchronisation.getUISemaphore().acquire();

                SimulationState state = synchronisation.getState();

                long currentTime = state.getCurrentTime();
                int holdingQueueSize = state.getHoldingQueue().size();
                int takeoffQueueSize = state.getTakeoffQueue().size();

                String summary = String.format(
                        "CurrentTime: %d, HoldingQueue: %d, TakeoffQueue: %d",
                        currentTime,
                        holdingQueueSize,
                        takeoffQueueSize);

                System.out.println(summary);

                Message currentMessage = synchronisation.getMessageQueue().poll();

                while (currentMessage != null) {
                    print(currentMessage);
                    currentMessage = synchronisation.getMessageQueue().poll();
                }
                Thread.sleep(250);

                synchronisation.getSimulationSemaphore().release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("DONE !!!");
    }

    public static void print(Message message) {
        String colorCode;

        // Determine color based on severity using 256-color codes
        switch (message.getSeverity()) {
            case HIGH:
                colorCode = ANSI_256_RED;
                break;
            case MEDIUM:
                colorCode = ANSI_256_ORANGE;
                break;
            case LOW:
                colorCode = ANSI_256_WHITE;
                break;
            default:
                colorCode = ANSI_256_WHITE;
        }

        // Format: [time] [source] [severity] message
        String formattedMessage = String.format("[%d] [%s] [%s] %s",
                message.getTime(), message.getSource(), message.getSeverity(), message.getMessage());

        // Print with color and reset
        System.out.println(colorCode + formattedMessage + ANSI_RESET);
    }
}
