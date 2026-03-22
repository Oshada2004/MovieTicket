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
    public double ticketPrice;

    public Showtime(String date,String time,int totalSeats,int availableSeats,double ticketPrice){
        this.date=date;
        this.time=time;
        this.totalSeats=totalSeats;
        this.availableSeats=availableSeats;
        this.ticketPrice=ticketPrice;
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

class DBService{

    private static Map<String,Movie> movieRepository=new LinkedHashMap<>();

    public boolean readData() {
        //String file="/home/lasan/Dev/MovieTicket/src/Movie Reservation Dataset.csv";
        String file="C:\\Users\\MSI\\Downloads\\Exception Handling Lab\\MovieTicket\\src\\Movie Reservation Dataset.csv";

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

                Movie movie=movieRepository.getOrDefault(code,new Movie(code,name,lang,genre));
                movie.addShowtime(new Showtime(date,time,total,avail,price));
                movieRepository.put(code,movie);
            }
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
        return movieRepository.values();
    }

    public static Movie getMovie(String code){
        return movieRepository.get(code);
    }
}

class POS{

    private static final int TIMEOUT = 60;
    private Scanner sc=new Scanner(System.in);
    private int choice=0;
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask(){
        @Override
        public void run() {
            saveSession();
        }
    };


    private void saveSession(){

    }

    public void displayMenu(){
        System.out.println("\n--- MOVIE RESERVATION SYSTEM ---");
        System.out.println("[1]. Browse Available Movies");
        System.out.println("[2] Exit");
        System.out.print(">>> Enter choice: ");
    }

    public void browseMovies() {
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
            
            if (inputCode.equals("BACK")) return;

            try {
                selectedMovie=DBService.getMovie(inputCode);
                if(selectedMovie==null) {
                    throw new InvalidMovieCodeException(inputCode);
                }
            } catch (InvalidMovieCodeException e) {
                System.out.println(e.getMessage());
            }
        }

        List<Showtime> showList=new ArrayList<>(selectedMovie.getShowtimes().values());
        Showtime selectedShow=null;

        while (selectedShow==null) {
            System.out.println("\n>>> AVAILABLE SHOWTIMES FOR: "+selectedMovie.name);
            System.out.printf("      Date       Time       Price      Available\n");
            for (int i=0; i<showList.size(); i++) {
                Showtime s=showList.get(i);
                System.out.printf("[%d] %-12s %-10s %-10.2f %-10d\n", i+1, s.date, s.time,s.ticketPrice, s.availableSeats);
            }

            System.out.print("\n>>> Select Showtime number (or 'BACK' to return): ");
            try {
                String inputCode=sc.nextLine().trim().toUpperCase();
                if (inputCode.equals("BACK")) return;

                int timeChoice=Integer.parseInt(inputCode)-1;

                if (timeChoice<0 || timeChoice>=showList.size()) {
                    throw new InvalidShowtimeSelectionException(inputCode);
                } else {
                    selectedShow = showList.get(timeChoice);
                }
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a valid number for the showtime.");
            } catch (InvalidShowtimeSelectionException e) {
                System.out.println(e.getMessage());
            }
        }

        while (true) {
            try {
                System.out.print(">>> Enter number of tickets to buy: ");
                int tickets=Integer.parseInt(sc.nextLine());

                if (tickets<=0) {
                    System.out.println("Please enter a positive number of tickets.");
                    continue;
                }

                if (tickets>selectedShow.availableSeats) {
                    throw new OverbookingException( selectedShow.availableSeats);
                }

                selectedShow.availableSeats-=tickets;
                double totalCost=tickets*selectedShow.ticketPrice;
                System.out.printf("Success! Total cost: %.2f.\n", totalCost);
                break;

            } catch (OverbookingException e) {
                System.out.println(e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a numeric value for tickets.");
            }
        }
    }

    public void eventLoop(){
        while(choice!=2){
            displayMenu();

            try{
                String input=sc.nextLine();
                timer.cancel();
                timer.schedule(task,TIMEOUT );
                choice=Integer.parseInt(input);

                if(choice!=1 && choice!=2){
                    throw new InvalidMenuChoiceException(input);
                }
                switch(choice){
                    case 1:browseMovies();break;
                    case 2:System.out.println("Exiting system...");break;
                }
            } catch(NumberFormatException e){
                System.out.println("Error: Please enter a numeric value.");
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