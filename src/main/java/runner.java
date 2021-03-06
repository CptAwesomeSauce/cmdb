import spark.ModelAndView;
import spark.Spark;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.sql.SQLException;
import java.util.Map;

import static spark.Spark.*;

/**
 * Team Yellow Movie Review Website
 * 367 Spring Semester
 * Sam Driver, David S., David M., Steven Lopez, Jasmine Julian
 */
public class runner {
    public static void main(String[] args) throws SQLException {

        DbFacade db = new DbFacade();
        ProjectController controller = new ProjectController();

        get("/homepage", controller::displayHome);

        get("/viewReviews", controller::displayMyReviews);
        post("/edit-review", controller::editMyReview);
        post("/edit-success", controller::postEditSuccess);
        post("/viewReviews/delSuccess", controller::deleteMyReview);

        post( "/authenticate", controller::postLoginForm);
        post( "/deauthenticate", controller::releaseLogin);
        get("/movie-list", controller::getMovieList);
        get("/movie-info", controller::getMovieInfo);
        get("/user-reviews", controller::getUserReview);
        get("/user-reviews/:ISN", controller::getUserReviews);

        get("/write-review/:ISN", controller::getReviewForm);
        get("/write-review", controller::getReview);
        post("/review-success/:ISN", controller::postReviewSuccess);
        get("/movie-by-genre", controller::getMovieListGenre);

        get("/movie-info/:ISN", controller::getMovies);
        post("/newuser", controller::createNewUser);
        post("new-user-form", controller::getNewUserForm);
        get("/user/userhome", controller::getUserHome);

        get("/mod/modhome", controller::getModHome);
        get("/mod/review-check", controller::displayReviewCheck);
        post("/mod/review-check/update", controller::approveReview);
        get("/mod/freezeUser", controller::displayFreezeUserForm);
        post("/mod/freezeUser/result", controller::modFreezeUser);
        get("mod/addMovie", controller::modAddMovieForm);
        post("/mod/addMovie/success", controller::modAddMovieSuc);

        get("/admin/adminhome", controller::getAdminHome);
        get("/admin/promote-user", controller::promoteDemoteForm);
        post("/admin/success-user-change", controller::promoteDemotePost);
        get("/admin/freezeUser", controller::adminDisplayFreezeUserForm);
        post("/admin/freezeUser/result", controller::adminFreezeUser);
        //This admin feature will only DISPLAY the results of the search.
        //The idea is that admins don't want to waste their time editing or
        //deleting reviews. So they could specify which reviews to delete
        //to a moderator.
        get("/admin/searchReview", controller::adminListReviewsForm);
        post("/admin/searchReview/result", controller::adminReviewPost);

        get("/way-home", controller::goHome);


            //db.addMovie("Animal House", "0000000000001", "Comedy", "R", "English", new Time(90 * 60 * 1000), 1978);
        before("/admin/*", controller::adminBeforeFilter);
        before("/mod/*", controller::modBeforeFilter);
        before("/user/*", controller::userBeforeFilter);
            db.close();




    }

    public static Object renderTemplate(Map<String, Object> data, String path){
        return new HandlebarsTemplateEngine().render(new ModelAndView(data,path));
    }
}
