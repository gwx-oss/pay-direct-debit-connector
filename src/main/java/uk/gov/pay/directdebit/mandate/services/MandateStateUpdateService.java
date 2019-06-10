package uk.gov.pay.directdebit.mandate.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.directdebit.mandate.dao.MandateDao;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.mandate.model.MandateState;
import uk.gov.pay.directdebit.mandate.model.MandateStatesGraph;
import uk.gov.pay.directdebit.notifications.services.UserNotificationService;
import uk.gov.pay.directdebit.payments.exception.InvalidStateTransitionException;
import uk.gov.pay.directdebit.payments.model.DirectDebitEvent;
import uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent;
import uk.gov.pay.directdebit.payments.services.DirectDebitEventService;

import javax.inject.Inject;

import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.DIRECT_DEBIT_DETAILS_CONFIRMED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_ACTIVE;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_CANCELLED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_FAILED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_PENDING;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.TOKEN_EXCHANGED;


public class MandateStateUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MandateService.class);
    private final MandateDao mandateDao;
    private final DirectDebitEventService directDebitEventService;
    private final UserNotificationService userNotificationService;

    @Inject
    public MandateStateUpdateService(
            MandateDao mandateDao,
            DirectDebitEventService directDebitEventService,
            UserNotificationService userNotificationService) {
        this.directDebitEventService = directDebitEventService;
        this.mandateDao = mandateDao;
        this.userNotificationService = userNotificationService;
    }

    Mandate confirmedOneOffDirectDebitDetailsFor(Mandate mandate) {
        return confirmedDetailsFor(mandate);
    }
    
    void mandateCreatedFor(Mandate mandate) {
        directDebitEventService.registerMandateCreatedEventFor(mandate);
    }

    Mandate confirmedOnDemandDirectDebitDetailsFor(Mandate mandate) {
        Mandate updatedMandate = confirmedDetailsFor(mandate);
        userNotificationService.sendOnDemandMandateCreatedEmailFor(updatedMandate);
        return updatedMandate;
    }

    void changePaymentMethodFor(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, SupportedEvent.PAYMENT_CANCELLED_BY_USER_NOT_ELIGIBLE);
        directDebitEventService.registerPaymentMethodChangedEventFor(newMandate);
    }

    void cancelMandateCreation(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, SupportedEvent.PAYMENT_CANCELLED_BY_USER);
        directDebitEventService.registerMandateCancelledEventFor(newMandate);
    }

    public DirectDebitEvent mandateActiveFor(Mandate mandate) {
        updateStateFor(mandate, MANDATE_ACTIVE);
        return directDebitEventService.registerMandateActiveEventFor(mandate);
    }

    public void mandateExpiredFor(Mandate mandate) {
        Mandate updatedMandate = updateStateFor(mandate, SupportedEvent.MANDATE_EXPIRED_BY_SYSTEM);
        directDebitEventService.registerMandateExpiredEventFor(updatedMandate);
    }

    public DirectDebitEvent mandateFailedFor(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, MANDATE_FAILED);
        userNotificationService.sendMandateFailedEmailFor(newMandate);
        return directDebitEventService.registerMandateFailedEventFor(newMandate);
    }

    public DirectDebitEvent mandateCancelledFor(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, MANDATE_CANCELLED);
        userNotificationService.sendMandateCancelledEmailFor(newMandate);
        return directDebitEventService.registerMandateCancelledEventFor(newMandate);
    }

    public DirectDebitEvent mandatePendingFor(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, MANDATE_PENDING);
        return directDebitEventService.registerMandatePendingEventFor(newMandate);
    }

    public Mandate receiveDirectDebitDetailsFor(Mandate mandate) {
        directDebitEventService.registerDirectDebitReceivedEventFor(mandate);
        return mandate;
    }

    public void payerCreatedFor(Mandate mandate) {
        directDebitEventService.registerPayerCreatedEventFor(mandate);
    }

    public void payerEditedFor(Mandate mandate) {
        directDebitEventService.registerPayerEditedEventFor(mandate);
    }

    public Mandate tokenExchangedFor(Mandate mandate) {
        Mandate newMandate = updateStateFor(mandate, TOKEN_EXCHANGED);
        directDebitEventService.registerTokenExchangedEventFor(newMandate);
        return newMandate;
    }

    private Mandate updateStateFor(Mandate mandate, DirectDebitEvent.SupportedEvent event) {
        MandateState newState = MandateStatesGraph
                .getStates()
                .getNextStateForEvent(mandate.getState(), event)
                .orElseThrow(() -> new InvalidStateTransitionException(event.toString(), mandate.getState().toString()));

                mandateDao.updateState(mandate.getId(), newState);
        LOGGER.info("Updating mandate {} - from {} to {}",
                mandate.getExternalId(),
                mandate.getState(),
                newState);
        mandate.setState(newState);
        return mandate;
    }

    boolean canUpdateStateFor(Mandate mandate, DirectDebitEvent.SupportedEvent event) {
        return MandateStatesGraph.getStates().getNextStateForEvent(mandate.getState(), event).isPresent();
    }

    private Mandate confirmedDetailsFor(Mandate mandate) {
        updateStateFor(mandate, DIRECT_DEBIT_DETAILS_CONFIRMED);
        mandateDao.updateReferenceAndPaymentProviderId(mandate);
        return mandate;
    }
}
