import java.util.concurrent.Semaphore;

public class Shared {

    static int numberOfPassengers;
    static int groupNum;

    static int numOfPassengersInPlane = 0;
    static int numOfGroups            = 0;

    static int [] ticketNumbers      = new int[31];

    static Semaphore gateWaitingAreaSem = new Semaphore(0, true);
    static Semaphore boardedPlaneSem    = new Semaphore(0, true);
    static Semaphore planeLandedSem     = new Semaphore(0, true);
    static Semaphore ticketNumMutex     = new Semaphore(1, true);
    static Semaphore groupMutex         = new Semaphore(1, true);
    static Semaphore mutex              = new Semaphore(1, true);


}
