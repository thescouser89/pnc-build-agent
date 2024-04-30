package org.jboss.pnc.buildagent.server;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.keycloak.adapters.servlet.KeycloakOIDCFilter.CONFIG_FILE_PARAM;

/**
 * Alternative implementation of KeycloakOIDCFilter, which does not use the .well-known/openid-configuration endpoint
 * and instead validates the authentication token offline using jjwt and the provided realm-public-key
 */
public class KeycloakOfflineOIDCFilter implements Filter {

    private final static Logger log = Logger.getLogger("" + KeycloakOfflineOIDCFilter.class);
    private KeycloakOfflineOIDCFilterConfiguration configuration;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);

        String fp = filterConfig.getInitParameter(CONFIG_FILE_PARAM);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            configuration = objectMapper.readValue(new File(fp), KeycloakOfflineOIDCFilterConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String authToken = request.getHeader("Authorization").replace("Bearer", "").trim();
        try {
            KeycloakOfflineTokenVerifier.verify(authToken, configuration.getPublicKey(), configuration.getAuthServerUrl());
        } catch (Exception e) {
            log.warning("Authorization failed with error: " + e);
            response.sendError(403);
        }
    }


    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
