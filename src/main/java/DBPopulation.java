import java.sql.*;
import java.util.ArrayList;

/**
 * Created by stopp on 4/24/2018.
 */
public class DBPopulation {
    Connection conn;
    String url;
    String username;
    String password;
    public DBPopulation(String url, String username, String password){
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public boolean createTables(){
        return true;
    }

    public boolean populateMovies(){
        ArrayList<String[]> inList = new ArrayList<String[]>();
        FileParse fp = new FileParse("C:/Users/stopp/Documents/movieCSV/movie.csv");
        inList = fp.parseCSV(28);
        Time t;
        int year;

        //do not load first record...it is the title of csv columns
        for(int i = 1210; i < inList.size();i++) {
            try {
                try{
                    t = new Time(Integer.parseInt(inList.get(i)[23]));
                }catch(NumberFormatException e){
                    t = new Time(5400000);//std apx 90min
                }
                try{
                    year = Integer.parseInt(inList.get(i)[23]);
                }catch(NumberFormatException e){
                    year = 9999;
                }
                String sql = "INSERT INTO movie(title, ISAN_ID, genre, MPAA_Rating, language, length, date) VALUES( ?, ?, ?, ?, ?, ?, ?);";
                conn = DriverManager.getConnection(url, username, password);
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.clearParameters();
                pstmt.setString(1, inList.get(i)[11]);
                pstmt.setString(2, Integer.toString(i));
                pstmt.setString(3, inList.get(i)[9]);
                pstmt.setString(4, inList.get(i)[21]);
                pstmt.setString(5, inList.get(i)[19]);
                pstmt.setTime(6, t);
                pstmt.setInt(7, year);
                System.out.println("round:"+i+ "; date val:"+year);
                pstmt.executeUpdate();
                conn.close();
            } catch (SQLException e) {
                System.out.println("insert err:{" + e.getMessage() + "}");
            }
        }
        return true;
    }
}
