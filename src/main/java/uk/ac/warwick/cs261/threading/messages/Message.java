package uk.ac.warwick.cs261.threading.messages;

/**
 * An immutable value object that encapsulates a single human-readable log entry produced
 * by a simulation event, carrying the timestamp, originating component, descriptive text,
 * and severity of the occurrence.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>All constructor arguments must not be {@code null}; no defensive copying or
 *       validation is performed.</li>
 *   <li>This class is effectively immutable and therefore safe for concurrent read access
 *       once constructed, provided the publishing thread ensures visibility (e.g. by
 *       inserting the instance into a thread-safe queue before the consumer reads it).</li>
 * </ul>
 */
public class Message
{
    /**
     * The simulation tick at which this message was generated.
     */
    private final long time;

    /**
     * A short label identifying the component or subsystem that produced this message,
     * such as {@code "Simulation"} or {@code "Simulation Error"}; used by the UI to
     * attribute log entries to their origin.
     */
    private final String source;

    /**
     * The human-readable text describing the event or condition that caused this message
     * to be produced.
     */
    private final String message;

    /**
     * The urgency classification of this message; determines how the UI layer highlights
     * or filters the entry in the simulation log.
     */
    private final MessageSeverity severity;

    /**
     * Constructs a fully specified, immutable {@code Message}.
     *
     * @param time     the simulation tick at which the event occurred; must be
     *                 non-negative
     * @param source   a short label identifying the producing component; must not be
     *                 {@code null}
     * @param message  the human-readable description of the event; must not be
     *                 {@code null}
     * @param severity the urgency classification of this message; must not be
     *                 {@code null}
     */
    public Message(long time, String source, String message, MessageSeverity severity)
    {
        this.time = time;
        this.source = source;
        this.message = message;
        this.severity = severity;
    }

    /**
     * Returns the simulation tick at which the event that produced this message occurred.
     *
     * @return the simulation tick; always non-negative
     */
    public long getTime() { return time; }

    /**
     * Returns the short label identifying the component or subsystem that produced this
     * message.
     *
     * @return the source label; never {@code null}
     */
    public String getSource() { return source; }

    /**
     * Returns the human-readable text describing the event or condition associated with
     * this message.
     *
     * @return the message text; never {@code null}
     */
    public String getMessage() { return message; }

    /**
     * Returns the urgency classification that governs how the UI layer displays or
     * filters this message.
     *
     * @return the {@link MessageSeverity}; never {@code null}
     */
    public MessageSeverity getSeverity() { return severity; }

    /**
     * Returns a formatted string representation of this message suitable for logging or
     * debugging, combining the severity, source, timestamp, and message text.
     *
     * <p>The format is {@code "[SEVERITY] source @ time: message"}, for example:
     * {@code "[HIGH] Simulation @ 3600: BA118 landing diverted"}.
     *
     * @return a non-{@code null} formatted string summarising all fields of this message
     */
    @Override
    public String toString()
    {
        return "[" + severity + "] " + source + " @ " + time + ": " + message;
    }
}