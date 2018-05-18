import com.sun.xml.internal.bind.v2.model.core.ID;
import spark.Request;
import spark.Response;
import spark.Session;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static spark.Spark.halt;

public class ProjectController {

    public Object displayHome(Request req, Response resp){ return runner.renderTemplate(null, "homepage.hbs"); }

    public Object getUserHome(Request req, Response resp) { return runner.renderTemplate(null, "user-home.hbs"); }

    public Object getModHome(Request req, Response resp) { return runner.renderTemplate(null, "mod-home.hbs"); }

    public Object getAdminHome(Request req, Response resp) { return runner.renderTemplate(null, "admin-home.hbs"); }

    public Object getMovieInfo(Request req, Response resp) { return runner.renderTemplate(null, "movie-item.hbs"); }

    public Object getUserReview(Request req, Response resp) {return runner.renderTemplate(null, "review-list.hbs"); }

    public Object getNewUserForm(Request req, Response resp) {return runner.renderTemplate(null, "new-user-form.hbs"); }

    public Object getReview(Request req, Response resp){ return runner.renderTemplate(null, "writeReviewForm.hbs"); }

    public Object editMyReview(Request req, Response resp){
        String userID = req.queryParams("ID_field");
        String mID = req.queryParams("isanID_field");
        try (DbFacade db = new DbFacade()) {
            ResultSet rset = db.getMyOneReviews(userID, mID);
            ArrayList <Map <String, String>> reviews = new ArrayList <>();
            while (rset.next()) {
                Map <String, String> row = new HashMap <>();
                row.put("comments", rset.getString(1));
                row.put("rating", rset.getString(2));
                row.put("title", rset.getString(3));
                row.put("dateTime", rset.getString(4));
                row.put("reviewed", rset.getString(5));
                row.put("isan_ID", rset.getString(6));
                row.put("userID", rset.getString(7));
                reviews.add(row);
            }
            Map <String, Object> data = new HashMap <>();
            data.put("reviews", reviews);
            return runner.renderTemplate(data, "editReviewForm.hbs");
        } catch (SQLException e) {
            resp.status(500);
            System.err.println("Couldn't find your reviews: " + e.getMessage());
            return "";
        }
    }

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
            if (movies.isEmpty()) {
                return runner.renderTemplate(null,"movie-list-empty.hbs");
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
        try (DbFacade db = new DbFacade()) {
            int banned = db.checkBlocked(uname);
            if(banned == 1){
                Map<String, Object> data = new HashMap<>();
                data.put("errorMsg", "This account is locked!");
                return runner.renderTemplate(data, "homepage.hbs");
            }

            int result = db.authenticateUser(uname, pwd);
            Session sess = req.session();
            sess.attribute("username", uname);
            sess.attribute("auth", true);


            if (result == 1) {
                sess.attribute("type", 1);
                return runner.renderTemplate(null, "suc-user.hbs");
            } else if (result == 2) {
                sess.attribute("type", 2);
                return runner.renderTemplate(null, "suc-mod.hbs");
            } else if (result == 3) {
                sess.attribute("type", 3);
                return runner.renderTemplate(null, "suc-admin.hbs");
            } else {
                sess.attribute("type", 0);
                Map<String, Object> data = new HashMap<>();
                data.put("errorMsg", "Login failed!");
                return runner.renderTemplate(data, "homepage.hbs");
            }
        } catch (SQLException e) {
            Map<String, Object> data = new HashMap<>();
            data.put("errorMsg", "Login failed!");
            return runner.renderTemplate(data, "homepage.hbs");
        }
    }

    public Object releaseLogin(Request req, Response resp) {
        req.session().attribute("username", "");
        req.session().attribute("auth", false);
        req.session().attribute("type", 0);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Logged out successfully");
        return runner.renderTemplate(data, "homepage.hbs");
    }

    public void userBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if (auth == null || (!auth)) {
            if (type != 1)
                halt(401, "Access denied");
        }
    }

    public void modBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if (auth == null || (!auth)) {
            if (type != 2)
                halt(401, "Access denied");
        }
    }

    public void adminBeforeFilter(Request req, Response resp) {
        Boolean auth = req.session().attribute("auth");
        int type = req.session().attribute("type");
        if (auth == null || (!auth)) {
            if (type != 3)
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

                if (db.checkUserName(uID)) {
                    Boolean result = db.createNewUser(fname, lname, uID, pwd1);
                }
                Map<String, Object> data = new HashMap<>();
                data.put("msg", "success");
                Session sess = req.session();
                sess.attribute("username", uID);
                sess.attribute("auth", true);
                sess.attribute("type", 1);
                return runner.renderTemplate(data, "suc-new-user.hbs");
            } catch (SQLException ex) {
                Map<String, Object> data = new HashMap<>();
                data.put("msg", "Create failed!");
                System.err.println("createNewUser, failed:" + ex.getMessage());
                return runner.renderTemplate(data, "new-user-form.hbs");
            }
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("msg", "password do not match!");
            System.err.println("createNewUser, pw match failed");
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
            System.err.println("PromoteDemote: " + e.getMessage());
            return "";
        }

        Map<String,Object> data = new HashMap<>();
        data.put("Msg", "Failed, Status Not Updated.");
        return runner.renderTemplate(data, "promoteDemoteForm.hbs");

    }

    public Object displayReviewCheck(Request req, Response resp){
        try(DbFacade db = new DbFacade()){
            ResultSet rset = db.checkReviewed();

            ArrayList<Map<String, String>> reviews = new ArrayList<>();
            while (rset.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("userID", rset.getString(1));
                row.put("title", rset.getString(2));
                row.put("comments", rset.getString(3));
                row.put("dateTime", rset.getString(4));
                row.put("rating", rset.getString(5));
                row.put("reviewed", rset.getString(6));
                row.put("isanID", rset.getString(7));
                reviews.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("reviews", reviews);
            return runner.renderTemplate(data, "modReviewCheckPage.hbs");



        }catch (SQLException e){
            resp.status(500);
            System.err.println("displayReviewCheck: " + e.getMessage());
            return "";

        }

    }

    public Object approveReview(Request req, Response resp){
        String approved = req.queryParams("approval_field");
        String ID = req.queryParams("userID_field");
        String isan = req.queryParams("isanID_field");

        if(Integer.parseInt(approved) == 0){
            try(DbFacade db = new DbFacade()){
                Boolean deleted = false;
                deleted = db.deleteReview(ID, isan, Integer.parseInt(approved));

                if(deleted)
                    return runner.renderTemplate(null, "modReviewDel.hbs");



            }catch(SQLException e){
                resp.status(500);
                System.err.println("approveReviewDelete: " + e.getMessage());
                return "";
            }

            Map<String,Object> data = new HashMap<>();
            data.put("ErrorMsg", "Failed, review unchanged.");
            return runner.renderTemplate(data, "modReviewPage.hbs");
        }


        try(DbFacade db = new DbFacade()){
            Boolean updated;
            updated = db.updateReviewStatus(ID, isan, Integer.parseInt(approved));

            if(updated)
                return runner.renderTemplate(null, "modReviewSuc.hbs");


        }catch (SQLException e){
            resp.status(500);
            System.err.println("approveReviewApprove: " + e.getMessage());
            return "";
        }

        Map<String,Object> data = new HashMap<>();
        data.put("ErrorMsg", "Failed, review unchanged.");
        return runner.renderTemplate(data, "modReviewPage.hbs");

    }

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
            if (reviews.isEmpty()) {
                return runner.renderTemplate(null,"review-list-empty.hbs");
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
                row.put("views", rset.getString(8));
                movies.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("movie", movies);
            try {
                if (req.session().attribute("auth").equals(true)) {
                    db.addView(req.session().attribute("username") ,req.params(":ISN"));
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

    public Object displayFreezeUserForm(Request req, Response resp){
        return runner.renderTemplate(null, "freezeUserForm.hbs");
    }

    public Object modFreezeUser(Request req, Response resp){
        String IDIn = req.queryParams("ID_field");

        try(DbFacade db = new DbFacade()){
            Boolean deleted;

            deleted = db.deleteUser(IDIn, 1);
            //1 is a parameter because a mod can only delete registered users.
            //Mods can't delete other mods or admins.

            if(deleted)
                return runner.renderTemplate(null, "modDelSuc.hbs");

        }catch (SQLException e){
            resp.status(500);
            Map<String,Object> data = new HashMap<>();
            data.put("ErrorMsg", "Failed, can't ban that user, delete their review first.");
            return runner.renderTemplate(data, "freezeUserForm.hbs");
        }
        Map<String,Object> data = new HashMap<>();
        data.put("ErrorMsg", "Failed, can't ban that user.");
        return runner.renderTemplate(data, "freezeUserForm.hbs");
    }

    public Object adminDisplayFreezeUserForm(Request req, Response resp){
        return runner.renderTemplate(null, "adminFreezeUserForm.hbs");
    }

    public Object adminFreezeUser(Request req, Response resp){
        String IDIn = req.queryParams("ID_field");

        try(DbFacade db = new DbFacade()){
            Boolean banned;

            banned = db.adminBanUser(IDIn);
            //This deleteUser method only takes ID because an admin can delete ANYONE

            if(banned)
                return runner.renderTemplate(null, "adminDelSuc.hbs");

        }catch (SQLException e){
            resp.status(500);
            System.err.println("Can't Ban: " + e.getMessage());
            return "";
        }
        Map<String,Object> data = new HashMap<>();
        data.put("ErrorMsg", "Failed, input does not match a user.");
        return runner.renderTemplate(data, "adminFreezeUserForm.hbs");

    }

    public Object getReviewForm(Request req, Response resp){
        ArrayList<Map<String,String>> movies = new ArrayList<>();
        Map<String,String> row = new HashMap<>();
        row.put("ISAN_ID", req.params(":ISN"));
        movies.add(row);
        Map<String, Object> data = new HashMap<>();
        data.put("movie", movies);

        return runner.renderTemplate(data, "writeReviewForm.hbs");
    }

    public Object postReviewSuccess(Request req, Response resp){
        String isanIN = req.params(":ISN");
        String IDIn = req.session().attribute("username");
        String ratingIn = req.queryParams("rating_field");
        String commentsIn = req.queryParams("comment_field");

        try(DbFacade db = new DbFacade()){
            db.addReview(IDIn, isanIN, commentsIn, ratingIn);

            String type = Integer.toString(req.session().attribute("type"));
            if(type.equals("1"))
                return runner.renderTemplate(null, "suc-review-user.hbs");
            else if(type.equals("2"))
                return runner.renderTemplate(null, "suc-review-mod.hbs");
            else if(type.equals("3"))
                return runner.renderTemplate(null, "suc-review-admin.hbs");
            else
                return runner.renderTemplate(null, "homepage.hbs");


        }catch(SQLException e){
            resp.status(500);
            System.err.println("Couldn't add review: " + e.getMessage());
            return runner.renderTemplate(null, "homepage.hbs");
        }

    }

    public Object postEditSuccess(Request req, Response resp){

        //String IDIn = req.session().attribute("username");
        String IDIn = req.queryParams("userID_field");
        String type = Integer.toString(req.session().attribute("type"));
//        if (type.equals("2")) {
//            IDIn = req.queryParams("ID_field");
//        }

        String isanIN = req.queryParams("isanID_field");

        String ratingIn = req.queryParams("new_rating");
        String commentsIn = req.queryParams("new_comment");

        try (DbFacade db = new DbFacade()) {
            boolean edited = db.editReview(IDIn, isanIN, ratingIn, commentsIn);

            if (edited) {
                if (type.equals("1"))
                    return runner.renderTemplate(null, "suc-edit-user.hbs");
                else if (type.equals("2"))
                    return runner.renderTemplate(null, "suc-edit-mod.hbs");
                else if (type.equals("3"))
                    return runner.renderTemplate(null, "suc-edit-admin.hbs");
                else
                    return runner.renderTemplate(null, "homepage.hbs");
            }

        } catch (SQLException e) {
            resp.status(500);
            System.err.println("Couldn't edit review: " + e.getMessage());
            return runner.renderTemplate(null, "homepage.hbs");
        }
        return runner.renderTemplate(null, "homepage.hbs");
    }

    public Object displayMyReviews(Request req, Response resp){
        String type = Integer.toString(req.session().attribute("type"));
        if(type.equals("2")){
            try(DbFacade db = new DbFacade()){
                ResultSet rset = db.getAllReviews();
                ArrayList<Map<String, String>> reviews = new ArrayList<>();
                while (rset.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("comments", rset.getString(1));
                    row.put("rating", rset.getString(2));
                    row.put("title", rset.getString(3));
                    row.put("dateTime", rset.getString(4));
                    row.put("reviewed", rset.getString(5));
                    row.put("isan_ID", rset.getString(6));
                    row.put("userID", rset.getString(7));
                    reviews.add(row);
                }
                if (reviews.isEmpty()) {
                    return runner.renderTemplate(null,"my-review-list-empty.hbs");
                }
                Map<String, Object> data = new HashMap<>();
                data.put("reviews", reviews);
                return runner.renderTemplate(data, "mod-displayReviews.hbs");
            }catch (SQLException e){
                resp.status(500);
                System.err.println("Couldn't find your reviews: " + e.getMessage());
                return "";

            }

        }

        String userID = req.session().attribute("username");
        try(DbFacade db = new DbFacade()){
            ResultSet rset = db.getMyReviews(userID);
            ArrayList<Map<String, String>> reviews = new ArrayList<>();
            while (rset.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("comments", rset.getString(1));
                row.put("rating", rset.getString(2));
                row.put("title", rset.getString(3));
                row.put("dateTime", rset.getString(4));
                row.put("reviewed", rset.getString(5));
                row.put("isan_ID", rset.getString(6));
                row.put("userID", userID);
                reviews.add(row);
            }
            if (reviews.isEmpty()) {
                return runner.renderTemplate(null,"my-review-list-empty.hbs");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("reviews", reviews);
            return runner.renderTemplate(data, "displayMyReviews.hbs");
        }catch (SQLException e){
            resp.status(500);
            System.err.println("Couldn't find your reviews: " + e.getMessage());
            return "";

        }
    }

    public Object deleteMyReview(Request req, Response resp){
        String IDIn = req.session().attribute("username");
        String type = Integer.toString(req.session().attribute("type"));

        if(type.equals("2")){
            IDIn = req.queryParams("ID_field");
        }

        String reviewedIn = req.queryParams("reviewed_field");
        String isanIN = req.queryParams("isanID_field");


        try(DbFacade db = new DbFacade()){
            db.deleteReview(IDIn, isanIN, Integer.parseInt(reviewedIn));
            if(type.equals("1"))
                return runner.renderTemplate(null, "suc-reviewDel-user.hbs");
            else if(type.equals("2"))
                return runner.renderTemplate(null, "suc-reviewDel-mod.hbs");
            else if(type.equals("3"))
                return runner.renderTemplate(null, "suc-reviewDel-admin.hbs");
            else
                return runner.renderTemplate(null, "homepage.hbs");


        }catch(SQLException e){
            resp.status(500);
            System.err.println("Couldn't add review: " + e.getMessage());
            return runner.renderTemplate(null, "homepage.hbs");
        }


    }

    public Object goHome(Request req, Response resp){
        try {
            String type = Integer.toString(req.session().attribute("type"));

            if (type.equals("1"))

                return runner.renderTemplate(null, "user-go-home.hbs");
            else if (type.equals("2"))
                return runner.renderTemplate(null, "mod-go-home.hbs");
            else if (type.equals("3"))
                return runner.renderTemplate(null, "admin-go-home.hbs");
            else
                return runner.renderTemplate(null, "you-go-home.hbs");
        } catch (NullPointerException e) {
            return runner.renderTemplate(null, "you-go-home.hbs");
        }
    }

    public Object adminListReviewsForm(Request req, Response resp){
        return runner.renderTemplate(null, "adminReviewCheckForm.hbs");
    }

    public Object adminReviewPost(Request req, Response resp){
        String commentLength = req.queryParams("length_field");
        String boolIn = req.queryParams("lg_field");
        boolean lg;

        if(boolIn.compareTo("true") == 0)
            lg = true;
        else if(boolIn.compareTo("false") == 0)
            lg = false;
        else
            lg = true;

        try (DbFacade db = new DbFacade()) {
            ResultSet rset = db.selectReviewByCommentLength(Integer.parseInt(commentLength), lg);
            ArrayList<Map<String, String>> reviews = new ArrayList<>();
            while (rset.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("comments", rset.getString(1));
                row.put("rating", rset.getString(2));
                row.put("title", rset.getString(3));
                row.put("dateTime", rset.getString(4));
                row.put("reviewed", rset.getString(5));
                row.put("isan_ID", rset.getString(6));
                row.put("userID", rset.getString(7));
                reviews.add(row);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("reviews", reviews);
            return runner.renderTemplate(data,"adminDisplayReviews.hbs");
        } catch (SQLException e) {
            resp.status(500);
            System.err.println("In adminReviewPost: " + e.getMessage());
            return "";
        }

    }

    public Object modAddMovieForm(Request req, Response resp){
        return runner.renderTemplate(null, "mod-AddMovieForm.hbs");
    }

    public Object modAddMovieSuc(Request req, Response resp){
        String titleIn = req.queryParams("title_field");
        String isanIn = req.queryParams("isan_field");
        String genreIn = req.queryParams("genre_field");
        String mpaaIn = req.queryParams("mpaa_field");
        String lang = req.queryParams("lang_field");
        String lengthIn = req.queryParams("length_field");
        String dateIn = req.queryParams("date_field");

        try {
            int first = lengthIn.indexOf(':');
            int second = lengthIn.indexOf(':', first + 1);
            String hours = lengthIn.substring(0, first);
            String minutes = lengthIn.substring(first + 1, second);
            String seconds = lengthIn.substring(second + 1);
            System.out.println(hours + " " + minutes + " " + seconds);

            //create a new time object from our string.
        java.sql.Time timeValue = new java.sql.Time(Integer.parseInt(hours), Integer.parseInt(minutes),
                Integer.parseInt(seconds) );

        try(DbFacade db = new DbFacade()){
            boolean added;
            added = db.addMovie(titleIn, isanIn, genreIn, mpaaIn, lang, timeValue, Integer.parseInt(dateIn));

            if(added)
                return runner.renderTemplate(null, "suc-mod-addMovie.hbs");

        }catch (SQLException e){
            resp.status(500);
            System.err.println("In modAddMovieSuc: " + e.getMessage());
            return "";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("errorMsg", "Add movie failed!");
        return runner.renderTemplate(data, "mod-AddMovieForm.hbs");

        }catch (StringIndexOutOfBoundsException e){
            Map<String, Object> data = new HashMap<>();
            data.put("errorMsg", "Add movie failed! Input a proper Length");
            return runner.renderTemplate(data, "mod-AddMovieForm.hbs");
        }

    }

}