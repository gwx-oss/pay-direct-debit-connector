package uk.gov.pay.directdebit.payments.clients;

import com.gocardless.resources.BankDetailsLookup;
import com.gocardless.resources.Customer;
import com.gocardless.resources.CustomerBankAccount;
import com.gocardless.resources.Payment;
import java.time.LocalDate;
import javax.inject.Inject;
import uk.gov.pay.directdebit.mandate.model.GoCardlessMandate;
import uk.gov.pay.directdebit.mandate.model.GoCardlessPayment;
import uk.gov.pay.directdebit.mandate.model.Mandate;
import uk.gov.pay.directdebit.payers.model.BankAccountDetails;
import uk.gov.pay.directdebit.payers.model.GoCardlessBankAccountLookup;
import uk.gov.pay.directdebit.payers.model.GoCardlessCustomer;
import uk.gov.pay.directdebit.payers.model.Payer;
import uk.gov.pay.directdebit.payments.model.Transaction;

import static com.gocardless.resources.BankDetailsLookup.AvailableDebitScheme.BACS;

public class GoCardlessClientFacade {

    private final GoCardlessClientWrapper goCardlessClientWrapper;

    @Inject
    public GoCardlessClientFacade(GoCardlessClientWrapper goCardlessClientWrapper) {
        this.goCardlessClientWrapper = goCardlessClientWrapper;
    }

    public String createCustomer(String mandateExternalId, Payer payer) {
        Customer customer = goCardlessClientWrapper.createCustomer(mandateExternalId, payer);
        return customer.getId();
    }

    public String createCustomerBankAccount(String mandateExternalId, GoCardlessCustomer customer,
                                                        String accountHolderName, String sortCode, String accountNumber) {
        CustomerBankAccount gcCustomerBankAccount = goCardlessClientWrapper.createCustomerBankAccount(mandateExternalId, customer,
                accountHolderName, sortCode, accountNumber);
        return gcCustomerBankAccount.getId();
    }

    public GoCardlessMandate createMandate(Mandate mandate, GoCardlessCustomer customer) {
        com.gocardless.resources.Mandate gcMandate = goCardlessClientWrapper.createMandate(mandate.getExternalId(), customer);
        return new GoCardlessMandate(
                mandate.getId(),
                gcMandate.getId(),
                gcMandate.getReference());
    }

    public GoCardlessPayment createPayment(Transaction transaction, GoCardlessMandate mandate) {
        Payment gcPayment = goCardlessClientWrapper.createPayment(transaction, mandate);
        return new GoCardlessPayment(
                transaction.getId(),
                gcPayment.getId(),
                LocalDate.parse(gcPayment.getChargeDate()));
    }

    public GoCardlessBankAccountLookup validate(BankAccountDetails bankAccountDetails) {
        BankDetailsLookup gcBankDetailsLookup = goCardlessClientWrapper.validate(bankAccountDetails);
        return new GoCardlessBankAccountLookup(
                gcBankDetailsLookup.getBankName(),
                gcBankDetailsLookup.getAvailableDebitSchemes().contains(BACS));
    }

}
