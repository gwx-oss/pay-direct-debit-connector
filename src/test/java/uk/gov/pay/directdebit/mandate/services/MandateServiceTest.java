package uk.gov.pay.directdebit.mandate.services;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.directdebit.app.config.DirectDebitConfig;
import uk.gov.pay.directdebit.gatewayaccounts.dao.GatewayAccountDao;
import uk.gov.pay.directdebit.mandate.dao.MandateDao;
import uk.gov.pay.directdebit.mandate.fixtures.MandateFixture;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.notifications.services.UserNotificationService;
import uk.gov.pay.directdebit.payments.exception.InvalidStateTransitionException;
import uk.gov.pay.directdebit.payments.fixtures.EventFixture;
import uk.gov.pay.directdebit.payments.model.Event;
import uk.gov.pay.directdebit.payments.services.PaymentRequestEventService;
import uk.gov.pay.directdebit.payments.services.TransactionService;
import uk.gov.pay.directdebit.tokens.services.TokenService;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.directdebit.mandate.model.MandateState.ACTIVE;
import static uk.gov.pay.directdebit.mandate.model.MandateState.AWAITING_DIRECT_DEBIT_DETAILS;
import static uk.gov.pay.directdebit.mandate.model.MandateState.CREATED;
import static uk.gov.pay.directdebit.mandate.model.MandateState.PENDING;
import static uk.gov.pay.directdebit.mandate.model.MandateState.SUBMITTED;
import static uk.gov.pay.directdebit.payments.model.Event.SupportedEvent.MANDATE_PENDING;

@RunWith(MockitoJUnitRunner.class)
public class MandateServiceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private PaymentRequestEventService mockedPaymentRequestEventService;
    @Mock
    private MandateDao mockedMandateDao;
    @Mock
    private GatewayAccountDao mockedGatewayAccountDao;
    @Mock
    private TokenService mockedTokenService;
    @Mock
    private TransactionService mockedTransactionService;
    @Mock
    private DirectDebitConfig mockedDirectDebitConfig;
    @Mock
    private UserNotificationService mockedUserNotificationService;

    private MandateService service;

    @Before
    public void setUp() {
        service = new MandateService(mockedDirectDebitConfig, mockedMandateDao, mockedGatewayAccountDao, mockedTokenService,
                mockedTransactionService, mockedPaymentRequestEventService, mockedUserNotificationService);
    }


    @Test
    public void shouldUpdateMandateStateRegisterEventAndSendEmail_whenMandateFails() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(PENDING)
                .toEntity();
        service.mandateFailedFor(mandate);
        verify(mockedPaymentRequestEventService).registerMandateFailedEventFor(mandate);
        verify(mockedUserNotificationService).sendMandateFailedEmailFor(mandate);
    }

    @Test
    public void shouldUpdateMandateStateRegisterEventAndSendEmail_whenMandateIsCancelled() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(PENDING)
                .toEntity();
        service.mandateCancelledFor(mandate);
        verify(mockedPaymentRequestEventService).registerMandateCancelledEventFor(mandate);
        verify(mockedUserNotificationService).sendMandateCancelledEmailFor(mandate);
    }

    @Test
    public void mandateActiveFor_shouldRegisterAMandateActiveEvent() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(PENDING)
                .toEntity();
        service.mandateActiveFor(mandate);

        verify(mockedPaymentRequestEventService).registerMandateActiveEventFor(mandate);
        assertThat(mandate.getState(), is(ACTIVE));
    }


    @Test
    public void findMandatePendingEventFor_shouldFindEvent() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(PENDING)
                .toEntity();
        Event event = EventFixture.aPaymentRequestEventFixture().toEntity();

        when(mockedPaymentRequestEventService.findBy(mandate.getId(), Event.Type.MANDATE, MANDATE_PENDING))
                .thenReturn(Optional.of(event));

        Event foundEvent = service.findMandatePendingEventFor(mandate).get();

        assertThat(foundEvent, is(event));
    }

    @Test
    public void payerCreatedFor_shouldRegisterAPayerCreatedEvent() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(AWAITING_DIRECT_DEBIT_DETAILS)
                .toEntity();

        service.payerCreatedFor(mandate);

        verify(mockedPaymentRequestEventService).registerPayerCreatedEventFor(mandate);
        verifyZeroInteractions(mockedMandateDao);
        assertThat(mandate.getState(), is(AWAITING_DIRECT_DEBIT_DETAILS));
    }

    @Test
    public void payerEditedFor_shouldRegisterAPayerEditedEvent() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(AWAITING_DIRECT_DEBIT_DETAILS)
                .toEntity();
        service.payerEditedFor(mandate);

        verify(mockedPaymentRequestEventService).registerPayerEditedEventFor(mandate);
        verifyZeroInteractions(mockedMandateDao);
        assertThat(mandate.getState(), Matchers.is(AWAITING_DIRECT_DEBIT_DETAILS));
    }

    @Test
    public void shouldRegisterEventWhenReceivingDirectDebitDetails() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(AWAITING_DIRECT_DEBIT_DETAILS)
                .toEntity();
        when(mockedMandateDao.findByExternalId(mandate.getExternalId()))
                .thenReturn(Optional.of(mandate));
        service.receiveDirectDebitDetailsFor(mandate.getExternalId());
        verify(mockedPaymentRequestEventService).registerDirectDebitReceivedEventFor(mandate);
        assertThat(mandate.getState(), Matchers.is(AWAITING_DIRECT_DEBIT_DETAILS));
    }

    @Test
    public void findMandateForToken_shouldUpdateTransactionStateAndRegisterEventWhenExchangingTokens() {
        String token = "token";
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(CREATED)
                .toEntity();
        when(mockedMandateDao.findByTokenId(token))
                .thenReturn(Optional.of(mandate));
        Mandate newMandate = service.findMandateForToken(token).get();
        assertThat(newMandate.getId(), is(notNullValue()));
        assertThat(newMandate.getExternalId(), is(mandate.getExternalId()));
        assertThat(newMandate.getReturnUrl(), is(mandate.getReturnUrl()));
        assertThat(newMandate.getGatewayAccount(), is(mandate.getGatewayAccount()));
        assertThat(newMandate.getReference(), is(mandate.getReference()));
        assertThat(newMandate.getPayer(), is(mandate.getPayer()));
        assertThat(newMandate.getType(), is(mandate.getType()));
        assertThat(newMandate.getState(), is(AWAITING_DIRECT_DEBIT_DETAILS));
        assertThat(newMandate.getCreatedDate(), is(mandate.getCreatedDate()));
        verify(mockedPaymentRequestEventService).registerTokenExchangedEventFor(newMandate);
    }

    @Test
    public void shouldUpdateMandateStateAndRegisterEventWhenConfirmingDirectDebitDetails() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(AWAITING_DIRECT_DEBIT_DETAILS)
                .toEntity();
        when(mockedMandateDao.findByExternalId(mandate.getExternalId())).thenReturn(Optional.of(mandate));
        Mandate newMandate = service.confirmedDirectDebitDetailsFor(mandate.getExternalId());
        assertThat(newMandate.getId(), is(notNullValue()));
        assertThat(newMandate.getExternalId(), is(mandate.getExternalId()));
        assertThat(newMandate.getReturnUrl(), is(mandate.getReturnUrl()));
        assertThat(newMandate.getGatewayAccount(), is(mandate.getGatewayAccount()));
        assertThat(newMandate.getReference(), is(mandate.getReference()));
        assertThat(newMandate.getPayer(), is(mandate.getPayer()));
        assertThat(newMandate.getType(), is(mandate.getType()));
        assertThat(newMandate.getState(), is(SUBMITTED));
        assertThat(newMandate.getCreatedDate(), is(mandate.getCreatedDate()));
    }

    @Test
    public void mandatePendingFor_shouldRegisterAMandatePendingEvent() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(SUBMITTED)
                .toEntity();

        service.mandatePendingFor(mandate);

        verify(mockedPaymentRequestEventService).registerMandatePendingEventFor(mandate);
        assertThat(mandate.getState(), is(PENDING));
    }

    @Test
    public void shouldThrowWhenSubmittingTheSameDetailsSeveralTimes_andCreateOnlyOneEvent_whenConfirmingDirectDebitDetails() {
        Mandate mandate = MandateFixture
                .aMandateFixture()
                .withState(AWAITING_DIRECT_DEBIT_DETAILS)
                .toEntity();

        when(mockedMandateDao.findByExternalId(mandate.getExternalId())).thenReturn(Optional.of(mandate));

        // first call with confirmation details
        service.confirmedDirectDebitDetailsFor(mandate.getExternalId());
        thrown.expect(InvalidStateTransitionException.class);
        thrown.expectMessage("Transition DIRECT_DEBIT_DETAILS_CONFIRMED from state SUBMITTED is not valid");

        // second call with same details that should throw and prevent adding a new event
        Mandate newMandate = service.confirmedDirectDebitDetailsFor(mandate.getExternalId());
        assertThat(newMandate.getState(), is(SUBMITTED));
        verify(mockedPaymentRequestEventService, times(1)).registerDirectDebitConfirmedEventFor(newMandate);
    }
}
