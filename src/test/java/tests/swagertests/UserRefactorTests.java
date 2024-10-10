package tests.swagertests;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import models.swager.FullUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.UserService;
import utils.CustomTpl;
import java.util.List;
import java.util.Random;
import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;


public class UserRefactorTests {
    private static Random random;
    private static UserService userService;

    @BeforeAll
    public static void setUp(){
        RestAssured.baseURI = "http://85.192.34.140:8080/";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates());
        random = new Random();
        userService = new UserService();
    }

    private FullUser getRandomUser(){
        int randomNumber = Math.abs(random.nextInt());
        return FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();
    }

    private FullUser getAdminUser(){
        return FullUser.builder()
                .login("admin")
                .pass("admin")
                .build();
    }

    @Test
    public void positiveRegisterTest(){
        FullUser user = getRandomUser();
        userService.register(user)
                .should(hasStatusCode(201))
                .should(hasMessage("User created"));
    }

    @Test
    public void negativRegisterLoginExistsTest(){
        FullUser user = getRandomUser();
        userService.register(user);
        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Login already exist"));
    }

    @Test
    public void negativRegisterNoPasswordTest(){
        FullUser user = getRandomUser();
        user.setPass(null);

        userService.register(user)
                .should(hasStatusCode(400))
                .should(hasMessage("Missing login or password"));
    }

    @Test
    public void positiveAuthTest(){
        FullUser user = getAdminUser();

        String token = userService.auth(user)
                .should(hasStatusCode(200))
                .asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    public void positiveNewUserAuthTest(){
        FullUser user = getRandomUser();
        userService.register(user);
        String token = userService.auth(user)
                .should(hasStatusCode(200)).asJwt();

        Assertions.assertNotNull(token);
    }

    @Test
    public void negativeAuthTest(){
        FullUser user = getRandomUser();
        userService.auth(user).should(hasStatusCode(401));
    }

    @Test
    public void positiveGetUserInfoTest(){
        FullUser user = getAdminUser();
        String token = userService.auth(user).asJwt();
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
        FullUser user = getRandomUser();
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
    public void negativeChangeAdminPasswordTest(){
        FullUser user = getAdminUser();

        String token = userService.auth(user).asJwt();

        String updatedPassValue = "newpassUpdated";
        userService.updatePass(updatedPassValue, token)
                .should(hasStatusCode(400))
                .should(hasMessage("Cant update base users"));
    }

    @Test
    public void negativeDeletedAdminTest(){
        FullUser user = getAdminUser();

        String token = userService.auth(user).asJwt();

        userService.deleteUser(token).should(hasStatusCode(400))
                .should(hasMessage("Cant delete base users"));
    }

    @Test
    public void positiveDeletedUserTest(){
        FullUser user = getRandomUser();
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
