package org.apereo.cas.logging.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.support.CookieRetrievingCookieGenerator;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import java.util.Enumeration;

/**
 * This is {@link ThreadContextMDCServletFilter}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@AllArgsConstructor
public class ThreadContextMDCServletFilter implements Filter {

    private final TicketRegistrySupport ticketRegistrySupport;
    private final CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    /**
     * Does nothing.
     *
     * @param filterConfig filter initial configuration. Ignored.
     */
    @Override
    public void init(final FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        try {
            final var request = (HttpServletRequest) servletRequest;

            addContextAttribute("remoteAddress", request.getRemoteAddr());
            addContextAttribute("remoteUser", request.getRemoteUser());
            addContextAttribute("serverName", request.getServerName());
            addContextAttribute("serverPort", String.valueOf(request.getServerPort()));
            addContextAttribute("locale", request.getLocale().getDisplayName());
            addContextAttribute("contentType", request.getContentType());
            addContextAttribute("contextPath", request.getContextPath());
            addContextAttribute("localAddress", request.getLocalAddr());
            addContextAttribute("localPort", String.valueOf(request.getLocalPort()));
            addContextAttribute("remotePort", String.valueOf(request.getRemotePort()));
            addContextAttribute("pathInfo", request.getPathInfo());
            addContextAttribute("protocol", request.getProtocol());
            addContextAttribute("authType", request.getAuthType());
            addContextAttribute("method", request.getMethod());
            addContextAttribute("queryString", request.getQueryString());
            addContextAttribute("requestUri", request.getRequestURI());
            addContextAttribute("scheme", request.getScheme());
            addContextAttribute("timezone", TimeZone.getDefault().getDisplayName());

            final var params = request.getParameterMap();
            params.keySet().forEach(k -> {
                final var values = params.get(k);
                addContextAttribute(k, Arrays.toString(values));
            });
            
            Collections.list(request.getAttributeNames()).forEach(a -> addContextAttribute(a, request.getAttribute(a)));
            final var requestHeaderNames = request.getHeaderNames();
            if (requestHeaderNames != null) {
                Collections.list(requestHeaderNames).forEach(h -> addContextAttribute(h, request.getHeader(h)));
            }

            final var cookieValue = this.ticketGrantingTicketCookieGenerator.retrieveCookieValue(request);
            if (StringUtils.isNotBlank(cookieValue)) {
                final var p = this.ticketRegistrySupport.getAuthenticatedPrincipalFrom(cookieValue);
                if (p != null) {
                    addContextAttribute("principal", p.getId());
                }
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

    private static void addContextAttribute(final String attributeName, final Object value) {
        final var result = value != null ? value.toString() : null;
        if (StringUtils.isNotBlank(result)) {
            MDC.put(attributeName, result);
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void destroy() {
    }
}
