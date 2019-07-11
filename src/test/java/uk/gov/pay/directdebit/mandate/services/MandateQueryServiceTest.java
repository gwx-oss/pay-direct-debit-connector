package uk.gov.pay.directdebit.mandate.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.directdebit.gatewayaccounts.model.GoCardlessOrganisationId;
import uk.gov.pay.directdebit.mandate.dao.MandateDao;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandateId;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.mandate.model.MandateLookupKey;
import uk.gov.pay.directdebit.mandate.model.SandboxMandateId;
import uk.gov.pay.directdebit.payments.model.GoCardlessMandateIdAndOrganisationId;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.pay.directdebit.gatewayaccounts.model.PaymentProvider.GOCARDLESS;
import static uk.gov.pay.directdebit.gatewayaccounts.model.PaymentProvider.SANDBOX;

@RunWith(MockitoJUnitRunner.class)
public class MandateQueryServiceTest {

    private static final SandboxMandateId SANDBOX_MANDATE_ID = SandboxMandateId.valueOf("Sandy");
    private static final GoCardlessMandateId GOCARDLESS_MANDATE_ID = GoCardlessMandateId.valueOf("MD123");
    private static final GoCardlessOrganisationId GOCARDLESS_ORGANISATION_ID = GoCardlessOrganisationId.valueOf("OR123");
    private static final GoCardlessMandateIdAndOrganisationId GOCARDLESS_MANDATE_ID_AND_ORGANISATION_ID =
            new GoCardlessMandateIdAndOrganisationId(GOCARDLESS_MANDATE_ID, GOCARDLESS_ORGANISATION_ID);

    @Mock
    private MandateDao mockMandateDao;
    
    @Mock
    private Mandate mockMandate;

    private MandateQueryService mandateQueryService;

    @Before
    public void setUp() {
        mandateQueryService = new MandateQueryService(mockMandateDao);
    }

    @Test
    public void findByPaymentProviderMandateIdWithSandboxMandateIdReturnsUpdateCount() {
        given(mockMandateDao.findByPaymentProviderMandateId(SANDBOX, SANDBOX_MANDATE_ID)).willReturn(Optional.of(mockMandate));

        Mandate result = mandateQueryService.findByPaymentProviderMandateId(SANDBOX, SANDBOX_MANDATE_ID);

        assertThat(result, is(mockMandate));

        verifyZeroInteractions(ignoreStubs(mockMandateDao));
    }

    @Test
    public void findByPaymentProviderMandateIdWithGoCardlessMandateIdAndOrganisationIdReturnsUpdateCount() {
        given(mockMandateDao.findByPaymentProviderMandateIdAndOrganisation(GOCARDLESS, GOCARDLESS_MANDATE_ID, GOCARDLESS_ORGANISATION_ID))
                .willReturn(Optional.of(mockMandate));

        Mandate result = mandateQueryService.findByPaymentProviderMandateId(GOCARDLESS, GOCARDLESS_MANDATE_ID_AND_ORGANISATION_ID);
        
        assertThat(result, is(mockMandate));
        
        verifyZeroInteractions(ignoreStubs(mockMandateDao));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByPaymentProviderMandateIdWithUnrecognisedTypeThrowsException() {
        mandateQueryService.findByPaymentProviderMandateId(GOCARDLESS, new UnrecognisedMandateLookupKeyImplementation());
    }

    private static class UnrecognisedMandateLookupKeyImplementation implements MandateLookupKey {

    }

}