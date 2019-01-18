package uk.gov.pay.directdebit.webhook.gocardless.services.handlers;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.directdebit.payments.model.DirectDebitEvent;
import uk.gov.pay.directdebit.payments.model.GoCardlessEvent;
import uk.gov.pay.directdebit.payments.services.GoCardlessEventService;
import uk.gov.pay.directdebit.payments.services.TransactionService;

public abstract class GoCardlessHandler implements GoCardlessActionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoCardlessHandler.class);

    protected TransactionService transactionService;
    GoCardlessEventService goCardlessService;

    protected abstract Optional<DirectDebitEvent> process(GoCardlessEvent event);

    GoCardlessHandler(TransactionService transactionService,
                      GoCardlessEventService goCardlessService) {
        this.transactionService = transactionService;
        this.goCardlessService = goCardlessService;
    }

    public void handle(GoCardlessEvent event) {
        process(event).ifPresent((directDebitEvent) -> {
            event.setEventId(directDebitEvent.getId());
            goCardlessService.updateInternalEventId(event);
            LOGGER.info("handled gocardless event with id: {}, resource type: {}", event.getEventId(), event.getResourceType().toString());
        });
    }
}
