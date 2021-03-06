import javax.xml.transform.Result;
import javax.xml.ws.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;


public class DbFacade implements AutoCloseable {

    private Connection conn = null;
    private String url;
    private String username;
    private String password;


    public DbFacade() throws SQLException {
        openDB();
    }

    private void openDB() throws SQLException {
        // Connect to the database
        String url = "jdbc:mariadb://mal.cs.plu.edu:3306/367_2018_yellow";
        //String url = "jdbc:mysql://127.0.0.1:2000/367_2018_yellow";
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

    //get info about a movie
    public ResultSet getMovieInfo(String ISAN) throws SQLException {
        String sql="SELECT title, movie.ISAN_ID, genre, MPAA_Rating, language, length, date, COUNT(DISTINCT(movie_user.User_ID)) " +
                "FROM movie, movie_user " +
                "WHERE movie.ISAN_ID = movie_user.ISAN_ID AND movie.ISAN_ID = ? ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, ISAN);
        return pstmt.executeQuery();
    }

    public ResultSet getMovieISN(String tit) throws SQLException {
        String sql="SELECT title , ISAN_ID FROM movie " +
                "WHERE title LIKE ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, "%"+tit+"%");

        return pstmt.executeQuery();
    }

    //add new review to table
    public boolean addReview(String userID, String isanID, String comments, String rating) {
        Date date = new Date();
        Object param = new Timestamp(date.getTime());

        try {
            String sql = "INSERT INTO review (dateTime, User_ID, isanID, comments, rating, reviewed) " +
                    "VALUES(?, ?, ?, ?, ?,?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setObject(1, param);
            pstmt.setString(2, userID);
            pstmt.setString(3, isanID);
            pstmt.setString(4, comments);
            pstmt.setString(5, rating);
            pstmt.setString(6, "0");
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
            String sql = "INSERT INTO user VALUES (?,?,?,?,?,?,SHA2(?,256));";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, fname);
            pstmt.setString(2, lname);
            pstmt.setString(3, userID);
            pstmt.setString(4, "no longer used");
            pstmt.setInt(5, type);
            pstmt.setInt(6, blocked);
            pstmt.setString(7, uPass);
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
    public ResultSet countViews(String isanID, String uID) {
        ResultSet r = null;
        try {
            String sql = "SELECT COUNT(DISTINCT(User_ID)) FROM movie_user WHERE ISAN_ID = ? AND User_ID = ? ";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, isanID);
            pstmt.setString(2, uID);
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
                sql = "SELECT review.comments, review.rating, movie.title, review.dateTime, " +
                        "review.reviewed, movie.ISAN_ID, review.User_ID" +
                        " FROM review, movie " +
                        "WHERE movie.ISAN_ID = review.isanID AND LENGTH(review.comments) <= ?";
            }else {
                sql = "SELECT review.comments, review.rating, movie.title, review.dateTime, " +
                        "review.reviewed, movie.ISAN_ID, review.User_ID" +
                        " FROM review, movie " +
                        "WHERE movie.ISAN_ID = review.isanID AND LENGTH(review.comments) >= ?";
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
                " password_hash = SHA2(?,256) AND " +
                "blocked = 0";
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

    public Boolean checkUserName(String uID)throws SQLException{
        String sql = "SELECT COUNT(*) AS rowcount FROM user WHERE " +
                " user_ID = ? ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);

        ResultSet rset = pstmt.executeQuery();
        rset.next();
        if(rset.getInt("rowcount") == 0){
            return true;
        }else {
            return false;
        }
    }

    public Boolean createNewUser(String fname, String lname, String id, String pwd)throws SQLException{
        String sql = "INSERT INTO user (fname,lname,user_ID,password,user_type,blocked,password_hash)" +
                "VALUES(?,?,?,?,?,?,SHA2(?,256))";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, fname);
        pstmt.setString(2, lname);
        pstmt.setString(3, id);
        pstmt.setString(4, "not used");
        pstmt.setString(5, "1");
        pstmt.setString(6, "0");
        pstmt.setString(7, pwd);

        ResultSet rset = pstmt.executeQuery();
        
        return true;
    }

    public ResultSet getReviews(String ISAN)throws SQLException{
        String sql = "SELECT review.comments, review.rating, movie.title FROM review, movie " +
                "WHERE isanID = ? AND review.isanID = movie.ISAN_ID AND review.reviewed = 1";
        System.err.println("in get reviews... ISN = " +ISAN);
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, ISAN);
        return pstmt.executeQuery();

    }

    public ResultSet searchByGenre(String gen){

        ResultSet r = null;
        //If the genre is other, we want to search for movies that
        //are all of the genres other than the ones on the homepage
        if(gen.compareTo("other") == 0){
            try {
                String sql = "SELECT * " +
                        "FROM movie "+
                        "WHERE LOWER(movie.genre) NOT LIKE '%horror%' " +
                        "AND LOWER(movie.genre) NOT LIKE '%comedy%' " +
                        "AND LOWER(movie.genre) NOT LIKE '%romance%' " +
                        "AND LOWER(movie.genre) NOT LIKE '%action%' ";
                Statement stmt = conn.createStatement();
                r = stmt.executeQuery(sql);
            } catch (SQLException e) {
                System.out.println("Report Views failed: " + e.getMessage());
            }
            return r;
        }

        try {
            String sql = "SELECT * " +
                    "FROM movie "+
                    "WHERE LOWER(movie.genre) LIKE LOWER(?) ";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, "%"+gen+"%");
            r = pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Report Views failed: " + e.getMessage());
        }
        return r;

    }

    public Boolean changeUserStatus(String uID, int type) throws SQLException{
        String sql = null;
        ResultSet rset = null;
        sql = "UPDATE user " +
                "SET user_type = ? " +
                "WHERE user_ID = ? ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setInt(1, type);
        pstmt.setString(2, uID);

        if(pstmt.executeUpdate() > 0)
            return true;
        return false;

    }

    public ResultSet checkReviewed() throws SQLException{
        String sql = null;
        ResultSet rset = null;
        Statement stmt = conn.createStatement();
        sql = "SELECT r.User_ID, m.title, r.comments, r.dateTime, r.rating, r.reviewed, r.isanID " +
                "FROM review r, movie m " +
                "WHERE r.isanID = m.ISAN_ID AND r.reviewed = 0 ";
        rset = stmt.executeQuery(sql);
        return rset;


    }

    public Boolean updateReviewStatus(String uID, String isan, int status)throws SQLException{
        String sql = null;
        ResultSet rset = null;
        sql = "UPDATE review " +
                "SET reviewed = ? " +
                "WHERE User_ID = ? AND isanID = ? ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setInt(1, status);
        pstmt.setString(2, uID);
        pstmt.setString(3,isan);

        if(pstmt.executeUpdate() > 0)
            return true;
        return false;

    }

    public Boolean editReview(String uID, String isan, String newrating, String newcomment) throws SQLException{
        String sql = null;
        ResultSet rset = null;

        Date date = new Date();
        Object time = new Timestamp(date.getTime());

            sql = "UPDATE review " +
                    "SET rating = ? , comments = ? , dateTime = ? " +
                    "WHERE User_ID = ? AND isanID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setString(1, newrating);
            pstmt.setString(2, newcomment);
            pstmt.setObject(3, time);
            pstmt.setString(4, uID);
            pstmt.setString(5, isan);
            if(pstmt.executeUpdate() > 0)
                return true;
            return false;
            }

    public Boolean deleteReview(String uID, String isan, int status) throws SQLException{
        String sql = null;
        ResultSet rset = null;
        sql = "DELETE FROM review " +
                "WHERE User_ID = ? AND isanID = ? AND reviewed = ? ";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);
        pstmt.setString(2, isan);
        pstmt.setInt(3,status);

        if(pstmt.executeUpdate() > 0)
            return true;
        return false;
    }

    public Boolean deleteUser(String uID, int delPriv) throws SQLException{
        String sql = null;
        sql = "Update user " +
                "SET blocked = 1 " +
                "WHERE user_ID = ? AND user_type = ? ";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);
        pstmt.setInt(2, delPriv);

        if(pstmt.executeUpdate() > 0)
            return true;
        return false;
    }

    public Boolean adminBanUser(String uID) throws SQLException{
        String sql = null;
        sql = "Update user " +
                "SET blocked = 1 " +
                "WHERE user_ID = ? ";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);

        if(pstmt.executeUpdate() > 0)
            return true;
        return false;
    }

    public ResultSet getMyReviews(String uID) throws SQLException{
        String sql = "SELECT review.comments, review.rating, movie.title, review.dateTime, review.reviewed, movie.ISAN_ID " +
                "FROM review, movie " +
                "WHERE User_ID = ? AND review.isanID = movie.ISAN_ID ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);
        return pstmt.executeQuery();
    }

    public ResultSet getMyOneReviews(String uID, String mID) throws SQLException{
        String sql = "SELECT review.comments, review.rating, movie.title, review.dateTime, review.reviewed, " +
                "movie.ISAN_ID, review.User_ID " +
                "FROM review, movie " +
                "WHERE User_ID = ? AND review.isanID = ? AND review.isanID = movie.ISAN_ID";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);
        pstmt.setString(2, mID);
        return pstmt.executeQuery();
    }

    public ResultSet getAllReviews() throws  SQLException{
        String sql = "SELECT review.comments, review.rating, movie.title, review.dateTime, " +
                "review.reviewed, movie.ISAN_ID, review.User_ID " +
                "FROM review, movie " +
                "WHERE review.isanID = movie.ISAN_ID ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        return pstmt.executeQuery();
    }

    public Boolean addView(String uID, String isan){
        try {

            String sql = "INSERT INTO movie_user (ISAN_ID, USER_ID) " +
                    "VALUES(?, ?) ";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.clearParameters();
            pstmt.setObject(1, isan);
            pstmt.setString(2, uID);
            int count = pstmt.executeUpdate();
            if (count > 0)
                return true;
            else
                return false;
        }catch (SQLException e){
            return false;
        }

    }

    public int checkBlocked(String uID) throws SQLException{
        String sql = "SELECT blocked " +
                "FROM user " +
                "WHERE user_ID = ? ";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.clearParameters();
        pstmt.setString(1, uID);
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
}