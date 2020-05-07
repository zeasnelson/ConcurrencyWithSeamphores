import java.util.concurrent.Semaphore;

public class Clock extends Thread {

    //To store the start time of this thread
    private long startTime;
    private FlightAttendant flightAttendant;

    public Clock(FlightAttendant flightAttendant){
        super("Clock");
        this.flightAttendant = flightAttendant;
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

    public void msg(String msg){
        System.out.println("["+getTime()+"] " + getName() + ": " + msg);
    }

    public long getTime(){
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * Put this thread to sleep for a specified amount of time
     * @param millis The time in milliseconds for this thread to sleep
     */
    public void gotToSleep(long millis){
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Calculate the depart time based on the  number of passengers
     * @param numOfPassengers the number of passengers in the simulation
     * @return the plane depart time
     */
    public int getDepartTime(int numOfPassengers){
        if( numOfPassengers <= 10)
            return 10000;
        else if( numOfPassengers <= 20)
            return 15000;
        else
            return 20000;
    }


    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();

        //plane depart time is calculated based on the number of passengers
        int boardingTime = getDepartTime(Shared.numberOfPassengers);
        //the approx. time at which the flight attendant is done embarking

        msg("Flight to Purell-Wonderland, NY will start boarding at " + boardingTime);

        //sleep until its time to board plane
        gotToSleep(boardingTime);
        msg("Boarding has started");

        //signal flight attendant to start boarding
        signal(flightAttendant.getStartBoardingSem());

        wait(Shared.boardedPlaneSem);

        // flight length
        gotToSleep(5000);

        //signal the flight attendant to start disembarking plane
        signal(Shared.planeLandedSem);

        // wait for the flight attendant to clean plane
        // reusing a semaphore
        wait(Shared.boardedPlaneSem);
        msg("Airport closed");
        msg("Clock terminated");

    }
}
