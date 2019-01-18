package uk.gov.pay.directdebit.notifications.services;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import uk.gov.pay.directdebit.app.config.DirectDebitConfig;
import org.slf4j.LoggerFactory;
import uk.gov.pay.directdebit.common.model.subtype.SunName;
import uk.gov.pay.directdebit.common.services.SunService;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.notifications.clients.AdminUsersClient;
import uk.gov.pay.directdebit.notifications.model.EmailPayload.EmailTemplate;
import uk.gov.pay.directdebit.payments.model.Transaction;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserNotificationService {

    private static final String DD_GUARANTEE_KEY = "dd guarantee link";
    private static final String MANDATE_REFERENCE_KEY = "mandate reference";
    private static final String STATEMENT_NAME_KEY = "statement name";
    private static final String COLLECTION_DATE_KEY = "collection date";
    private static final String AMOUNT_KEY = "amount";
    private static final String BANK_ACCOUNT_LAST_DIGITS_KEY = "bank account last 2 digits";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserNotificationService.class);

    private AdminUsersClient adminUsersClient;
    private final DirectDebitConfig directDebitConfig;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final SunService sunService;

    @Inject
    public UserNotificationService(AdminUsersClient adminUsersClient, DirectDebitConfig directDebitConfig, SunService sunService) {
        this.adminUsersClient = adminUsersClient;
        this.directDebitConfig = directDebitConfig;
        this.sunService = sunService;
    }

    public void sendMandateFailedEmailFor(Mandate mandate) {
        adminUsersClient.sendEmail(EmailTemplate.MANDATE_FAILED, mandate,
                ImmutableMap.of(
                        MANDATE_REFERENCE_KEY, mandate.getMandateReference(),
                        DD_GUARANTEE_KEY, directDebitConfig.getLinks().getDirectDebitGuaranteeUrl()
                )
        );
    }

    public void sendOnDemandMandateCreatedEmailFor(Mandate mandate) {
        EmailTemplate template = EmailTemplate.ON_DEMAND_MANDATE_CREATED;
        Optional<SunName> sunName = sunService.getSunNameFor(mandate);
        if (!sunName.isPresent()) {
            logMissingSunName(template, mandate);
            return;
        }

        adminUsersClient.sendEmail(template, mandate,
                ImmutableMap.<String, String>builder()
                        .put(MANDATE_REFERENCE_KEY, mandate.getMandateReference())
                        .put(BANK_ACCOUNT_LAST_DIGITS_KEY, mandate.getPayer().getAccountNumberLastTwoDigits())
                        .put(STATEMENT_NAME_KEY, sunName.get().toString())
                        .put(DD_GUARANTEE_KEY, directDebitConfig.getLinks().getDirectDebitGuaranteeUrl())
                        .build()
        );
    }

    public void sendMandateCancelledEmailFor(Mandate mandate) {
        adminUsersClient.sendEmail(EmailTemplate.MANDATE_CANCELLED, mandate,
                ImmutableMap.of(
                        MANDATE_REFERENCE_KEY, mandate.getMandateReference(),
                        DD_GUARANTEE_KEY, directDebitConfig.getLinks().getDirectDebitGuaranteeUrl()
                )
        );
    }

    public void sendOneOffPaymentConfirmedEmailFor(Transaction transaction, LocalDate earliestChargeDate) {
        sendPaymentConfirmedEmailFor(EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED, transaction, earliestChargeDate);
    }

    public void sendOnDemandPaymentConfirmedEmailFor(Transaction transaction, LocalDate earliestChargeDate) {
        sendPaymentConfirmedEmailFor(EmailTemplate.ON_DEMAND_PAYMENT_CONFIRMED, transaction, earliestChargeDate);
    }

    private void sendPaymentConfirmedEmailFor(EmailTemplate template, Transaction transaction, LocalDate earliestChargeDate) {
        Mandate mandate = transaction.getMandate();
        Optional<SunName> sunName = sunService.getSunNameFor(mandate);
        if (!sunName.isPresent()) {
            logMissingSunName(template, mandate);
            return;
        }

        adminUsersClient.sendEmail(template, mandate,
                ImmutableMap.<String, String>builder()
                        .put(AMOUNT_KEY, formatToPounds(transaction.getAmount()))
                        .put(COLLECTION_DATE_KEY, DATE_TIME_FORMATTER.format(earliestChargeDate))
                        .put(MANDATE_REFERENCE_KEY, mandate.getMandateReference())
                        .put(BANK_ACCOUNT_LAST_DIGITS_KEY, mandate.getPayer().getAccountNumberLastTwoDigits())
                        .put(STATEMENT_NAME_KEY, sunName.get().toString())
                        .put(DD_GUARANTEE_KEY, directDebitConfig.getLinks().getDirectDebitGuaranteeUrl())
                        .build());
    }

    public void sendPaymentFailedEmailFor(Transaction transaction) {
        adminUsersClient.sendEmail(EmailTemplate.PAYMENT_FAILED, transaction.getMandate(),
                ImmutableMap.of(
                        DD_GUARANTEE_KEY, directDebitConfig.getLinks().getDirectDebitGuaranteeUrl()
                ));
    }

    private static String formatToPounds(long amountInPence) {
        return BigDecimal.valueOf(amountInPence, 2).toString();
    }

    private void logMissingSunName(EmailTemplate template, Mandate mandate) {
        LOGGER.error("Mandate {} does not have a Service User Number. " +
                "Email with template {} is not being sent.", mandate.getExternalId(), template);
    }

}
