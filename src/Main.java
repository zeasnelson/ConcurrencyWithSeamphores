import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        Shared.numberOfPassengers = 19;  // num of passengers in simulation
        Shared.groupNum = 4;             // size of group when entering plane
        Shared.counterNum = 3;           // clerk line length

        // create threads
        FlightAttendant flightAttendant = new FlightAttendant();
        Clerk clerkOne = new Clerk("ClerkOne");
        Clerk clerkTwo = new Clerk("ClerkTwo");
        Clock clock   = new Clock(flightAttendant);

        ArrayList<Passenger> passengers = new ArrayList<>(Shared.numberOfPassengers);
        for( int i = 0; i < Shared.numberOfPassengers; i++ ){
            passengers.add(new Passenger("Passenger-"+(i+1), clerkOne, clerkTwo, flightAttendant));
        }

        // start threads
        clock.start();
        passengers.forEach(Passenger::start);
        clerkOne.start();
        clerkTwo.start();
        flightAttendant.start();

    }

}
