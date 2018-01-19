package uk.gov.pay.directdebit.app.filters;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import uk.gov.pay.directdebit.app.logger.PayLoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class LoggingFilter implements Filter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final Logger LOGGER = PayLoggerFactory.getLogger(LoggingFilter.class);

    public static String currentRequestId() {
        return MDC.get(HEADER_REQUEST_ID);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws
            IOException,
            ServletException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        String requestURL = ((HttpServletRequest) servletRequest).getRequestURI();
        String requestMethod = ((HttpServletRequest) servletRequest).getMethod();
        String requestId = StringUtils.defaultString(((HttpServletRequest) servletRequest).getHeader(HEADER_REQUEST_ID), "");

        MDC.put(HEADER_REQUEST_ID, requestId);

        LOGGER.info(format("%s to %s began", requestMethod, requestURL));
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable throwable) {
            LOGGER.error("Exception - direct debit connector request - " + requestURL + " - exception - " + throwable.getMessage(), throwable);
        } finally {
            LOGGER.info(format("%s to %s ended - total time %dms", requestMethod, requestURL,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            stopwatch.stop();
        }
    }

    @Override
    public void destroy() {
    }

}