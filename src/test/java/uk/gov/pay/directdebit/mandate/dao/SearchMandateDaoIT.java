package uk.gov.pay.directdebit.mandate.dao;

import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.directdebit.DirectDebitConnectorApp;
import uk.gov.pay.directdebit.junit.DropwizardConfig;
import uk.gov.pay.directdebit.junit.DropwizardJUnitRunner;
import uk.gov.pay.directdebit.junit.DropwizardTestContext;
import uk.gov.pay.directdebit.junit.TestContext;
import uk.gov.pay.directdebit.mandate.fixtures.MandateFixture;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.mandate.model.MandateBankStatementReference;
import uk.gov.pay.directdebit.payers.fixtures.PayerFixture;
import uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture;

import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static uk.gov.pay.directdebit.mandate.fixtures.MandateFixture.aMandateFixture;
import static uk.gov.pay.directdebit.mandate.model.MandateState.FAILED;
import static uk.gov.pay.directdebit.mandate.params.MandateSearchParams.aMandateSearchParams;
import static uk.gov.pay.directdebit.payers.fixtures.PayerFixture.aPayerFixture;
import static uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture.aGatewayAccountFixture;

@RunWith(DropwizardJUnitRunner.class)
@DropwizardConfig(app = DirectDebitConnectorApp.class, config = "config/test-it-config.yaml")
public class SearchMandateDaoIT {

    @DropwizardTestContext
    private TestContext testContext;

    private MandateSearchDao mandateSearchDao;
    private GatewayAccountFixture gatewayAccountFixture = aGatewayAccountFixture().withExternalId("gateway-account-id");
    private PayerFixture joeBloggs = aPayerFixture().withEmail("joe.bloggs@example.com").withName("Joe Bloggs");
    private MandateFixture mandate1 = aMandateFixture()
            .withGatewayAccountFixture(gatewayAccountFixture)
            .withServiceReference("REF1234")
            .withPayerFixture(joeBloggs)
            .withCreatedDate(now());
    private MandateFixture mandate2 = aMandateFixture()
            .withGatewayAccountFixture(gatewayAccountFixture)
            .withMandateBankStatementReference(MandateBankStatementReference.valueOf("STATEMENT123"))
            .withState(FAILED)
            .withCreatedDate(now().minusHours(6));
    
    @Before
    public void setup() {
        mandateSearchDao = new MandateSearchDao(testContext.getJdbi());

        gatewayAccountFixture.insert(testContext.getJdbi());
        mandate1.insert(testContext.getJdbi());
        mandate2.insert(testContext.getJdbi());
    }
    
    @Test
    @Parameters({"REF1234", "ref1234", "f12"})
    public void searchByReference(String searchString) {
        var mandateSearchParams = aMandateSearchParams().withReference(searchString).withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(mandateSearchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate1.getId());
    }
    
    @Test
    public void searchByState() {
        var mandateSearchParams = aMandateSearchParams().withMandateState(FAILED).withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(mandateSearchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate2.getId());
    }
    
    @Test
    @Parameters({"STATEMENT123", "statement123", "ment"})
    public void searchByBankStatementReference(String searchString) {
        var mandateSearchParams = aMandateSearchParams()
                .withMandateBankStatementReference(MandateBankStatementReference.valueOf(searchString))
                .withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(mandateSearchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate2.getId());
    }
    
    @Test
    @Parameters({"joe.bloggs@example.com", "joe.bloggs@EXAMPLE.com", "joe.bloggs"})
    public void searchByEmail(String searchString) {
        var mandateSearchParams = aMandateSearchParams().withEmail(searchString).withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(mandateSearchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate1.getId());
    }

    @Test
    @Parameters({"JOe Bloggs", "joe bloggs", "bloggs"})
    public void searchByName(String searchString) {
        var mandateSearchParams = aMandateSearchParams().withName(searchString).withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(mandateSearchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate1.getId());
    }
    
    @Test
    public void searchByFromDate() {
        var searchParams = aMandateSearchParams().withFromDate(now().minusHours(1)).withGatewayAccountId("gateway-account-id");
        List<Mandate> results = mandateSearchDao.search(searchParams);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(mandate1.getId());

        searchParams = aMandateSearchParams().withFromDate(now().minusHours(7)).withGatewayAccountId("gateway-account-id");
        results = mandateSearchDao.search(searchParams);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.stream().map(m -> m.getId()).collect(Collectors.toList())).contains(mandate1.getId(), mandate2.getId());
    }
    
    @Test
    public void searchByToDate() {
        
    }
    
    @Test
    public void searchByDateRange() {
        
    }
    
    @Test
    public void searchByPage() {
        
    }
    
    @Test
    public void searchByDisplaySize() {
        
    }
    
    @Test
    public void searchByInvalidDisplaySize() {
        
    }
    
    @Test
    public void searchByMultipleParams() {
        
    }
}
