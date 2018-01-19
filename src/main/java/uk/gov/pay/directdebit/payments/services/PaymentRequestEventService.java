package uk.gov.pay.directdebit.payments.services;

import org.slf4j.Logger;
import uk.gov.pay.directdebit.app.logger.PayLoggerFactory;
import uk.gov.pay.directdebit.payments.dao.PaymentRequestEventDao;
import uk.gov.pay.directdebit.payments.model.PaymentRequest;
import uk.gov.pay.directdebit.payments.model.PaymentRequestEvent;
import uk.gov.pay.directdebit.payments.model.Transaction;

import static uk.gov.pay.directdebit.payments.model.PaymentRequestEvent.directDebitDetailsConfirmed;
import static uk.gov.pay.directdebit.payments.model.PaymentRequestEvent.directDebitDetailsReceived;
import static uk.gov.pay.directdebit.payments.model.PaymentRequestEvent.mandateCreated;
import static uk.gov.pay.directdebit.payments.model.PaymentRequestEvent.payerCreated;
import static uk.gov.pay.directdebit.payments.model.PaymentRequestEvent.tokenExchanged;

public class PaymentRequestEventService {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(PaymentRequestEventService.class);

    private final PaymentRequestEventDao paymentRequestEventDao;

    public PaymentRequestEventService(PaymentRequestEventDao paymentRequestEventDao) {
        this.paymentRequestEventDao = paymentRequestEventDao;
    }

    private void insertEventFor(Transaction charge, PaymentRequestEvent event) {
        LOGGER.info("Creating event for {} {}: {} - {}",
                charge.getType(), charge.getPaymentRequestExternalId(),
                event.getEventType(), event.getEvent());
        paymentRequestEventDao.insert(event);
    }

    void insertEventFor(PaymentRequest paymentRequest, PaymentRequestEvent event) {
        LOGGER.info("Creating event for payment request {}: {} - {}",
                paymentRequest.getExternalId(), event.getEventType(), event.getEvent());
        paymentRequestEventDao.insert(event);
    }

    public void registerDirectDebitReceivedEventFor(Transaction charge) {
        insertEventFor(charge, directDebitDetailsReceived(charge.getPaymentRequestId()));
    }

    public void registerDirectDebitConfirmedEventFor(Transaction charge) {
        PaymentRequestEvent event = directDebitDetailsConfirmed(charge.getPaymentRequestId());
        insertEventFor(charge, event);
    }

    public void registerPayerCreatedEventFor(Transaction charge) {
        PaymentRequestEvent event = payerCreated(charge.getPaymentRequestId());
        insertEventFor(charge, event);
    }

    public void registerMandateCreatedEventFor(Transaction charge) {
        PaymentRequestEvent event = mandateCreated(charge.getPaymentRequestId());
        insertEventFor(charge, event);
    }

    public void registerTokenExchangedEventFor(Transaction charge) {
        PaymentRequestEvent event = tokenExchanged(charge.getPaymentRequestId());
        insertEventFor(charge, event);
    }
}