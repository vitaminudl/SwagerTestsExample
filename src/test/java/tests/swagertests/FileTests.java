package tests.swagertests;

import io.qameta.allure.Attachment;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.FileService;
import utils.CustomTpl;
import static assertions.Conditions.hasMessage;
import static assertions.Conditions.hasStatusCode;
import java.io.File;


public class FileTests {
    private static FileService fileService;

    @BeforeAll
    public static void setUp(){
        RestAssured.baseURI = "http://85.192.34.140:8080/";
           RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(),
                   CustomTpl.customLogFilter().withCustomTemplates());
           fileService = new FileService();
    }

    @Attachment(value = "downloaded", type = "image/jpg")
    private byte[] attachFile(byte[] bytes){
        return bytes;
    }

    @Test
    public void positiveDownloadTest(){
        byte[] file = fileService.downloadBaseImage()
                .asResponse().asByteArray();
        attachFile(file);
        File expectedFile = new File("src/test/resources/testPictures.jpg");

        Assertions.assertNotEquals(expectedFile.length(), file.length);
    }

    @Test
    public void positiveUploadloadTest(){
        File expectedFile = new File("src/test/resources/testPictures.jpg");
        fileService.uploadFile(expectedFile)
                .should(hasStatusCode(200))
                .should(hasMessage("file uploaded to server"));

        byte[] actualFile = fileService.downloadLastFile().asResponse().asByteArray();
        Assertions.assertTrue(actualFile.length != 0);
        Assertions.assertEquals(expectedFile.length(), actualFile.length);
    }


}
