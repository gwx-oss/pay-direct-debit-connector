package uk.gov.pay.directdebit.payments.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandateId;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.mandate.model.MandateBankStatementReference;
import uk.gov.pay.directdebit.mandate.model.PaymentProviderMandateIdAndBankReference;
import uk.gov.pay.directdebit.mandate.model.SandboxMandateId;
import uk.gov.pay.directdebit.mandate.services.gocardless.GoCardlessService;
import uk.gov.pay.directdebit.payments.exception.CreateCustomerFailedException;
import uk.gov.pay.directdebit.payments.exception.CreateMandateFailedException;
import uk.gov.pay.directdebit.payments.exception.GoCardlessMandateNotConfirmed;
import uk.gov.pay.directdebit.payments.model.GoCardlessPaymentId;
import uk.gov.pay.directdebit.payments.model.PaymentProviderPaymentIdAndChargeDate;

import java.time.LocalDate;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.Month.JULY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GoCardlessServiceOnDemandTest extends GoCardlessServiceTest {

    @Before
    public void setUp() {
        service = new GoCardlessService(mockedGoCardlessClientFactory, mockedGoCardlessCustomerDao, mockedGoCardlessPaymentDao, mockedPaymentDao);
        when(mockedGoCardlessClientFactory.getClientFor(Optional.of(gatewayAccountFixture.getAccessToken()))).thenReturn(mockedGoCardlessClientFacade);
        when(mockedGoCardlessClientFacade.createCustomer(MANDATE_ID, payerFixture.toEntity())).thenReturn(goCardlessCustomer);
        when(mockedGoCardlessClientFacade.createCustomerBankAccount(MANDATE_ID, goCardlessCustomer, payerFixture.getName(), SORT_CODE, ACCOUNT_NUMBER)).thenReturn(goCardlessCustomer);
    }

    @Test
    public void confirm_shouldCreateAGoCardlessCustomerBankAccountMandateAndPaymentWhenReceivingConfirmTransaction() {
        Mandate mandate = mandateFixture.toEntity();

        when(mockedGoCardlessClientFacade.createMandate(mandate, goCardlessCustomer))
                .thenReturn(new PaymentProviderMandateIdAndBankReference(
                        SandboxMandateId.valueOf("aPaymentProviderId"),
                        MandateBankStatementReference.valueOf(BANK_ACCOUNT_ID)));

        PaymentProviderMandateIdAndBankReference paymentProviderMandateIdAndBankReference = service.confirmMandate(mandate, bankAccountDetails);
        verify(mockedGoCardlessCustomerDao).insert(goCardlessCustomer);
        InOrder orderedCalls = inOrder(mockedGoCardlessClientFacade);

        orderedCalls.verify(mockedGoCardlessClientFacade).createCustomer(MANDATE_ID, payerFixture.toEntity());
        orderedCalls.verify(mockedGoCardlessClientFacade).createCustomerBankAccount(MANDATE_ID, goCardlessCustomer, payerFixture.getName(), SORT_CODE, ACCOUNT_NUMBER);
        orderedCalls.verify(mockedGoCardlessClientFacade).createMandate(mandate, goCardlessCustomer);

        assertThat(paymentProviderMandateIdAndBankReference.getMandateBankStatementReference(), is(MandateBankStatementReference.valueOf(BANK_ACCOUNT_ID)));
    }

    @Test
    public void confirm_shouldThrow_ifFailingToCreateCustomerInGoCardless() {
        when(mockedGoCardlessClientFacade.createCustomer(MANDATE_ID, payerFixture.toEntity()))
                .thenThrow(new RuntimeException("ooops"));

        thrown.expect(CreateCustomerFailedException.class);
        thrown.expectMessage(format("Failed to create customer in gocardless, mandate id: %s, payer id: %s",
                MANDATE_ID, payerFixture.getExternalId()));
        thrown.reportMissingExceptionWithMessage("CreateCustomerFailedException expected");

        service.confirmMandate(mandateFixture.toEntity(), bankAccountDetails);
    }

    @Test
    public void confirm_shouldThrow_ifFailingToCreateCustomerBankAccountInGoCardless() {
        verifyCreateCustomerBankAccountFailedException();

        service.confirmMandate(mandateFixture.toEntity(), bankAccountDetails);
    }

    @Test
    public void confirm_shouldThrow_ifFailingToCreateMandateInGoCardless() {
        when(mockedGoCardlessClientFacade.createMandate(mandateFixture.toEntity(), goCardlessCustomer))
                .thenThrow(new RuntimeException("gocardless said no"));

        thrown.expect(CreateMandateFailedException.class);
        thrown.expectMessage(format("Failed to create mandate in gocardless, mandate id: %s", MANDATE_ID));
        thrown.reportMissingExceptionWithMessage("CreateMandateFailedException expected");

        service.confirmMandate(mandateFixture.toEntity(), bankAccountDetails);
    }

    @Test
    public void collect_shouldCreateAndStoreAPaymentForAValidGoCardlessMandate() {
        GoCardlessPaymentId goCardlessPaymentId = GoCardlessPaymentId.valueOf("expectedGoCardlessPaymentId");
        GoCardlessMandateId goCardlessMandateId = GoCardlessMandateId.valueOf("aGoCardlessMandateId");
        LocalDate chargeDateFromGoCardless = LocalDate.of(1969, JULY, 16);
        
        Mandate mandate = mandateFixture
                .withPaymentProviderId(goCardlessMandateId)
                .toEntity();

        when(mockedGoCardlessClientFacade.createPayment(payment, goCardlessMandateId))
                .thenReturn(new PaymentProviderPaymentIdAndChargeDate(goCardlessPaymentId, chargeDateFromGoCardless));

        LocalDate actualChargeDate = service.collect(mandate, payment);
        verify(mockedPaymentDao).updateProviderIdAndChargeDate(payment);

        assertThat(actualChargeDate, is(chargeDateFromGoCardless));
    }
   
    @Test
    public void collect_shouldThrowIfTryingToCollectFromAnUnconfirmedMandate() {
        Mandate mandate = mandateFixture
                .withPaymentProviderId(null)
                .toEntity();

        thrown.expect(GoCardlessMandateNotConfirmed.class);
        thrown.expectMessage(format("Mandate with mandate id: %s has not been confirmed with GoCardless", MANDATE_ID));
        thrown.reportMissingExceptionWithMessage("GoCardlessMandateNotConfirmed expected");

        service.collect(mandate, payment);
    }
    
}
