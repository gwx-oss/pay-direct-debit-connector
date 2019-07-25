package uk.gov.pay.directdebit.mandate.model;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.gov.pay.directdebit.payments.model.DirectDebitEvent;
import uk.gov.pay.directdebit.payments.model.DirectDebitStatesGraph;

import static uk.gov.pay.directdebit.mandate.model.MandateState.ACTIVE;
import static uk.gov.pay.directdebit.mandate.model.MandateState.AWAITING_DIRECT_DEBIT_DETAILS;
import static uk.gov.pay.directdebit.mandate.model.MandateState.USER_SETUP_CANCELLED;
import static uk.gov.pay.directdebit.mandate.model.MandateState.CREATED;
import static uk.gov.pay.directdebit.mandate.model.MandateState.USER_SETUP_EXPIRED;
import static uk.gov.pay.directdebit.mandate.model.MandateState.FAILED;
import static uk.gov.pay.directdebit.mandate.model.MandateState.PENDING;
import static uk.gov.pay.directdebit.mandate.model.MandateState.SUBMITTED_TO_PROVIDER;
import static uk.gov.pay.directdebit.mandate.model.MandateState.USER_CANCEL_NOT_ELIGIBLE;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.DIRECT_DEBIT_DETAILS_CONFIRMED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_ACTIVE;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_CANCELLED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_EXPIRED_BY_SYSTEM;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_FAILED;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.MANDATE_PENDING;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.PAYMENT_CANCELLED_BY_USER;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.PAYMENT_CANCELLED_BY_USER_NOT_ELIGIBLE;
import static uk.gov.pay.directdebit.payments.model.DirectDebitEvent.SupportedEvent.TOKEN_EXCHANGED;

public class MandateStatesGraph extends DirectDebitStatesGraph<MandateState> {

    public static MandateState initialState() {
        return PENDING;
    }
    
    public static MandateStatesGraph getStates() {
        return new MandateStatesGraph();
    }
    
    @Override
    protected ImmutableValueGraph<MandateState, DirectDebitEvent.SupportedEvent> buildStatesGraph() {
        MutableValueGraph<MandateState, SupportedEvent> graph = ValueGraphBuilder
                .directed()
                .build();

        addNodes(graph, MandateState.values());

        graph.putEdgeValue(CREATED, AWAITING_DIRECT_DEBIT_DETAILS, TOKEN_EXCHANGED);
        graph.putEdgeValue(AWAITING_DIRECT_DEBIT_DETAILS, SUBMITTED_TO_PROVIDER, DIRECT_DEBIT_DETAILS_CONFIRMED);
        graph.putEdgeValue(AWAITING_DIRECT_DEBIT_DETAILS, USER_SETUP_CANCELLED, PAYMENT_CANCELLED_BY_USER);
        graph.putEdgeValue(AWAITING_DIRECT_DEBIT_DETAILS, USER_CANCEL_NOT_ELIGIBLE, PAYMENT_CANCELLED_BY_USER_NOT_ELIGIBLE);
        
        graph.putEdgeValue(CREATED, USER_SETUP_EXPIRED, MANDATE_EXPIRED_BY_SYSTEM);
        graph.putEdgeValue(AWAITING_DIRECT_DEBIT_DETAILS, USER_SETUP_EXPIRED, MANDATE_EXPIRED_BY_SYSTEM);
        graph.putEdgeValue(SUBMITTED_TO_PROVIDER, USER_SETUP_EXPIRED, MANDATE_EXPIRED_BY_SYSTEM);

        graph.putEdgeValue(SUBMITTED_TO_PROVIDER, PENDING, MANDATE_PENDING);
        graph.putEdgeValue(PENDING, ACTIVE, MANDATE_ACTIVE);
        graph.putEdgeValue(PENDING, FAILED, MANDATE_FAILED);
        graph.putEdgeValue(PENDING, USER_SETUP_CANCELLED, MANDATE_CANCELLED);
        graph.putEdgeValue(ACTIVE, USER_SETUP_CANCELLED, MANDATE_CANCELLED);

        return ImmutableValueGraph.copyOf(graph);
    }
}
