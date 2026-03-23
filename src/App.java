import java.io.*;
import java.util.*;


class InvalidMenuChoiceException extends Exception{
    public InvalidMenuChoiceException(String choice){
        super("Choice \""+choice+"\" is not a valid menu option.");
    }
}

class InvalidMovieCodeException extends Exception{
    public InvalidMovieCodeException(String code){
        super("Movie Code \""+code+"\" is not a valid Movie Code.");
    }
}

class InvalidShowtimeSelectionException extends Exception {
    public InvalidShowtimeSelectionException(String selection) {
        super("Selection \""+selection+"\" is not a valid showtime option.");
    }
}

class OverbookingException extends Exception {
    public OverbookingException(int available) {
        super("Only "+available+" tickets are available.");
    }
}

class Showtime implements Serializable{
    public String date,time;
    public int totalSeats,availableSeats;
    ArrayList<Integer> reservedSeats;
    public double ticketPrice;

    public Showtime(String date,String time,int totalSeats,int availableSeats,double ticketPrice){
        this.date=date;
        this.time=time;
        this.totalSeats=totalSeats;
        this.availableSeats=availableSeats;
        this.ticketPrice=ticketPrice;
        this.reservedSeats = new ArrayList<>();
    }
}

class Movie implements Serializable{
    public String code,name,language,genre;
    private Map<String,Showtime> showtimes=new HashMap<>();

    public Movie(String code,String name,String language,String genre){
        this.code=code;
        this.name=name;
        this.language=language;
        this.genre=genre;
    }
    public void addShowtime(Showtime s){
        showtimes.put(s.date+s.time,s);
    }
    public Map<String,Showtime> getShowtimes() {
        return showtimes;
    }
}
class MovieRepository implements Serializable{
    public Map<String, Movie> getMr() {
        return mr;
    }

    public void setMr(Map<String, Movie> mr) {
        this.mr = mr;
    }

    private Map<String,Movie>  mr = new LinkedHashMap<>();

    public MovieRepository(Map<String, Movie> mr) {
        this.mr = mr;
    }
}

class DBService {

    private static MovieRepository mr;

    public boolean readData() {
        String file="/home/lasan/Dev/MovieTicket/src/Movie Reservation Dataset.csv";
        //String file="C:\\Users\\MSI\\Downloads\\Exception Handling Lab\\MovieTicket\\src\\Movie Reservation Dataset.csv";
        LinkedHashMap<String, Movie> map = new LinkedHashMap<>();
        try(BufferedReader br=new BufferedReader(new FileReader(file))){

            String line;
            br.readLine();

            while((line=br.readLine())!=null){

                String[] data=line.split(",");
                String code=data[0];
                String name=data[1];
                String date=data[2];
                String time=data[3];
                int total=Integer.parseInt(data[4].trim());
                int avail=Integer.parseInt(data[5].trim());
                double price=Double.parseDouble(data[6].trim());
                String lang=data[7];
                String genre=data[8];

                Movie movie=map.getOrDefault(code,new Movie(code,name,lang,genre));
                movie.addShowtime(new Showtime(date,time,total,avail,price));
                map.put(code,movie);
            }
            mr = new MovieRepository(map);
        } catch(FileNotFoundException e) {
            System.err.println("The file \""+file+"\" is not found.");
            return false;
        } catch(Exception e) {
            System.err.println("Error:"+e.getMessage());
            return false;
        }
        return true;
    }

    public static Collection<Movie> getAllMovies(){
        return mr.getMr().values();
    }

    public static Movie getMovie(String code){
        return mr.getMr().get(code);
    }

    public static void saveState(){
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream("mr.ser"))){
            oos.writeObject(mr);
        } catch (IOException e){
            System.out.println("Something went wrong while saving");
        }
    }
    public static void loadState(){
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream("mr.ser"))) {

            mr = (MovieRepository) ois.readObject();

        } catch (Exception e) {
            System.out.println("Something went wrong while loading save");
        }
    }
}

class BrowseHelper {

    public static Movie selectMovie(Scanner sc) {
        Collection<Movie> movies=DBService.getAllMovies();
        Movie selectedMovie=null;

        while (selectedMovie==null) {
            System.out.println("\nCODE       NAME                 GENRE");
            System.out.println("------------------------------------------");
            for (Movie m : movies) {
                System.out.printf("%-10s %-20s %-10s\n", m.code, m.name, m.genre);
            }

            System.out.print("\n>>> Enter Movie Code to select (or 'BACK' to return): ");
            String inputCode=sc.nextLine().trim().toUpperCase();

            if (inputCode.equals("BACK")) return null;

            try {
                selectedMovie=DBService.getMovie(inputCode);
                if (selectedMovie==null) {
                    throw new InvalidMovieCodeException(inputCode);
                }
            } catch (InvalidMovieCodeException e) {
                System.out.println(e.getMessage());
            }
        }
        return selectedMovie;
    }

    public static Showtime selectShowtime(Scanner sc, Movie selectedMovie) {
        List<Showtime> showList=new ArrayList<>(selectedMovie.getShowtimes().values());
        Showtime selectedShow=null;

        while (selectedShow==null) {
            System.out.println("\n>>> AVAILABLE SHOWTIMES FOR: " + selectedMovie.name);
            System.out.printf("      Date       Time       Price      Available\n");
            for (int i=0; i < showList.size(); i++) {
                Showtime s=showList.get(i);
                System.out.printf("[%d] %-12s %-10s %-10.2f %-10d\n", i + 1, s.date, s.time, s.ticketPrice,
                        s.availableSeats - s.reservedSeats.size());
            }

            System.out.print("\n>>> Select Showtime number (or 'BACK' to return): ");
            try {
                String inputCode=sc.nextLine().trim().toUpperCase();
                if (inputCode.equals("BACK")) return null;

                int timeChoice=Integer.parseInt(inputCode) - 1;

                if (timeChoice<0 || timeChoice>=showList.size()) {
                    throw new InvalidShowtimeSelectionException(inputCode);
                } else {
                    selectedShow=showList.get(timeChoice);
                }
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a valid number for the showtime.");
            } catch (InvalidShowtimeSelectionException e) {
                System.out.println(e.getMessage());
            }
        }
        return selectedShow;
    }

    public static void processTickets(Scanner sc, Showtime selectedShow) {
        while (true) {
            try {
                System.out.println("Available seats: ");
                for (int i = 1; i <= selectedShow.availableSeats; i++){

                    if (!selectedShow.reservedSeats.contains(i)) {
                        System.out.print(i + " ");
                    } else {
                        // to keep spacing consistent
                        System.out.print(" ".repeat(Integer.toString(i).length()) + " ");

                    };
                    if (i%8==0) System.out.print("\n");
                }
                System.out.println("\n>>>> Enter a space seperated list of tickets to select your seats");
                ArrayList<Integer>  seats = new ArrayList<>();
                String l = sc.nextLine();
                for (String s: l.split(" ")){
                    seats.add(Integer.parseInt(s));
                }

                if (seats.isEmpty()) {
                    System.out.println("Please enter a positive number of tickets.");
                    continue;
                }

                if (seats.size()>selectedShow.availableSeats) {
                    throw new OverbookingException(selectedShow.availableSeats);
                }

                selectedShow.reservedSeats.addAll(seats);

                double totalCost=seats.size() * selectedShow.ticketPrice;
                System.out.printf("Success! Total cost: %.2f.\n", totalCost);
                break;

            } catch (OverbookingException e) {
                System.out.println(e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter numeric values for seats.");
            }
        }
    }
}

class POS{

    private static final int TIMEOUT=60;
    private Scanner sc=new Scanner(System.in);
    private int choice=0;
    private Timer timer = new Timer(true);
    private TimerTask currentTask;
    private boolean sleeping = false;

    private synchronized void resetTimer() {
        if (currentTask != null) {
            currentTask.cancel();
        }

        currentTask = new TimerTask() {
            @Override
            public void run() {
                saveSession();
            }
        };

        timer.schedule(currentTask, TIMEOUT);
    }

    private synchronized void saveSession() {
        if (!sleeping) {
            DBService.saveState();
            sleeping = true;
            System.out.println("\n[Session saved due to inactivity]");
        }
    }

    private synchronized void wakeSessionIfNeeded() {
        if (sleeping) {
            DBService.loadState();
            sleeping = false;
            System.out.println("[Session restored]");
        }
    }

    private String readLine() {
        resetTimer();                 // start/restart inactivity countdown
        String input = sc.nextLine(); // blocks until user types something
        wakeSessionIfNeeded();        // if timeout happened while waiting, restore
        resetTimer();                 // restart countdown after activity
        return input;
    }

    public void displayMenu(){
        System.out.println("\n--- MOVIE RESERVATION SYSTEM ---");
        System.out.println("[1]. Browse Available Movies");
        System.out.println("[2] Exit");
        System.out.print(">>> Enter choice: ");
    }

    public void browseMovies() {
        Movie selectedMovie=BrowseHelper.selectMovie(sc);
        if (selectedMovie == null) return;

        Showtime selectedShow=BrowseHelper.selectShowtime(sc, selectedMovie);
        if (selectedShow == null) return;

        BrowseHelper.processTickets(sc, selectedShow);
    }

    public void eventLoop(){
        while(choice!=2){
            displayMenu();

            try{
                String input= readLine();
                choice=Integer.parseInt(input);

                if(choice!=1 && choice!=2){
                    throw new InvalidMenuChoiceException(input);
                }
                switch(choice){
                    case 1:browseMovies();break;
                    case 2:System.out.println("Exiting system...");break;
                }
            } catch(NumberFormatException e){
                System.out.println("Error: Please enter a numeric choice.");
                choice=0;
            } catch (InvalidMenuChoiceException e){
                System.out.println(e.getMessage());
                choice=0;
            } catch (Exception e){
                System.out.println("Unexpected error : "+e.getMessage());
                choice=0;
            }
        }
    }
}

public class App{

    public static void main(String[] args){

        DBService db=new DBService();
        if(db.readData()){
            POS p=new POS();
            p.eventLoop();
        }
    }
}