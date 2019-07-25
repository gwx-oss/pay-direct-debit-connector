package uk.gov.pay.directdebit.payments.model;

import uk.gov.pay.directdebit.payments.api.ExternalPaymentState;

import static uk.gov.pay.directdebit.payments.api.ExternalPaymentState.EXTERNAL_CANCELLED_USER_NOT_ELIGIBLE;
import static uk.gov.pay.directdebit.payments.api.ExternalPaymentState.EXTERNAL_FAILED;
import static uk.gov.pay.directdebit.payments.api.ExternalPaymentState.EXTERNAL_PENDING;
import static uk.gov.pay.directdebit.payments.api.ExternalPaymentState.EXTERNAL_STARTED;
import static uk.gov.pay.directdebit.payments.api.ExternalPaymentState.EXTERNAL_SUCCESS;

public enum PaymentState implements DirectDebitState {

    CREATED(EXTERNAL_STARTED),
    SUBMITTED_TO_PROVIDER(EXTERNAL_PENDING),
    FAILED(EXTERNAL_FAILED),
    CANCELLED(EXTERNAL_FAILED),
    EXPIRED(EXTERNAL_FAILED),
    USER_CANCEL_NOT_ELIGIBLE(EXTERNAL_CANCELLED_USER_NOT_ELIGIBLE),
    PAID_OUT(EXTERNAL_SUCCESS);

    private ExternalPaymentState externalState;

    PaymentState(ExternalPaymentState externalState) {
        this.externalState = externalState;
    }

    public ExternalPaymentState toExternal() {
        return externalState;
    }

    public String toSingleQuoteString() {
        return "'" + this + "'";
    }
}
