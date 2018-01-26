package uk.gov.pay.directdebit.gatewayaccounts.dao.mapper;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import uk.gov.pay.directdebit.gatewayaccounts.model.GatewayAccount;
import uk.gov.pay.directdebit.gatewayaccounts.model.PaymentProvider;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GatewayAccountMapper implements ResultSetMapper<GatewayAccount> {

    private static final String ID_COLUMN = "id";
    private static final String PAYMENT_PROVIDER_COLUMN = "payment_provider";
    private static final String SERVICE_NAME_COLUMN = "service_name";
    private static final String TYPE_COLUMN = "type";
    private static final String DESCRIPTION_COLUMN = "description";
    private static final String ANALYTICS_ID_COLUMN = "analytics_id";

    @Override
    public GatewayAccount map(int index, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new GatewayAccount(
                resultSet.getLong(ID_COLUMN),
                PaymentProvider.fromString(resultSet.getString(PAYMENT_PROVIDER_COLUMN)),
                GatewayAccount.Type.fromString(resultSet.getString(TYPE_COLUMN)),
                resultSet.getString(SERVICE_NAME_COLUMN),
                resultSet.getString(DESCRIPTION_COLUMN),
                resultSet.getString(ANALYTICS_ID_COLUMN)
        );
    }
}
