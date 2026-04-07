package uk.ac.warwick.cs261.ui.factories;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;
import uk.ac.warwick.cs261.ui.UIUtil;

public class MessageViewFactory 
{
    private static String ROOT_CSS_CLASS = "msg-root";
    private static String TIME_CSS_CLASS = "msg-time";
    private static String SOURCE_CSS_CLASS = "msg-source";
    private static String MESSAGE_CSS_CLASS = "msg-message";

    private static String HIGH_SEVERITY_CSS_SUFFIX = "--high";
    private static String MEDIUM_SEVERITY_CSS_SUFFIX = "--medium";
    private static String LOW_SEVERITY_CSS_SUFFIX = "--low";

    private static String getSeveritySuffix(MessageSeverity severity) 
    {
        return switch (severity) 
        {
            case HIGH   -> HIGH_SEVERITY_CSS_SUFFIX;
            case MEDIUM -> MEDIUM_SEVERITY_CSS_SUFFIX;
            case LOW    -> LOW_SEVERITY_CSS_SUFFIX;
        };
    }

    public static HBox create(Message message) 
    {
        String suffix = getSeveritySuffix(message.getSeverity());
        
        Label timeLabel = new Label(UIUtil.timeToString(message.getTime()));
        timeLabel.getStyleClass().add(TIME_CSS_CLASS);

        Label sourceLabel = new Label(message.getSource());
        sourceLabel.getStyleClass().addAll(SOURCE_CSS_CLASS, SOURCE_CSS_CLASS + suffix);

        Label messageLabel = new Label(message.getMessage());
        messageLabel.getStyleClass().addAll(MESSAGE_CSS_CLASS, MESSAGE_CSS_CLASS + suffix);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        HBox messageOutput = new HBox(8, timeLabel, sourceLabel, messageLabel);
        messageOutput.getStyleClass().add(ROOT_CSS_CLASS);

        return messageOutput;
    }

    private MessageViewFactory() {}
}