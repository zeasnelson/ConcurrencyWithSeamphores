import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String[] args) {

        Shared.numberOfPassengers = 30;
        Shared.groupNum = 4;

        FlightAttendant flightAttendant = new FlightAttendant();
        Clerk clerkOne = new Clerk("ClerkOne");
        Clerk clerkTwo = new Clerk("ClerkTwo");
        Clock clock   = new Clock(flightAttendant);

        ArrayList<Passenger> passengers = new ArrayList<>(Shared.numberOfPassengers);
        for( int i = 0; i < Shared.numberOfPassengers; i++ ){
            passengers.add(new Passenger("Passenger-"+(i+1), clerkOne, clerkTwo, flightAttendant));
        }

        clock.start();
        passengers.forEach(Passenger::start);
        clerkOne.start();
        clerkTwo.start();
        flightAttendant.start();






    }
}
