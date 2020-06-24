import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {


        // num of passengers in simulation
        try {
            Shared.numberOfPassengers = Integer.parseInt(args[0]);
            if( Shared.numberOfPassengers < 0 || Shared.numberOfPassengers > 30 ) {
                System.out.print("The number of passengers must be between 0 - 30");
                return;
            }
        }catch(Exception e) {
            System.out.print("Please enter a valid number");
        }

        Shared.counterNum = 3;           // clerk line length
        Shared.groupNum   = 4;           // size of group when entering plane

        // create threads
        FlightAttendant flightAttendant = new FlightAttendant();
        Clerk clerkOne = new Clerk("ClerkOne");
        Clerk clerkTwo = new Clerk("ClerkTwo");
        Clock clock    = new Clock(flightAttendant);

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
