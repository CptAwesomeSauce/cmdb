import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by stopp on 4/20/2018.
 */


/**
 * class parses a csv file into an array by value in a line
 * when a line of data is parsed the line is added to an ArrayList
 */
public class FileParse {
    String fname;
    int col;
    int set;
    ArrayList<String[]> rowList;

    public FileParse(String fname){
        this.fname = fname;
        this.col = 0;
        this.set = 0;
        rowList = new ArrayList<String[]>();
    }

    //get the parsed csv
    public ArrayList<String[]>  parseCSV(int colLength){
        //at fin return array list of strings
        String[] thisSet = new String[colLength];
        String field = "";
        boolean inString = false;
        File file = new File(fname);

        if (!file.exists()) {
            System.out.println(fname + " does not exist.");
            return rowList;
        }
        if (!(file.isFile() && file.canRead())) {
            System.out.println(file.getName() + " cannot be read from.");
            return rowList;
        }
        try {
            Scanner scan = new Scanner(file);
            char current;
            String line;
            while (scan.hasNextLine()) {
                line = "";
                line = scan.nextLine();
                line += ",";

                for (int i = 0; i < line.length(); i++) {

                    current = line.charAt(i);
                    //use current char for comarison / save to field

                    //if the char is a comma increment column field
                    if (current == ',' && !inString) {
                        if(current == '"'){
                            if(inString == false)
                                inString = true;
                            else
                                inString = false;
                        }else{
                            thisSet[col] = field;
                            col++;
                            if (col >= colLength) {
                                col = 0;
                                set++;
                                rowList.add(thisSet);
                                thisSet = new String[colLength];
                            }
                        }

                        field = "";
                    } else {//else add the char to the current column value for this set
                        field += current;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rowList;
    }
}
