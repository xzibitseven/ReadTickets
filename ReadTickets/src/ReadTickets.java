package readtickets;

import java.util.Objects;
import java.io.FileReader;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.FileNotFoundException;
import java.io.IOException;
 
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.json.simple.JsonArray;
import org.json.simple.DeserializationException;
import java.text.ParseException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;


/*
 * We will assume that the departure time from Vladivostok is indicated in the "tickets.json" file in local time (GTM+10)
 * and the arrival time to Tel Aviv in local time (GTM+3).
 */

public class ReadTickets {
    
    private final char utf8Bom = '\uFEFF';
    private final int timeZone = 7; // GTM+7
    private final String fileName;
    // time zone difference between Vladivostok and Tel-Aviv in hours
    private final int differenceInTimeZones;
    
    ReadTickets() {
        fileName = "tickets.json";
        differenceInTimeZones = timeZone;
    }
    
    ReadTickets(String fileName) {
        this.fileName = fileName;
        differenceInTimeZones = timeZone;
    }
    
    ReadTickets(String fileName, int differenceInTimeZones) {
        this.fileName = fileName;
        this.differenceInTimeZones = differenceInTimeZones;
    }
  
    public String getFileName() {
        return fileName;
    }
    
    public int getDifferenceInTimeZone() {
        return differenceInTimeZones;
    }
    
    /*
     * The method reads data from a file. If there is a utf8-bom character at the beginning of the file, deletes it.
     * Returns a String containing all the data from the file.
     */
    protected String readJsonFile(final String fileName) throws IOException {
    
        FileReader fileReader = null;
        String buffer = null;
        try {
            File file = new File(fileName);
            fileReader = new FileReader(file, StandardCharsets.UTF_8);
            
            char[] rawBuffer = new char[(int) file.length()];
            
            int dataLength = 0;
            if (fileReader.ready()) {
                dataLength = fileReader.read(rawBuffer);
            } else {
                throw new IOException("Exception: The function 'fileReader.read(data)' stream isn't ready to be read!");
            }
                                    
            buffer = new String(rawBuffer, 0, dataLength);
            if (buffer.charAt(0) == utf8Bom) {
                buffer = buffer.substring(1);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            try {
                if(fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException ex) {
                throw ex;
            }
        }
        
        
        return buffer;
    }
    
    /*
     * The method reads data from the buffer in Json format, parses them and stores in an array the duration of each flight in milliseconds.
     * Returns an array of the duration of each flight in milliseconds.
     */
    protected ArrayList<Long> getFlightTime(final String buffer) throws DeserializationException, ParseException {
    
        JsonObject jsonObject = (JsonObject) Jsoner.deserialize(buffer);
        JsonArray tickets = (JsonArray) jsonObject.get("tickets");
            
        ArrayList<Long> flightsTime = new ArrayList<>(tickets.size());
        String pattern = "dd.MM.yy HH:mm";
            
        for(Iterator it=tickets.iterator(); it.hasNext(); ) {
            JsonObject object = (JsonObject) it.next();
            
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            Date departure = format.parse((String) object.get("departure_date") + " " + (String) object.get("departure_time"));
            Date arrival = format.parse((String) object.get("arrival_date") + " " + (String) object.get("arrival_time") );            
            flightsTime.add((arrival.getTime() - departure.getTime()));
        }
            
        return flightsTime;
    }
    
    /*
     * The method calculates the average flight time and returns a string with the average flight time in hours and minutes.
     */
    protected String getAverageFlightTime(final ArrayList<Long> flightsTime) throws IOException {
    
        if(flightsTime.isEmpty()) {
            return "";
        }
    
        flightsTime.sort(Comparator.naturalOrder());
        long sum = flightsTime.stream().reduce(Long::sum).get();
        
        // calculate the average value
        long average = sum / flightsTime.size();
        
        return "Average flight time between Vladivostok and Tel-Aviv: "
               + getTimeFromMilliseconds(average);
    }
    
    /*
     * The method calculates the 90th percentile of flight time
     */
    protected String get90thPercentileFlightTime(final ArrayList<Long> flightsTime) throws IOException {
    
        if(flightsTime.isEmpty()) {
            return "";
        }
        
        // calculate the index of the value in the array, which is equal to the 90th percentile
        int index = (90 * flightsTime.size()) / 100;
        
        return "90th percentile of flight time between Vladivostok and Tel-Aviv (in minutes): " 
               + getTimeFromMilliseconds(flightsTime.get(index).longValue());
    }

    /*
     * The method takes the time in the format of a single long value in milliseconds, and returns a string with the time in the format: <hh> hours <mm> minutes.
     */
    protected String getTimeFromMilliseconds(long time) {
    
        int minutes = (int) time / (1000 * 60);
        int hours = (minutes / 60);
        minutes %= 60;
        hours += differenceInTimeZones;
        
        String result = hours + " hours " + minutes;
        result += (minutes > 1) ?  " minutes." : " minute."; 
        return result;
    }
    
    /*
     * Creates an instance of the Read Tickets class
     */
    public static ReadTickets createInstance(final String args[], final CommandLineArguments arguments) throws ParameterException {
    
        if ((args.length > 0) && (arguments.getFileName().length() > 0)) {
            final File file = new File(arguments.getFileName());
            if ( file.exists() && !file.isDirectory() ) {
                return new ReadTickets(arguments.getFileName());
            } else {
                throw new ParameterException("ParameterException: An invalid value was passed for the file name and path. The file does not exist!");
            }
        } else {
            return new ReadTickets();
        }
    }
    
    public static void main(String args[]) {
    
        try {
        
            CommandLineArguments arguments = new CommandLineArguments();
            final JCommander cmd = JCommander.newBuilder()
                                             .programName("ReadTickets")
                                             .addObject(arguments)
                                             .build();
            
            cmd.parse(args);
            
            if (arguments.getHelp()) {
                cmd.usage();
            } else {
            
                ReadTickets readTickets = createInstance(args, arguments);
                
                String buffer = readTickets.readJsonFile(readTickets.fileName);
                ArrayList<Long> flightsTime =  readTickets.getFlightTime(buffer);
          
                System.out.println(readTickets.getAverageFlightTime(flightsTime));
                System.out.println(readTickets.get90thPercentileFlightTime(flightsTime));
            }
                        
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace(); 
        } catch (DeserializationException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (ParameterException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    } 
}

/*
 * A class describing expected command-line arguments, for parsing.
 */
class CommandLineArguments {

    @Parameter(names={"-h", "--help"}, description="Help/Usage", help=true, order=0)
    private boolean help;
    
    @Parameter(names={"-i","--inputFile <file name>"}, description="Path and file name with data in JSON format.", order=1)
    private String fileName;
    
    public boolean getHelp() {
        return help;
    }
    
    public String getFileName() {
        return fileName;
    }
}
