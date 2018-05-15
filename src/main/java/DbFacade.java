import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by stopp on 4/13/2018.
 */

public class DbFacade implements AutoCloseable {

    private Connection conn = null;
    private String url;
    private String username;
    private String password;

    // I learned GIT [DAVE - (253 -778 -0985)
    public DbFacade() throws SQLException {
        openDB();
    }

    private void openDB() throws SQLException {
        // Connect to the database
        //url = "jdbc:mariadb://mal.cs.plu.edu:3306/367_2018_yellow";
        String url = "jdbc:mysql://127.0.0.1:2000/367_2018_yellow";
        String username = "yellow_2018";
        String password = "367rocks!";

        conn = DriverManager.getConnection(url, username, password);
    }


    public Connection getConn() {
        return conn;
    }

    //close DB connection
    public void close() {
        try {
            if(conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e);
        }
        conn = null;
    }

    //add new review to table
    public boolean addReview(String userID, String isanID, String comments, String rating) {
        Date date = new Date();
        Object param = new Timestamp(date.getTime());

        try {
            String sql = "INSERT INTO review (dateTime, User_ID, isanID, comments, rating) VALUES(?, ?, ?, ?, ?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setObject(1, param);
            pstmt.setString(2, userID);
            pstmt.setString(3, isanID);
            pstmt.setString(4, comments);
            pstmt.setString(5, rating);
            int count = pstmt.executeUpdate();
            if(count > 0)
                return true;
            else
                return false;
        } catch (SQLException e) {
            System.out.println("insert error:{" + e.getMessage() + "}");
            return false;
        }

    }

    //add new user to table
    public boolean addUser(String fname, String lname, String userID,
                         String uPass, int type, int blocked) {
        try {
            String sql = "INSERT INTO user VALUES (?,?,?,?,?,?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, fname);
            pstmt.setString(2, lname);
            pstmt.setString(3, userID);
            pstmt.setString(4, uPass);
            pstmt.setInt(5, type);
            pstmt.setInt(6, blocked);
            int count = pstmt.executeUpdate();
            if(count > 0)
                return true;
            else
                return false;
        } catch (SQLException e) {
            System.out.println("insert error:{" + e.getMessage() + "}");
            return false;
        }
    }

    //add new movie to table
    public boolean addMovie(String title, String isanID, String genre, String mpaa, String lang, Time length, int date) {
        try {
            String sql = "INSERT INTO movie(title, isan_ID, genre, MPAA_rating, language, length, date) VALUES( ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, title);
            pstmt.setString(2, isanID);
            pstmt.setString(3, genre);
            pstmt.setString(4, mpaa);
            pstmt.setString(5, lang);
            pstmt.setTime(6, length);
            pstmt.setInt(7, date);
            int count = pstmt.executeUpdate();
            if(count > 0)
                return true;
            else
                return false;
        } catch (SQLException e) {
            System.out.println("insert err:{" + e.getMessage() + "}");
            return false;
        }

    }

    //counts the users who have accessed a movie
    public ResultSet countViews(String isanID) {
        ResultSet r = null;
        try {
            String sql =   "SELECT COUNT(*) FROM movie_user WHERE ISAN_ID = ? ";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, isanID);
            r = pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Report Views failed: " + e.getMessage());
        }
        return r;
    }
    //need add UPDATE review

    // SELECT movie by genre and ratings given have at least 1 rating higher than given
    //may update to average review when more reviews have populated
    public ResultSet selectByGenreAndRating(String genre, int rating) {
        ResultSet r = null;

        try {
        String sql = "SELECT * FROM movie, review "+
                "WHERE movie.ISAN_ID=review.isanID AND movie.genre LIKE ? AND review.rating >= ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, "%"+genre+"%");
            pstmt.setInt(2, rating );
            r = pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Report Views failed: " + e.getMessage());
        }
        return r;
    }

    //select reviews by comment length (> or <)
    public ResultSet selectReviewByCommentLength(int length, boolean lessThan) {
        ResultSet r = null;
        String sql;
        try {
            if(lessThan) {
                sql = "SELECT * FROM review WHERE LENGTH(review.comments) < ?";
            }else {
                sql = "SELECT * FROM review WHERE LENGTH(review.comments) > ?";
            }
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setInt(1, length);
            r = pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Report Views failed: " + e.getMessage());
        }
        return r;
    }

    //populate the movie table with data from csv
    public void resetMovieTable() {
        String fName = "C:/Users/stopp/Documents/movieCSV/movieTestCase.csv";
        FileParse fp = new FileParse(fName);
        ArrayList<String[]> list = fp.parseCSV(28);
        DBPopulation pop = new DBPopulation(url,username,password);
        pop.populateMovies();

    }

    public int authenticateUser( String username, String password ) throws SQLException {
        String sql = "SELECT user_type FROM user WHERE " +
                " user_ID = ? AND " +
                " password = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, username);
        pstmt.setString(2, password);
        ResultSet rset = pstmt.executeQuery();
        try {
            rset.next();
            return Integer.parseInt(rset.getString(1));
        }catch(NumberFormatException ex){
            System.err.println("dbfacade: " + ex.getMessage());
            return 0;
        }catch (NullPointerException np){
            System.err.println("dbfacade: " + np.getMessage());
            return 0;
        }
    }

    public Boolean createNewUser(String fname, String lname, String id, String pwd){
        return true;
    }
}