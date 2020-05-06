import java.util.Random;
import java.util.concurrent.Semaphore;

public class Passenger extends Thread {

    // Store the seat number
    private int seatNum;

    // Store the zone number
    private int zoneNum;

    //
    private Boolean missedFlight;

    // To save the start time for the thread
    private long startTime;

    private Clerk clerkOne, clerkTwo;
    private FlightAttendant flightAttendant;

    private Semaphore exitPlane;

    public Passenger(String passengerID, Clerk clerkOne, Clerk clerkTwo, FlightAttendant flightAttendant){
        super(passengerID);
        this.clerkOne = clerkOne;
        this.clerkTwo = clerkTwo;
        this.flightAttendant = flightAttendant;
        this.missedFlight = true;
        this.exitPlane = new Semaphore(0, false);
    }

    public void setMissedFlight(Boolean missedFlight) {
        this.missedFlight = missedFlight;
    }

    public long getTime(){
        return System.currentTimeMillis() - startTime;
    }

    public int getSeatNum() {
        return seatNum;
    }

    public void setSeatNum(int seatNum) {
        this.seatNum = seatNum;
    }

    public void setZoneNum(int zoneNum) {
        this.zoneNum = zoneNum;
    }

    public void msg(String msg){
        System.out.println("["+getTime()+"] " + getName() + ": " + msg);
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

    /**
     *  Compare the seat number of this passenger with the seat number of another passenger
     * @param OtherPassenger an object of type Passenger
     * @return < 0 if this passengers seat is less that the other, 0 if equal,
     * > 0 if the seat number of this passenger is grater than the other's passenger seat number
     */
    public int compareSeatNumber(Passenger OtherPassenger) {
        return seatNum - OtherPassenger.getSeatNum();
    }

    public void exitPlane(){
        exitPlane.release();
    }

    /**
     * Put the thread to sleep
     * @param milli time to sleep in millis
     */
    public void goToSleep(int milli){
        try {
            sleep(milli);
        } catch (InterruptedException e) {
            System.out.println("Passenger lost");
        }
    }


    /**
     * Assign this passenger to one of the clerk lines
     * the clerk line is randomly assigned
     */
    public void goToClerkLine(){
        /*
         * 0: clerk one line
         * 1: clerk two line
         */
        int random = (int) Math.round(Math.random());
        switch (random) {
            case 0:
                clerkOne.addPassenger(this);
                msg("arrived to the airport. Waiting for clerkOne");
                wait(clerkOne.getLineSem());
                break;
            case 1:
                clerkTwo.addPassenger(this);
                msg("arrived to the airport. Waiting for clerkTwo");
                wait(clerkTwo.getLineSem());
        }

    }

    public void goToFlightAttendantLine(){
        switch (zoneNum) {
            case 1:
                flightAttendant.addToZoneOneQueue(this);
                msg("ZoneNum: " + zoneNum + " waiting in zone one line");
                wait(flightAttendant.getZoneOneLineSem());
                break;
            case 2:
                flightAttendant.addToZoneTwoQueue(this);
                msg("ZoneNum: " + zoneNum + " waiting in zone two line");
                wait(flightAttendant.getZoneTwoLineSem());
                break;
            case 3:
                flightAttendant.addToZoneThreeQueue(this);
                msg("ZoneNum: " + zoneNum + " waiting in zone three line");
                wait(flightAttendant.getZoneThreeLineSem());
        }
    }



    @Override
    public void run() {

        this.startTime = System.currentTimeMillis();

        //get a random number between 0 and 3 sec for arrival time
        Random rand = new Random();
        int randomTime = rand.nextInt(3000);
        goToSleep(randomTime);

        // wait at the line to get a seat and zone number
        goToClerkLine();

        msg("Walking to the gate");
        randomTime = rand.nextInt(3000)+3000;
        goToSleep(randomTime);
        msg("arrived at gate. Waiting for flight attendant to call");


        wait(Shared.mutex);
        flightAttendant.addPassToWaitGate(this);
        signal(Shared.mutex);

        //wait at gate
        wait(Shared.gateWaitingAreaSem);


        // passenger takes between 1 to 2 seconds to get to line
        randomTime = rand.nextInt(1000) + 1000;
        goToSleep(randomTime);

        if( !missedFlight ) {

            signal(Shared.mutex);
            flightAttendant.addPassToPlaneList(this);
            wait(Shared.mutex);

            //block this passenger in line depending on zone number
            goToFlightAttendantLine();

            // all passengers wait here until they form a group of size groupNum
            wait(Shared.groupMutex);
            msg("entered plane - going to sleep");


            // wait to exit plane
            wait(exitPlane);

            msg("seatNum: " + seatNum + " leaving plane");

            wait(Shared.mutex);
            Shared.numberOfPassengers++;
            // last passenger, signal FlightAttendant to start cleaning
            if ( Shared.numberOfPassengers == Shared.numOfPassengersInPlane ) {
                signal(flightAttendant.getGoHomeSem());
            }
            signal(Shared.mutex);

        }
        else {
            // passenger missed flight
            goToSleep(100);
            msg("re-booked flight. Terminates");
        }



    }
}
