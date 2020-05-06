import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class FlightAttendant extends Thread {

    //To store the start of this thread
    private long startTime;

    private Semaphore zoneOneSem;
    private Semaphore zoneTwoSem;
    private Semaphore zoneThreeSem;
    private Semaphore startBoardingSem;
    private Semaphore goHomeSem;

    private Queue<Passenger> zoneOneQueue;
    private Queue<Passenger> zoneTwoQueue;
    private Queue<Passenger> zoneThreeQueue;
    private Queue<Passenger> passengersQueue;

    private ArrayList<Passenger> passengersList;


    public FlightAttendant(){
        super("FlightAttendant");
        this.passengersList   = new ArrayList<>();

        this.startBoardingSem = new Semaphore(0, false);
        this.zoneOneSem       = new Semaphore(0, true);
        this.zoneTwoSem       = new Semaphore(0, true);
        this.zoneThreeSem     = new Semaphore(0, true);
        this.goHomeSem        = new Semaphore(0, false);

        this.zoneOneQueue     = new LinkedList<>();
        this.zoneTwoQueue     = new LinkedList<>();
        this.zoneThreeQueue   = new LinkedList<>();
        this.passengersQueue  = new LinkedList<>();

    }

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

    public void wait(Semaphore sem){
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void signal(Semaphore sem){
        sem.release();
    }

    public void addToZoneOneQueue(Passenger passenger){
        zoneOneQueue.add(passenger);
    }

    public void addToZoneTwoQueue(Passenger passenger){
        zoneTwoQueue.add(passenger);
    }

    public void addToZoneThreeQueue(Passenger passenger){
        zoneThreeQueue.add(passenger);
    }

    public void msg(String msg){
        System.out.println("["+getTime()+"] " + getName() + ": " + msg);
    }

    public long getTime(){
        return System.currentTimeMillis() - this.startTime;
    }

    public void sortBySeatNumber(){
        passengersList.sort(Passenger::compareSeatNumber);
    }

    public void addPassToPlaneList(Passenger passenger){
        this.passengersList.add(passenger);
    }

    public void addPassToWaitGate(Passenger passenger){
        this.passengersQueue.add(passenger);
    }

    public void waitToStartBoarding(){
        try {
            startBoardingSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            System.out.println("Ooops. Looks like the flight attendant just quit");
        }
    }



    public void boardPlane(Semaphore lineSem, Queue<Passenger> lineQueue, int zone){
        int groupSizeCount  = 0;
        int totalPassengers = lineQueue.size();
        int groupNum        = Shared.groupNum;
        // This is pretty much the same algorithm used in the Producer-Consumer threads from class
        while ( totalPassengers > 0 ){
            wait(Shared.mutex);
            groupSizeCount++;
            totalPassengers--;
            if( totalPassengers == 0 && groupSizeCount % groupNum != 0 ){
                groupNum = groupSizeCount = groupSizeCount % groupNum;
            }
            signal(Shared.mutex);
            signal(lineSem);
            if (groupSizeCount % groupNum == 0) {
                for (int i = 0; i < groupNum; i++) {
                    signal(Shared.groupMutex);
                }
                // sleep to let the previous group enter the plane
                goToSleep(100);
                Shared.numOfPassengersInPlane += groupNum;
                msg("group: " + ++Shared.numOfGroups + " size: " + groupNum + " in zone " + zone + " entered plane");
            }
        }
    }

    public void rebookFlights(){
        // if any passenger missed a flight
        while (Shared.gateWaitingAreaSem.hasQueuedThreads()){
            Passenger passenger = passengersQueue.poll();
            if( passenger != null ) {
                msg(passenger.getName() + " missed the flight");
                signal(Shared.gateWaitingAreaSem);
            }
        }
    }

    public void disembarkPassengers(){
        sortBySeatNumber();
        for( Passenger passenger : passengersList ){
            passenger.exitPlane();

            //wait .5 secs to let next passenger exit plane
            goToSleep(500);
        }
    }

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
        waitToStartBoarding();

        msg("Started boarding plane");
        msg("All passengers should walk to their respective lines");

        // call all passenger in waitingArea to their zones
        // if a passenger is not in this semaphore, then the passenger missed the flight
        callPassengers();

        // passengers can take up to 4 seconds to get to the line
        // wait for all passengers to make it to the boarding line
        goToSleep(4000);



        // board zone 1
        boardPlane(zoneOneSem, zoneOneQueue, 1);
        // board zone 2
        boardPlane(zoneTwoSem, zoneTwoQueue, 2);
        // board zone 3
        boardPlane(zoneThreeSem, zoneThreeQueue, 3);

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

        //wake up all passengers (signal)
        wait(Shared.mutex);
        Shared.numberOfPassengers = 0;
        signal(Shared.mutex);

        // let passengers exit plane
        disembarkPassengers();

        msg("waiting for passengers to disembark");
        wait(goHomeSem);

        msg(passengersList.size() + " passengers arrived home");

        msg("cleaning plane...");
        goToSleep(2000);
        msg("Finished cleaning");
        msg("Going home");
        signal(Shared.boardedPlaneSem);

    }
}
