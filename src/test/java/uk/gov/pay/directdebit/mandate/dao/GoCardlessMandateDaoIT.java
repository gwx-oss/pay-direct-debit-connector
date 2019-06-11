package uk.gov.pay.directdebit.mandate.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.directdebit.DirectDebitConnectorApp;
import uk.gov.pay.directdebit.junit.DropwizardConfig;
import uk.gov.pay.directdebit.junit.DropwizardJUnitRunner;
import uk.gov.pay.directdebit.junit.DropwizardTestContext;
import uk.gov.pay.directdebit.junit.TestContext;
import uk.gov.pay.directdebit.mandate.fixtures.GoCardlessMandateFixture;
import uk.gov.pay.directdebit.mandate.fixtures.MandateFixture;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandate;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandateId;
import uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture;
import uk.gov.pay.directdebit.payments.fixtures.TransactionFixture;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.directdebit.mandate.fixtures.GoCardlessMandateFixture.aGoCardlessMandateFixture;
import static uk.gov.pay.directdebit.mandate.fixtures.MandateFixture.aMandateFixture;
import static uk.gov.pay.directdebit.payments.fixtures.GatewayAccountFixture.aGatewayAccountFixture;

@RunWith(DropwizardJUnitRunner.class)
@DropwizardConfig(app = DirectDebitConnectorApp.class, config = "config/test-it-config.yaml")
public class GoCardlessMandateDaoIT {

    @DropwizardTestContext
    private TestContext testContext;

    private GoCardlessMandateDao mandateDao;
    private MandateFixture mandateFixture;

    private final static GoCardlessMandateId GOCARDLESS_MANDATE_ID = GoCardlessMandateId.valueOf("NA23434");

    private GoCardlessMandateFixture testGoCardlessMandate;

    @Before
    public void setup() {
        mandateDao = testContext.getJdbi().onDemand(GoCardlessMandateDao.class);
        GatewayAccountFixture gatewayAccountFixture = aGatewayAccountFixture().insert(testContext.getJdbi());
        mandateFixture = aMandateFixture().withGatewayAccountFixture(gatewayAccountFixture).insert(testContext.getJdbi());
        TransactionFixture.aTransactionFixture().withMandateFixture(mandateFixture).insert(testContext.getJdbi());
        testGoCardlessMandate = aGoCardlessMandateFixture().withMandateId(mandateFixture.getId())
                .withGoCardlessMandateId(GOCARDLESS_MANDATE_ID);
    }

    @Test
    public void shouldFindAGoCardlessMandateByMandateId() {
        testGoCardlessMandate.insert(testContext.getJdbi());
        GoCardlessMandate goCardlessMandate = mandateDao.findByMandateId(mandateFixture.getId()).get();
        assertThat(goCardlessMandate.getId(), is(testGoCardlessMandate.getId()));
        assertThat(goCardlessMandate.getMandateId(), is(mandateFixture.getId()));
        assertThat(goCardlessMandate.getGoCardlessMandateId(), is(GOCARDLESS_MANDATE_ID));
    }

    @Test
    public void shouldFindAGoCardlessMandateByEventResourceId() {
        testGoCardlessMandate.insert(testContext.getJdbi());
        GoCardlessMandate goCardlessMandate = mandateDao.findByEventResourceId(GOCARDLESS_MANDATE_ID).get();
        assertThat(goCardlessMandate.getId(), is(testGoCardlessMandate.getId()));
        assertThat(goCardlessMandate.getMandateId(), is(mandateFixture.getId()));
        assertThat(goCardlessMandate.getGoCardlessMandateId(), is(GOCARDLESS_MANDATE_ID));
    }

    @Test
    public void shouldNotFindAGoCardlessMandateByEventResourceId_ifResourceIdIsInvalid() {
        GoCardlessMandateId resourceId = GoCardlessMandateId.valueOf("non_existing_resourceId");
        assertThat(mandateDao.findByEventResourceId(resourceId), is(Optional.empty()));
    }

    @Test
    public void shouldNotFindAGoCardlessMandateByMandateId_ifMandateIdIsInvalid() {
        Long mandateId = 102L;
        assertThat(mandateDao.findByMandateId(mandateId), is(Optional.empty()));
    }
}
