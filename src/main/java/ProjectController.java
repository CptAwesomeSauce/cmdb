import spark.Request;
import spark.Response;
import spark.Session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.halt;

public class ProjectController {



    public Object displayHome(Request req, Response resp){ return runner.renderTemplate(null, "homepage.hbs"); }

    public Object getUserHome(Request req, Response resp) { return runner.renderTemplate(null, "user-home.hbs"); }

    public Object getModHome(Request req, Response resp) { return runner.renderTemplate(null, "admin-home.hbs"); }

    public Object getAdminHome(Request req, Response resp) { return runner.renderTemplate(null, "admin-home.hbs"); }

    public Object getMovieInfo(Request req, Response resp) { return runner.renderTemplate(null, "movie-item.hbs"); }

    public Object getUserReview(Request req, Response resp) {return runner.renderTemplate(null, "review-list.hbs"); }

    public Object getNewUserForm(Request req, Response resp) {return runner.renderTemplate(null, "new-user-form.hbs"); }

    public Object getMovieList(Request req, Response resp) {
        String title = req.queryParams("title_field");
        try(DbFacade db = new DbFacade()) {
            ResultSet rset = db.getMovieISN(title);
            ArrayList<Map<String,String>> movies = new ArrayList<>();
            while(rset.next()) {
                Map<String,String> row = new HashMap<>();
                row.put("title", rset.getString(1));
                row.put("ISAN_ID", rset.getString(2));
                movies.add(row);
            }
            Map<String,Object> data = new HashMap<>();
            data.put("movies",movies);
            return runner.renderTemplate(data,"movie-list-partial.hbs");
        }catch(SQLException e) {
            resp.status(500);
            System.err.println("getMovieList: " + e.getMessage());
            return "";
        }
    }
    public Object postLoginForm(Request req, Response resp) {
        String uname = req.queryParams("login_field");
        String pwd = req.queryParams("pword_field");
        try(DbFacade db = new DbFacade()) {
            int result = db.authenticateUser(uname, pwd);
            Session sess = req.session();
            sess.attribute("username", uname);
            sess.attribute("auth", true);
            if(result  == 1){
                sess.attribute("type", 1);
                return runner.renderTemplate(null, "suc-user.hbs");
            }else if(result == 2){
                sess.attribute("type", 2);
                return runner.renderTemplate(null, "suc-mod.hbs");
            }else if(result == 3) {
                sess.attribute("type", 2);
                return runner.renderTemplate(null, "suc-admin.hbs");
            }else{
                sess.attribute("type", 0);
                Map<String,Object> data = new HashMap<>();
                data.put("errorMsg", "Login failed!");
                return runner.renderTemplate(data, "homepage.hbs");
            }

        } catch(SQLException e) {
            resp.status(500);
            System.err.println("postLoginForm: " + e.getMessage());
            return "";
        }
    }

    public Object displayGenrePost(Request req, Response resp){
        String titleIn = req.queryParams("title_field");
        String genreIn = req.queryParams("genre_field");

        //If the user checked the "other" button, we want to redirect them
        //to another page where they can enter another genre.
        if (genreIn.compareTo("other") == 0)
            return runner.renderTemplate(null, "otherGenreForm.hbs");
        else{

        }
        //else display steven's page with the list of movies that match
        return null;
    }

    public Object releaseLogin(Request req, Response resp){
        req.session().attribute("username", "");
        req.session().attribute("auth", false);
        req.session().attribute("type", 0);
        Map<String,Object> data = new HashMap<>();
        data.put("message", "Logged out successfully");
        return runner.renderTemplate(data, "homepage.hbs");
    }

    public void userBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if( auth == null || (!auth) ) {
            if(type != 1)
                halt(401, "Access denied");
        }
    }

    public void modBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if( auth == null || (!auth) ) {
            if(type != 2)
                halt(401, "Access denied");
        }
    }

    public void adminBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if( auth == null || (!auth) ) {
            if(type != 3)
                halt(401, "Access denied");
        }
    }

    public Object createNewUser(Request req, Response resp) {
        String fname = req.queryParams("fname");
        String lname = req.queryParams("lname");
        String uID = req.queryParams("userid");
        String pwd1 = req.queryParams("pword");
        String pwd2 = req.queryParams("pwordconf");

        if (pwd1.equals(pwd2)) {
            try (DbFacade db = new DbFacade()) {

                if(db.checkUserName(uID).equals(true)) {
                    Boolean result = db.createNewUser(fname, lname, uID, pwd1);
                    Session sess = req.session();
                    sess.attribute("username", uID);
                    sess.attribute("auth", true);
                    sess.attribute("type", 1);
                    Map<String, Object> data = new HashMap<>();
                    data.put("msg", "success");
                    return runner.renderTemplate(data, "suc-new-user.hbs");
                }else{
                    Map<String,Object> data = new HashMap<>();
                    data.put("msg", "Create failed!");
                    return runner.renderTemplate(data, "new-user-form.hbs");
                }
            } catch (SQLException ex) {
                Map<String,Object> data = new HashMap<>();
                data.put("msg", "Create failed!");
                return runner.renderTemplate(data, "new-user-form.hbs");
            }
        }else{
            Map<String,Object> data = new HashMap<>();
            data.put("msg", "password do not match!");
            return runner.renderTemplate(data, "new-user-form.hbs");
        }
    }

    public Object getMovieListGenre(Request req, Response resp) {
        String genreIn = req.queryParams("genre_field");

        try (DbFacade db = new DbFacade()) {
            ResultSet rset = db.searchByGenre(genreIn);

            ArrayList<Map<String, String>> movies = new ArrayList<>();
            while (rset.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("title", rset.getString(1));
                row.put("ISAN_ID", rset.getString(2));
                row.put("genre", rset.getString(3));
                row.put("MPAA_Rating", rset.getString(4));
                row.put("language", rset.getString(5));
                row.put("length", rset.getString(6));
                row.put("date", rset.getString(7));
                movies.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("movies", movies);
            return runner.renderTemplate(data, "movie-list-partial.hbs");

        } catch (SQLException e) {
            resp.status(500);
            System.err.println("getMovieList: " + e.getMessage());
            return "";
        }

    }

    public Object promoteDemoteForm(Request req, Response resp){
        return runner.renderTemplate(null, "promoteDemoteForm.hbs");
    }

    public Object promoteDemotePost(Request req, Response resp){
        String IDIn = req.queryParams("ID_field");
        String typeIn = req.queryParams("priv_field");

        try(DbFacade db = new DbFacade()){
            boolean updated;
            updated = db.changeUserStatus(IDIn, Integer.parseInt(typeIn));

            if(updated) {
                Map<String,Object> data = new HashMap<>();
                data.put("Msg", "User Status Successfully Updated!");
                return runner.renderTemplate(data, "promoteDemoteForm.hbs");
            }

        }catch (SQLException e){
            resp.status(500);
            System.err.println("getMovieList: " + e.getMessage());
            return "";
        }

        Map<String,Object> data = new HashMap<>();
        data.put("Msg", "Failed, Status Not Updated.");
        return runner.renderTemplate(data, "promoteDemoteForm.hbs");

    }

//    public Object reviewCheck(Request req, Response resp){
//
//    }


    public Object getUserReviews(Request req, Response resp){
        try(DbFacade db = new DbFacade()){
            ResultSet rset = db.getReviews(req.params(":ISN"));
            ArrayList<Map<String,String>> reviews = new ArrayList<>();
            while(rset.next()) {
                Map<String,String> row = new HashMap<>();
                row.put("title", rset.getString(3));
                row.put("rating", rset.getString(2));
                row.put("comments", rset.getString(1));
                reviews.add(row);
            }
            Map<String,Object> data = new HashMap<>();
            data.put("reviews",reviews);
            return runner.renderTemplate(data,"review-list-partial.hbs");
        }catch(SQLException ex){
            System.err.println("getUserReviews:" + ex.getMessage());
            return runner.renderTemplate(null,"review-list.hbs");
        }
    }

    public Object getMovies(Request req, Response resp) throws SQLException{
        try (DbFacade db = new DbFacade()){
            ResultSet rset = db.getMovieInfo(req.params(":ISN"));

            ArrayList<Map<String,String>> movies = new ArrayList<>();
            while(rset.next()){
                Map<String,String> row = new HashMap<>();
                row.put("title", rset.getString(1));
                row.put("ISAN_ID", rset.getString(2));
                row.put("genre", rset.getString(3));
                row.put("MPAA_Rating", rset.getString(4));
                row.put("language", rset.getString(5));
                row.put("length", rset.getString(6));
                row.put("date", rset.getString(7));
                movies.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("movie", movies);
            try {
                if (req.session().attribute("auth").equals(true)) {
                    return runner.renderTemplate(data, "movie-infoU.hbs");
                } else {
                    return runner.renderTemplate(data, "movie-infoN.hbs");
                }
            }catch(NullPointerException ex){
                System.err.println("getMovies:"+ex.getMessage());
                return runner.renderTemplate(data, "movie-infoN.hbs");
            }
        } catch(SQLException e) {
            resp.status(500);
            System.err.println("postLoginForm: " + e.getMessage());
            return "";
        }
    }

}
