package cloudgene.mapred.api.v2;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.IsEqual.equalTo;

@MicronautTest
public class HealthControllerTest {
	@Test
	public void testHealthController() {
		RestAssured.when().get("/health").then()
				.statusCode(200)
				.body(equalTo("OK"));
	}
}
