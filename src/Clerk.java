import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Clerk extends Thread {


    //The start time for this thread
    private long startTime;

    //To store the number of passengers that this clerk helps
    private int numOfPassengersHelped;

    // for the passengers to form lines
    private Semaphore lineSem;

    // for the clerk to assign seat and zone numbers
    private Queue<Passenger> lineQueue;


    /**
     * Constructs a Clerk thread
     * @param threadName The name of this thread
     */
    public Clerk( String threadName ){
        super(threadName);
        this.lineQueue = new LinkedList<>();
        this.lineSem = new Semaphore(Shared.counterNum, true);
    }

    public Semaphore getLineSem() {
        return lineSem;
    }

    public void addPassenger(Passenger passenger){
        this.lineQueue.add(passenger);
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
     * Put this thread to sleep
     * @param milli time in millis for thread to sleep
     */
    public void goToSleep(long milli){
        try {
            sleep(milli);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Print a msg preceded by the thread name and the time this msg is being printed
     * @param msg The msg to be printed
     */
    public void msg(String msg){
        System.out.println("["+getTime()+"] " + getName() + ": " + msg);
    }

    /**
     * Generates a random unique seat number for a passenger
     * @return the seat number
     */
    public int getSeatNum(){
        Random rand = new Random();
        boolean foundTicketNum = false;
        int seatNum = -1;

        // generate a unique seat number
        wait(Shared.ticketNumMutex);
        while (!foundTicketNum){
            seatNum = rand.nextInt(30)+1;
            if( Shared.ticketNumbers[seatNum] == 0 ){
                foundTicketNum = true;
                Shared.ticketNumbers[seatNum]++;
            }
        }
        signal(Shared.ticketNumMutex);

        return seatNum;
    }

    /**
     * Generate a zone number based on the seat number
     * @param seatNum the seat number
     * @return a zone number between 1-3
     */
    public int getZone(int seatNum){
        if( seatNum >= 0 && seatNum <= 10 ){
            return 1;
        }
        else if( seatNum >= 11 && seatNum <= 20 ){
            return 2;
        }

        else if( seatNum >= 21 && seatNum <= 30 ){
            return 3;
        }
        else
            return -1;
    }

    /**
     * Compute the total elapsed time based on the start time
     * @return total elapsed time
     */
    public long getTime(){
        return System.currentTimeMillis() - this.startTime;
    }


    @Override
    public void run() {

        this.startTime = System.currentTimeMillis();

        msg("arrived to the counter. Getting ready...");
        //clerk takes 2 seconds to get ready to work, breakfast, etc...
        goToSleep(2000);
        msg("ready to help passengers");

        //
        boolean run = true;
        while (run){
            wait(Shared.mutex);
            if( Shared.numberOfPassengers != 0 ){
                    Passenger passenger = lineQueue.poll();
                    if( passenger != null ){
                        int seatNum = getSeatNum();
                        int zoneNum = getZone(seatNum);
                        passenger.setSeatNum(seatNum);
                        passenger.setZoneNum(zoneNum);
                        // take .5 seconds to generate a seat and zone number
                        goToSleep(500);
                        // signal the passenger to go to gate
                        signal(passenger.getToGateSem());

                        msg(passenger.getName() + " assigned seatNum: " + seatNum + " zoneNum: " + zoneNum);
                        signal(lineSem);
                        Shared.numberOfPassengers--;
                        numOfPassengersHelped++;
                    }
            }
            else
                run = false;

            signal(Shared.mutex);
        }

        msg("Helped " + numOfPassengersHelped + " passengers. Going home");
    }
}
