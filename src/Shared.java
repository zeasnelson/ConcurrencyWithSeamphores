import java.util.concurrent.Semaphore;

/**
 * This class contains only semaphores and variables that are shared by all or most threads
 */
public class Shared {


    // project specification required variables, initialized in Main
    static int numberOfPassengers;
    static int groupNum;
    static int counterNum;



    //For the clerks to generate unique seat numbers
    static int [] ticketNumbers      = new int[31];



    //blocking semaphores
    // to block all passengers that arrive to the waiting gate
    static Semaphore gateWaitingAreaSem = new Semaphore(0, true);

    // The clock waits in this semaphore until the flightAttendant signal
    // that all passengers boarded the plane
    static Semaphore boardedPlaneSem    = new Semaphore(0, true);

    // flightAttendant blocks here until Clock thread signals that the plane landed
    static Semaphore planeLandedSem     = new Semaphore(0, true);



    // mutex semaphores
    // to enforce M.E while generating seat numbers
    static Semaphore ticketNumMutex     = new Semaphore(1, true);

    // to create groups when passengers board the plane
    static Semaphore groupMutex         = new Semaphore(1, true);

    // To enforce M.E in over different CSs
    static Semaphore mutex              = new Semaphore(1, true);


}
