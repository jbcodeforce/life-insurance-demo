package org.acme.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;;

@QuarkusTest
public class VersionResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/api/v1/version")
          .then()
             .statusCode(200)
             .body(containsStringIgnoringCase("version"));
    }

}