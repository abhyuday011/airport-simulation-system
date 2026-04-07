package uk.ac.warwick.cs261.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Generates and stores a complete pre-computed schedule of typed objects at
 * Poisson-distributed inter-arrival times over a fixed simulation duration, providing
 * sequential access via a stateful cursor.
 *
 * <p>At construction the schedule is populated in a single forward pass: starting from
 * tick {@code 0}, the interval until the next event is sampled from the exponential
 * distribution implied by the Poisson process with rate {@code flowRate} using the
 * inverse-transform method - {@code -ln(1 - U) / lambda}, where {@code U} is drawn from
 * a {@link UniformRealDistribution} over {@code [0, 1)}. The supplied
 * {@code createObjectFunction} is called with the current tick to produce the scheduled
 * object, which is appended to the internal list. This repeats until
 * {@code currentTime >= duration}. The resulting list is a memoryless Poisson arrival
 * process over the full simulation horizon.
 *
 * <p>After construction, objects are retrieved in chronological order via
 * {@link #getNext()}, which advances an internal cursor on each call. Because the
 * schedule is fully materialised at construction time, retrieval is O(1) and requires
 * no further sampling.
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>Construction</td><td>O(n) where n is the expected number of events</td></tr>
 *   <tr><td>{@link #getNext()}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #getSchedule()}</td><td>O(1)</td></tr>
 * </table>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code rng}, {@code createObjectFunction}, and the objects it produces must not
 *       be {@code null}.</li>
 *   <li>{@code flowRate} must be strictly positive; passing {@code 0} or a negative value
 *       will produce an infinite or undefined inter-arrival time.</li>
 *   <li>The internal cursor advances monotonically; there is no mechanism to reset or
 *       replay the schedule without constructing a new instance.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 *
 * @param <T> the type of objects produced by {@code createObjectFunction} and stored in
 *            the schedule
 */
public class Schedule<T>
{
    /**
     * The complete pre-computed list of scheduled objects in chronological order,
     * populated at construction and never modified thereafter.
     */
    private final List<T> schedule = new ArrayList<>();

    /**
     * The zero-based index of the next object to be returned by {@link #getNext()};
     * incremented on each successful call. When {@code currentIndex >= schedule.size()},
     * the schedule is exhausted and {@link #getNext()} returns {@code null}.
     */
    private int currentIndex;

    /**
     * Constructs a {@code Schedule} by generating a Poisson-distributed sequence of
     * objects over the specified duration and populating the internal list.
     *
     * <p>The generation algorithm proceeds as follows:
     * <ol>
     *   <li>Initialise {@code currentTime} to {@code 0}.</li>
     *   <li>While {@code currentTime < duration}: invoke {@code createObjectFunction}
     *       with {@code currentTime} to produce the next object and append it to the
     *       schedule; then advance {@code currentTime} by the inter-arrival interval
     *       computed via {@link #getTimeTillNextEvent(UniformRealDistribution, double)}.</li>
     *   <li>Initialise the cursor {@link #currentIndex} to {@code 0}.</li>
     * </ol>
     *
     * @param rng                  the random number generator used to seed the internal
     *                             {@link UniformRealDistribution}; must not be {@code null}
     * @param flowRate             the mean number of events per simulation tick (i.e. the
     *                             Poisson rate {@code lambda}); must be strictly positive
     * @param duration             the total number of simulation ticks over which to
     *                             generate events; must be non-negative
     * @param createObjectFunction a factory function that accepts a scheduled tick and
     *                             returns the corresponding object; must not be
     *                             {@code null} and must not return {@code null}
     */
    public Schedule(RandomGenerator rng, double flowRate, long duration, Function<Long, T> createObjectFunction)
    {
        UniformRealDistribution standardUniformDistribution = new UniformRealDistribution(rng, 0.0, 1.0);

        long currentTime = 0;

        while (currentTime < duration)
        {
            schedule.add(createObjectFunction.apply(currentTime));
            currentTime += getTimeTillNextEvent(standardUniformDistribution, flowRate);
        }

        currentIndex = 0;
    }

    /**
     * Returns the next scheduled object and advances the internal cursor by one position.
     *
     * <p>Objects are returned in the chronological order in which they were generated at
     * construction. Once all objects have been retrieved, subsequent calls return
     * {@code null} indefinitely without modifying the cursor further.
     *
     * @return the next scheduled object in chronological order, or {@code null} if the
     *         schedule has been fully consumed
     */
    public T getNext()
    {
        if (currentIndex < schedule.size())
            return schedule.get(currentIndex++);

        return null;
    }

    /**
     * Returns the complete pre-computed list of scheduled objects in chronological order.
     *
     * <p>The returned list is the internal backing list and is not copied; callers must
     * not modify it. It reflects the full schedule regardless of how many objects have
     * already been retrieved via {@link #getNext()}.
     *
     * @return the immutable view of all scheduled objects; never {@code null}
     */
    public List<T> getSchedule() { return schedule; }

    /**
     * Computes the inter-arrival interval until the next Poisson event using the
     * inverse-transform method.
     *
     * <p>For a Poisson process with rate {@code lambda}, inter-arrival times follow an
     * exponential distribution with the same rate. The inverse CDF is
     * {@code -ln(1 - U) / lambda}, where {@code U} is a standard uniform sample on
     * {@code [0, 1)}. The result is rounded to the nearest {@code long} to produce an
     * integer tick offset.
     *
     * @param distribution the standard uniform distribution over {@code [0.0, 1.0)} used
     *                     to sample {@code U}; must not be {@code null}
     * @param lambda       the Poisson rate (mean events per tick); must be strictly
     *                     positive to avoid division by zero or a negative interval
     * @return the number of simulation ticks until the next event, rounded to the nearest
     *         integer; always non-negative for valid inputs
     */
    private long getTimeTillNextEvent(UniformRealDistribution distribution, double lambda)
    {
        return Math.round(-Math.log(1.0 - distribution.sample()) / lambda);
    }
}