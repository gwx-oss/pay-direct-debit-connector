package uk.gov.pay.directdebit.tokens.resources;

import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import uk.gov.pay.directdebit.junit.TestContext;
import uk.gov.pay.directdebit.junit.DropwizardAppWithPostgresRule;
import uk.gov.pay.directdebit.mandate.fixtures.MandateFixture;
import uk.gov.pay.directdebit.mandate.model.MandateState;
import uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture;
import uk.gov.pay.directdebit.tokens.fixtures.TokenFixture;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.pay.directdebit.events.model.GovUkPayEventType.MANDATE_CREATED;
import static uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture.aGatewayAccountFixture;
import static uk.gov.pay.directdebit.payments.fixtures.GovUkPayEventFixture.aGovUkPayEventFixture;
import static uk.gov.pay.directdebit.tokens.fixtures.TokenFixture.aTokenFixture;
import static uk.gov.pay.directdebit.util.NumberMatcher.isNumber;

public class SecurityTokensResourceIT {

    private GatewayAccountFixture testGatewayAccount;
    private TestContext testContext;

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Before
    public void setUp() {
        testContext = app.getTestContext();
        testGatewayAccount = aGatewayAccountFixture().insert(testContext.getJdbi());
    }

    @After
    public void tearDown() {
        app.getDatabaseTestHelper().truncateAllData();
    }

    @Test
    public void shouldReturn200WithMandateForValidToken() {
        MandateFixture testMandate = MandateFixture.aMandateFixture().withGatewayAccountFixture(testGatewayAccount)
                .insert(testContext.getJdbi());

        aGovUkPayEventFixture()
                .withMandateId(testMandate.getId())
                .withEventType(MANDATE_CREATED)
                .insert(testContext.getJdbi());
        
        TokenFixture testToken = aTokenFixture().withMandateId(testMandate.getId()).insert(testContext.getJdbi());
        String requestPath = "/v1/tokens/{token}/mandate".replace("{token}", testToken.getToken());
        givenSetup()
                .get(requestPath)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("external_id", is(testMandate.getExternalId().toString()))
                .body("mandate_reference", is(testMandate.getMandateReference().toString()))
                .body("return_url", is(testMandate.getReturnUrl()))
                .body("gateway_account_id", isNumber(testGatewayAccount.getId()))
                .body("gateway_account_external_id", is(testGatewayAccount.getExternalId()))
                .body("state", is(MandateState.AWAITING_DIRECT_DEBIT_DETAILS.toString()))
                .body("$", not(hasKey("transaction_external_id")));
    }
    
    @Test
    public void shouldReturnNoContentWhenDeletingToken() {
        MandateFixture testMandate = MandateFixture.aMandateFixture()
                .withGatewayAccountFixture(testGatewayAccount)
                .insert(testContext.getJdbi());
        TokenFixture testToken = aTokenFixture().withMandateId(testMandate.getId()).insert(testContext.getJdbi());
        String requestPath = "/v1/tokens/{token}".replace("{token}", testToken.getToken());
        givenSetup()
                .delete(requestPath)
                .then()
                .statusCode(204);
    }

    private RequestSpecification givenSetup() {
        return given().port(testContext.getPort())
                .contentType(JSON);
    }
}
