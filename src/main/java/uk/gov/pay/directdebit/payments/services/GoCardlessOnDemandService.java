package uk.gov.pay.directdebit.payments.services;

import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.slf4j.Logger;
import uk.gov.pay.directdebit.app.logger.PayLoggerFactory;
import uk.gov.pay.directdebit.gatewayaccounts.model.GatewayAccount;
import uk.gov.pay.directdebit.mandate.model.ConfirmationDetails;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandate;
import uk.gov.pay.directdebit.mandate.model.GoCardlessPayment;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.payers.api.BankAccountValidationResponse;
import uk.gov.pay.directdebit.payers.model.GoCardlessCustomer;
import uk.gov.pay.directdebit.payments.model.DirectDebitPaymentProvider;
import uk.gov.pay.directdebit.payments.models.GoCardlessConfirmation;

import javax.inject.Inject;
import java.util.Map;

public class GoCardlessOnDemandService implements DirectDebitPaymentProvider {
    private static final Logger LOGGER = PayLoggerFactory.getLogger(GoCardlessOnDemandService.class);

    private final GoCardlessService goCardlessService;

    @Inject
    public GoCardlessOnDemandService(GoCardlessService goCardlessService) {
        this.goCardlessService = goCardlessService;
    }

    @Override
    public void confirm(ConfirmationDetails confirmationDetails) {
        Mandate mandate = confirmationDetails.getMandate();
        LOGGER.info("Confirming direct debit details, mandate with id: {}", mandate.getExternalId());
        GoCardlessCustomer goCardlessCustomer = goCardlessService.createCustomer(mandate);
        String gcBankAccountId = goCardlessService.createCustomerBankAccount(mandate, 
                goCardlessCustomer,
                confirmationDetails.getBankAccountDetails()
        );
        goCardlessCustomer.setCustomerBankAccountId(gcBankAccountId);
        GoCardlessMandate goCardlessMandate = goCardlessService.createMandate(mandate, goCardlessCustomer);

        persist(GoCardlessConfirmation.from(goCardlessMandate, goCardlessCustomer), mandate);
    }

    @Override
    public uk.gov.pay.directdebit.payments.model.Transaction collect(GatewayAccount gatewayAccount, Map<String, String> collectPaymentRequest) {
        LOGGER.info("Collecting payment for GOCARDLESS, mandate with id: {}", mandateExternalId);

        GoCardlessMandate goCardlessMandate = goCardlessService.findGoCardlessMandateForMandate(mandate);
        GoCardlessPayment payment = goCardlessService.createPayment(transaction, goCardlessMandate);
        LOGGER.info("Submitted payment collection for GOCARDLESS, for mandate with id: {}", mandateExternalId);
        return transaction;
    }

    @Override
    public BankAccountValidationResponse validate(String mandateExternalId, Map<String, String> bankAccountDetailsRequest) {
        return goCardlessService.validate(mandateExternalId, bankAccountDetailsRequest);
    }

    @Transaction
    private void persist(GoCardlessConfirmation gcConfirmation, Mandate mandate) {
        goCardlessService.persistGoCardlessMandate(gcConfirmation.getMandate());
        goCardlessService.persistGoCardlessCustomer(gcConfirmation.getCustomer());
        goCardlessService.updateMandateReference(mandate, gcConfirmation.getMandate().getGoCardlessReference());
    }
}    