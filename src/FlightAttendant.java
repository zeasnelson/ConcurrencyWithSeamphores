import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class FlightAttendant extends Thread {

    //To store the start of this thread
    private long startTime;

    // to store the number of groups of passengers
    private int numOfGroups;

    // each zone has its own blocking semaphore
    private Semaphore zoneOneSem;
    private Semaphore zoneTwoSem;
    private Semaphore zoneThreeSem;

    // blocking sem for the clock thread to signal the flightAttendant to start boarding
    private Semaphore startBoardingSem;

    // blocking sem the last passenger signals the flightAttendant to start cleaning plane
    private Semaphore goHomeSem;

    // to pull all passengers that don't miss the flight
    private Queue<Passenger> passengersQueue;

    // all passengers that boarded the plane,
    // to be able to disembark the plane is ascending order by seatNum
    private ArrayList<Passenger> passengersList;

    //constructor
    public FlightAttendant(){
        super("FlightAttendant");
        this.numOfGroups = 0;
        this.passengersList   = new ArrayList<>();

        this.startBoardingSem = new Semaphore(0, false);
        this.zoneOneSem       = new Semaphore(0, true);
        this.zoneTwoSem       = new Semaphore(0, true);
        this.zoneThreeSem     = new Semaphore(0, true);
        this.goHomeSem        = new Semaphore(0, false);

        this.passengersQueue  = new LinkedList<>();

    }


    // getters and setters
    public Semaphore getZoneOneLineSem() {
        return zoneOneSem;
    }

    public Semaphore getZoneTwoLineSem() {
        return zoneTwoSem;
    }

    public Semaphore getZoneThreeLineSem() {
        return zoneThreeSem;
    }

    public Semaphore getGoHomeSem() { return goHomeSem; }

    public Semaphore getStartBoardingSem() {
        return startBoardingSem;
    }

    public int getNumOfPassengersInPlane(){
        return passengersList.size();
    }

    public void sortBySeatNumber(){
        passengersList.sort(Passenger::compareSeatNumber);
    }

    public void msg(String msg){
        System.out.println("["+getTime()+"] " + getName() + ": " + msg);
    }

    public long getTime(){
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * P(S)
     * @param sem semaphore
     */
    public void wait(Semaphore sem){
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * V(S)
     * @param sem semaphore
     */
    public void signal(Semaphore sem){
        sem.release();
    }

    /**
     * Add a passenger to the list of passengers who are boarding the plane
     * @param passenger the passenger to be added to the list
     */
    public void addPassToPlaneList(Passenger passenger){
        this.passengersList.add(passenger);
    }

    /**
     * Add a passenger to a queue
     * This are the passengers who don't miss the flight
     * @param passenger the passenger to be added to the queue
     */
    public void addPassToWaitGate(Passenger passenger){
        this.passengersQueue.add(passenger);
    }

    /**
     * Put this thread to sleep
     * @param milli time in millis for thread to sleep
     */
    public void goToSleep(long milli){
        try {
            sleep(milli);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            System.out.println("Oops. Looks like the flight attendant just quit");
        }
    }

    /**
     * For each zone, make groups of 4 passengers and board plane
     * @param lineSem zone line semaphore
     * @param zone the zone being boarded
     */
    public void boardPlane(Semaphore lineSem, int zone){
        int groupSizeCount  = 0;
        int groupNum  = Shared.groupNum;

        while ( lineSem.hasQueuedThreads() ){
            // there is only one flightAttendant using this variable, no need to protect with mutex
            groupSizeCount++;
            signal(lineSem);
            if (groupSizeCount % groupNum == 0) {
                for (int i = 0; i < groupNum; i++) {
                    signal(Shared.groupMutex);
                }
                // sleep to let the previous group enter the plane
                goToSleep(200);
                msg("group: " + ++numOfGroups + " in zone " + zone + " entered plane");
            }
        }
    }

    /**
     * All the passengers who missed the flight should still be waiting in gateWaitingArea semaphore
     * The if statement in the passenger thread will allow the passenger to gracefully terminate
     */
    public void rebookFlights(){
        // if any passenger missed a flight
        while (Shared.gateWaitingAreaSem.hasQueuedThreads()){
            Passenger passenger = passengersQueue.poll();
            if( passenger != null ) {
                msg(passenger.getName() + " zoneNum: " + passenger.getZoneNum() + " missed the flight");
                signal(Shared.gateWaitingAreaSem);
            }
        }
    }

    /**
     * Sorts the passengersList by seatNum and signals the passenger's exit semaphore
     */
    public void disembarkPassengers(){
        sortBySeatNumber();
        for( Passenger passenger : passengersList ){
            signal(passenger.getExitPlaneSem());
            //wait .5 secs to let next passenger exit plane
            goToSleep(500);
        }
    }

    /**
     * All passengers who made it to the gate will be released here
     * when the flightAttendant begins calling zones
     */
    public void callPassengers(){
        int size = Shared.gateWaitingAreaSem.getQueueLength();
        int i = 0;
        while ( i < size ){
            Passenger passenger = passengersQueue.poll();
            if( passenger != null ) {
                passenger.setMissedFlight(false);
                signal(Shared.gateWaitingAreaSem);
                i++;
            }
        }
    }

    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();

        msg("started. Waiting to start calling passengers");
        //wait until Clock signals to start boarding
        wait(startBoardingSem);

        msg("Started boarding plane");

        // call all passenger in waitingArea to their zones
        // if a passenger is not in this semaphore, then the passenger missed the flight
        callPassengers();

        // passengers can take up to 4 seconds to get to the line
        // wait for all passengers to make it to the boarding line
        goToSleep(4000);


        msg("zone 1 passengers can enter plane  in groups of 4");
        // board zone 1
        boardPlane(zoneOneSem, 1);
        // board zone 2
        msg("zone 2 passengers can enter plane in groups of 4");
        boardPlane(zoneTwoSem, 2);
        // board zone 3
        msg("zone 3 passengers can enter plane in groups of 4");
        boardPlane(zoneThreeSem, 3);

        // wait for all passengers inside plane to seat
        goToSleep(1000);

        // signal the Clock thread that all passenger boarded the plane
        signal(Shared.boardedPlaneSem);

        // rebook flights
        rebookFlights();

        msg("Welcome to Purrel Airlines.");
        msg("Plane is departing now");
        msg("This flight will be two hours long.");

        //wait for two hours
        wait(Shared.planeLandedSem);

        msg("Plane is about to land");
        goToSleep(1000);
        msg("Plane landed. Leave in ascending order by seat number");

        // let passengers exit plane
        disembarkPassengers();

        msg("waiting for passengers to disembark");
        wait(goHomeSem);

        msg(passengersList.size() + " passengers arrived home");

        msg("cleaning plane...");
        goToSleep(2000);
        msg("Finished cleaning");
        msg("Going home - terminated");
        signal(Shared.boardedPlaneSem);

    }
}
