package uk.ac.warwick.cs261;

import org.junit.Test;

import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftFactory;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.Assert.*;

public class AircraftOrderingTest
{
    /** Creates an arriving aircraft with the given status and queue-enter time. */
    private Aircraft arriving(AircraftStatus status, long queueEnterTime)
    {
        Aircraft a = AircraftFactory.createLandingAircraft(0, 200);
        a.setQueueEnterTime(queueEnterTime);
        a.setStatus(status);
        return a;
    }

    /** Creates a departing aircraft with the given queue-enter time. */
    private Aircraft departing(long queueEnterTime)
    {
        Aircraft a = AircraftFactory.createTakeoffAircraft(0, 200);
        a.setQueueEnterTime(queueEnterTime);
        return a;
    }

    private List<Aircraft> drain(PriorityQueue<Aircraft> pq)
    {
        List<Aircraft> result = new ArrayList<>();
        while (!pq.isEmpty()) result.add(pq.poll());
        return result;
    }

    private boolean isTierTwo(AircraftStatus s)
    {
        return s == AircraftStatus.LOW_FUEL || s == AircraftStatus.PASSENGER_HEALTH_SEVERE;
    }

    private boolean isTierOne(AircraftStatus s)
    {
        return s == AircraftStatus.MECHANICAL_FAILURE || s == AircraftStatus.PASSENGER_HEALTH;
    }

    @Test
    public void holdingQueue_lowFuelBeforeOk()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft ok      = arriving(AircraftStatus.OK, 1);
        Aircraft lowFuel = arriving(AircraftStatus.LOW_FUEL, 2);

        q.add(ok); 
        q.add(lowFuel);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_severeHealthBeforeOk()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft ok     = arriving(AircraftStatus.OK, 1);
        Aircraft severe = arriving(AircraftStatus.PASSENGER_HEALTH_SEVERE, 2);
        q.add(ok); q.add(severe);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_mechFailureBeforeOk()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft ok   = arriving(AircraftStatus.OK, 1);
        Aircraft mech = arriving(AircraftStatus.MECHANICAL_FAILURE, 2);
        q.add(ok); q.add(mech);

        assertTrue(isTierOne(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_passengerHealthBeforeOk()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft ok     = arriving(AircraftStatus.OK, 1);
        Aircraft health = arriving(AircraftStatus.PASSENGER_HEALTH, 2);
        
        q.add(ok); 
        q.add(health);

        assertTrue(isTierOne(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_lowFuelBeforeMechFailure()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft mech    = arriving(AircraftStatus.MECHANICAL_FAILURE, 1);
        Aircraft lowFuel = arriving(AircraftStatus.LOW_FUEL, 2);

        q.add(mech); 
        q.add(lowFuel);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_severeHealthBeforeMechFailure()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft mech   = arriving(AircraftStatus.MECHANICAL_FAILURE, 1);
        Aircraft severe = arriving(AircraftStatus.PASSENGER_HEALTH_SEVERE, 2);

        q.add(mech); 
        q.add(severe);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_lowFuelBeforePassengerHealth()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft health  = arriving(AircraftStatus.PASSENGER_HEALTH, 1);
        Aircraft lowFuel = arriving(AircraftStatus.LOW_FUEL, 2);

        q.add(health); 
        q.add(lowFuel);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_severeHealthBeforePassengerHealth()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft health = arriving(AircraftStatus.PASSENGER_HEALTH, 1);
        Aircraft severe = arriving(AircraftStatus.PASSENGER_HEALTH_SEVERE, 2);

        q.add(health); 
        q.add(severe);

        assertTrue(isTierTwo(drain(q).get(0).getStatus()));
    }

    @Test
    public void holdingQueue_fullDrainRespectsAllTiers()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();

        q.add(arriving(AircraftStatus.OK, 1));
        q.add(arriving(AircraftStatus.PASSENGER_HEALTH, 2));
        q.add(arriving(AircraftStatus.MECHANICAL_FAILURE, 3));
        q.add(arriving(AircraftStatus.PASSENGER_HEALTH_SEVERE, 4));
        q.add(arriving(AircraftStatus.LOW_FUEL, 5));

        List<Aircraft> result = drain(q);

        assertTrue(isTierTwo(result.get(0).getStatus()));
        assertTrue(isTierTwo(result.get(1).getStatus()));
        assertTrue(isTierOne(result.get(2).getStatus()));
        assertTrue(isTierOne(result.get(3).getStatus()));

        assertEquals(AircraftStatus.OK, result.get(4).getStatus());
    }

    @Test
    public void holdingQueue_tierTwo_earlierQueueTimeFirst()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft late  = arriving(AircraftStatus.LOW_FUEL, 10);
        Aircraft early = arriving(AircraftStatus.LOW_FUEL,  5);

        q.add(late); 
        q.add(early);

        assertSame(early, drain(q).get(0));
    }

    @Test
    public void holdingQueue_tierOne_earlierQueueTimeFirst()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft late  = arriving(AircraftStatus.MECHANICAL_FAILURE, 10);
        Aircraft early = arriving(AircraftStatus.MECHANICAL_FAILURE,  5);

        q.add(late); 
        q.add(early);

        assertSame(early, drain(q).get(0));
    }

    @Test
    public void holdingQueue_tierZero_earlierQueueTimeFirst()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft late  = arriving(AircraftStatus.OK, 10);
        Aircraft early = arriving(AircraftStatus.OK,  5);

        q.add(late); 
        q.add(early);

        assertSame(early, drain(q).get(0));
    }

    @Test
    public void holdingQueue_mixedTierAndTime_correctFullOrder()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft fuel1   = arriving(AircraftStatus.LOW_FUEL, 5);
        Aircraft severe1 = arriving(AircraftStatus.PASSENGER_HEALTH_SEVERE, 3);
        Aircraft mech1   = arriving(AircraftStatus.MECHANICAL_FAILURE, 7);
        Aircraft health1 = arriving(AircraftStatus.PASSENGER_HEALTH, 2);
        Aircraft ok1     = arriving(AircraftStatus.OK, 1);

        q.add(fuel1); 
        q.add(severe1); 
        q.add(mech1); 
        q.add(health1); 
        q.add(ok1);

        List<Aircraft> result = drain(q);

        // Tier 2: SEVERE1 (t=3) before FUEL1 (t=5)
        assertSame(severe1, result.get(0));
        assertSame(fuel1, result.get(1));
        // Tier 1: HEALTH1 (t=2) before MECH1 (t=7)
        assertSame(health1, result.get(2));
        assertSame(mech1, result.get(3));
        // Tier 0
        assertSame(ok1, result.get(4));
    }

    @Test
    public void takeoffQueue_earlierQueueTimeFirst()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft late  = departing(10);
        Aircraft early = departing( 5);
        q.add(late); q.add(early);

        assertSame(early, drain(q).get(0));
    }

    @Test
    public void takeoffQueue_fullDrainIsFifo()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft d1 = departing(10);
        Aircraft d2 = departing(20);
        Aircraft d3 = departing(30);
        Aircraft d4 = departing(40);
        Aircraft d5 = departing(50);

        q.add(d3);
        q.add(d1);
        q.add(d5); 
        q.add(d2); 
        q.add(d4);

        List<Aircraft> result = drain(q);
        assertSame(d1, result.get(0));
        assertSame(d2, result.get(1));
        assertSame(d3, result.get(2));
        assertSame(d4, result.get(3));
        assertSame(d5, result.get(4));
    }

    @Test
    public void takeoffQueue_statusIgnored()
    {
        PriorityQueue<Aircraft> q = new PriorityQueue<>();
        Aircraft late  = departing(10);
        Aircraft early = departing( 5);
        late.setStatus(AircraftStatus.LOW_FUEL); // high severity, must not affect order

        q.add(late); 
        q.add(early);

        assertSame(early, drain(q).get(0));
    }
}