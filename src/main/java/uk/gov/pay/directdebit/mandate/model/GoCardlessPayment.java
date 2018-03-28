package uk.gov.pay.directdebit.mandate.model;

import java.time.LocalDate;

public class GoCardlessPayment {

    private Long id;
    private Long transactionId;
    private String paymentId;
    private LocalDate chargeDate;

    public GoCardlessPayment(Long id, Long transactionId, String paymentId, LocalDate chargeDate) {
        this.id = id;
        this.transactionId = transactionId;
        this.paymentId = paymentId;
        this.chargeDate = chargeDate;
    }

    public GoCardlessPayment(Long transactionId, String paymentId, LocalDate chargeDate) {
        this(null, transactionId, paymentId, chargeDate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoCardlessPayment that = (GoCardlessPayment) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (!transactionId.equals(that.transactionId)) return false;
        if (!paymentId.equals(that.paymentId)) return false;
        return chargeDate.equals(that.chargeDate);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + transactionId.hashCode();
        result = 31 * result + paymentId.hashCode();
        result = 31 * result + chargeDate.hashCode();
        return result;
    }
}
