package tests.swagertests;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import listener.AdminUser;
import listener.AdminUserResolver;
import models.swager.FullUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import services.UserService;
import utils.CustomTpl;
import java.util.List;
import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;
import static utils.RandomTestData.*;

@ExtendWith({AdminUserResolver.class})
public class UserRefactorTests {
    private static UserService userService;
    private FullUser user;

    @BeforeEach
    public void initTestUser(){
        user = getRandomUser();
    }

    @BeforeAll
    public static void setUp(){
        RestAssured.baseURI = "http://85.192.34.140:8080/";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates());
        userService = new UserService();
    }

    @Test
    public void positiveRegisterTest(){
        userService.register(user)
                .should(hasStatusCode(201))
                .should(hasMessage("User created"));
    }

    @Test
    public void positiveRegisterWithGamesTest(){
        FullUser user = getRandomUserWithGames();
        userService.register(user)
                .should(hasStatusCode(201))
                .should(hasMessage("User created"));
    }


    @Test
    public void negativRegisterLoginExistsTest(){
        userService.register(user);
        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Login already exist"));
    }

    @Test
    public void negativRegisterNoPasswordTest(){
        user.setPass(null);

        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Missing login or password"));
    }

    @Test
    public void positiveAdminAuthTest(@AdminUser FullUser admin){
        String token = userService.auth(admin)
                .should(hasStatusCode(200))
                .asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    public void positiveNewUserAuthTest(){
        userService.register(user);
        String token = userService.auth(user)
                .should(hasStatusCode(200)).asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    public void negativeAuthTest(){
        userService.auth(user).should(hasStatusCode(401));
    }

    @Test
    public void positiveGetUserInfoTest(@AdminUser FullUser admin){
        String token = userService.auth(admin).asJwt();
        userService.getUserInfo(token)
                .should(hasStatusCode(200));
    }

    @Test
    public void negativeGetUserInfoInvalidJWTTest(){
        userService.getUserInfo("fake jwt").should(hasStatusCode(401));
    }

    @Test
    public void negativeGetUserInfoWithoutJWTTest(){
        userService.getUserInfo().should(hasStatusCode(401));
    }

    @Test
    public void positiveChangePassTest(){
        String oldPassword = user.getPass();
        userService.register(user);

        String token = userService.auth(user).asJwt();

        String updatedPassValue = "newpassUpdated";

        userService.updatePass(updatedPassValue, token)
                .should(hasStatusCode(200))
                .should(hasMessage("User password successfully changed"));

        user.setPass(updatedPassValue);

        token = userService.auth(user).should(hasStatusCode(200)).asJwt();

        FullUser updatedUser = userService.getUserInfo(token).as(FullUser.class);

        Assertions.assertNotEquals(oldPassword,updatedUser.getPass());
    }

    @Test
    public void negativeChangeAdminPasswordTest(@AdminUser FullUser admin){
        String token = userService.auth(admin).asJwt();

        String updatedPassValue = "newpassUpdated";
        userService.updatePass(updatedPassValue, token)
                .should(hasStatusCode(400))
                .should(hasMessage("Cant update base users"));
    }

    @Test
    public void negativeDeletedAdminTest(@AdminUser FullUser admin){
        String token = userService.auth(admin).asJwt();

        userService.deleteUser(token).should(hasStatusCode(400))
                .should(hasMessage("Cant delete base users"));
    }

    @Test
    public void positiveDeletedUserTest(){
        userService.register(user);
        String token = userService.auth(user).asJwt();

        userService.deleteUser(token).should(hasStatusCode(200))
                .should(hasMessage("User successfully deleted"));
    }

    @Test
    public void positiveGetAllUsersTest(){
        List<String> users = userService.getAllUsers().asList(String.class);
        Assertions.assertTrue(users.size()>=3);
    }

}
