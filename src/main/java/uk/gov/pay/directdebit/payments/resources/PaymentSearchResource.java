package uk.gov.pay.directdebit.payments.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.pay.directdebit.payments.params.PaymentViewSearchParams;
import uk.gov.pay.directdebit.payments.services.PaymentSearchService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.directdebit.payments.params.PaymentViewSearchParams.PaymentViewSearchParamsBuilder.aPaymentViewSearchParams;

@Path("/")
public class PaymentSearchResource {

    private final PaymentSearchService paymentSearchService;

    @Inject
    public PaymentSearchResource(PaymentSearchService paymentSearchService) {
        this.paymentSearchService = paymentSearchService;
    }

    @GET
    @Path("/v1/api/accounts/{accountId}/payments")
    @Produces(APPLICATION_JSON)
    @Timed
    public Response searchPayments(
            @PathParam("accountId") String accountExternalId,
            @QueryParam("page") Integer pageNumber,
            @QueryParam("display_size") Integer displaySize,
            @QueryParam("from_date") String fromDate,
            @QueryParam("to_date") String toDate,
            @QueryParam("reference") String reference,
            @QueryParam("amount") Long amount,
            @QueryParam("mandate_id") String mandateId,
            @QueryParam("state") String state,
            @Context UriInfo uriInfo) {

        PaymentViewSearchParams searchParams = aPaymentViewSearchParams(accountExternalId)
                .withPage(pageNumber)
                .withDisplaySize(displaySize)
                .withFromDateString(fromDate)
                .withToDateString(toDate)
                .withReference(reference)
                .withAmount(amount)
                .withMandateId(mandateId)
                .withState(state)
                .build();
        return Response.ok().entity(paymentSearchService.withUriInfo(uriInfo).getPaymentSearchResponse(searchParams)).build();
    }
}