package tests.swagertests;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import models.swager.FullUser;
import models.swager.Info;
import models.swager.JwtAuthData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.CustomTpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;


public class UserTests {

    private static Random random;
    @BeforeAll
    public static void setUp(){
        RestAssured.baseURI = "http://85.192.34.140:8080/";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                CustomTpl.customLogFilter().withCustomTemplates());
        random = new Random();
    }

    @Test
    public void positiveRegisterTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());
    }

    @Test
    public void negativRegisterLoginExistsTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        Info erroeInfo = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Login already exist", erroeInfo.getMessage());
    }

    @Test
    public void negativRegisterNoPasswordTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Missing login or password", info.getMessage());
    }

    @Test
    public void positiveAuthTest(){
        JwtAuthData authData = new JwtAuthData("admin","admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);
    }

    @Test
    public void positiveNewUserAuthTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(),user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);
    }

    @Test
    public void negativeAuthTest(){
        JwtAuthData authData = new JwtAuthData("asdad","asdasd");

        given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(401);
    }

    @Test
    public void positiveGetUserInfoTest(){
        JwtAuthData authData = new JwtAuthData("admin","admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Assertions.assertNotNull(token);

        given().auth().oauth2(token).get("/api/user")
                .then().statusCode(200);
    }

    @Test
    public void positiveGetUserInfoInvalidJWTTest(){
        given().auth().oauth2("some values")
                .get("/api/user")
                .then().statusCode(401);
    }

    @Test
    public void negativeGetUserInfoWithoutJWTTest(){
        given()
                .get("/api/user")
                .then().statusCode(401);
    }

    @Test
    public void positiveChangePass(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(),user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Map<String,String> newPassword = new HashMap<>();
        String updatedPassValue = "newpassUpdated";
        newPassword.put("password", updatedPassValue);


        Info updatePassInfo = given().contentType(ContentType.JSON)
                .auth().oauth2(token)
                .body(newPassword)
                .put("/api/user")
                .then().extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User password successfully changed", updatePassInfo.getMessage());

        authData.setPassword(updatedPassValue);
        token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        FullUser updatedUser = given().auth().oauth2(token).get("/api/user")
                .then().statusCode(200)
                .extract().as(FullUser.class);
        Assertions.assertNotEquals(user.getPass(),updatedUser.getPass());
    }

    @Test
    public void negativeChangeAdminPasswordTest(){
        JwtAuthData authData = new JwtAuthData("admin","admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Map<String,String> newPassword = new HashMap<>();
        String updatedPassValue = "newpassUpdated";
        newPassword.put("password", updatedPassValue);


        Info updatePassInfo = given().contentType(ContentType.JSON)
                .auth().oauth2(token)
                .body(newPassword)
                .put("/api/user")
                .then().statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Cant update base users", updatePassInfo.getMessage());
    }

    @Test
    public void negativeDeletedAdminTest(){
        JwtAuthData authData = new JwtAuthData("admin","admin");

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Info info = given().auth().oauth2(token)
                .delete("/api/user")
                .then().statusCode(400)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("Cant delete base users", info.getMessage());;
    }

    @Test
    public void deletedUserTest(){
        int randomNumber = Math.abs(random.nextInt());
        FullUser user = FullUser.builder()
                .login("testUserLogin" + randomNumber)
                .pass("testpass")
                .build();

        Info info = given().contentType(ContentType.JSON)
                .body(user)
                .post("/api/signup")
                .then()
                .statusCode(201)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User created", info.getMessage());

        JwtAuthData authData = new JwtAuthData(user.getLogin(),user.getPass());

        String token = given().contentType(ContentType.JSON)
                .body(authData)
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("token");

        Info infoDeleted = given().auth().oauth2(token)
                .delete("/api/user")
                .then().statusCode(200)
                .extract().jsonPath().getObject("info", Info.class);
        Assertions.assertEquals("User successfully deleted", infoDeleted.getMessage());
    }

    @Test
    public void positiveGetAllUsersTest(){
        List<String> users = given().get("/api/users")
                .then().statusCode(200)
                .extract().as(new TypeRef<List<String>>() {});
        Assertions.assertTrue(users.size()>=3);
    }

}
