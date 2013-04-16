package org.jenkinsci.plugins.knappsack;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import hudson.util.Secret;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.jenkinsci.plugins.knappsack.models.Application;
import org.jenkinsci.plugins.knappsack.models.TokenResponse;

import javax.ws.rs.core.MediaType;

public class KnappsackAPI {

    private final String knappsackURL;
    private final String userName;
    private final Secret userPassword;

    public KnappsackAPI(String knappsackURL, String userName, Secret userPassword) {
        this.knappsackURL = knappsackURL;
        this.userName = userName;
        this.userPassword = userPassword;
    }

    public TokenResponse getTokenResponse() {
        String url = knappsackURL + "/oauth/token";
        url = url + "?client_id=mobile_api_client&client_secret=kzI7QNsbne8KOlS&grant_type=password&username=" + userName + "&password=" + userPassword;
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to connect to " + knappsackURL + " with the given user name and password.  HTTP status: " + response.getStatus());
        }

        return response.getEntity(TokenResponse.class);
    }

    public Application[] getApplications() {
        TokenResponse tokenResponse = getTokenResponse();
        String url = knappsackURL + "/api/v1/applications";
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(url);

        String token = tokenResponse.getAccess_token();
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + token).get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to retrieve applications from " + knappsackURL + " with the given user name and password.  HTTP status: " + response.getStatus());
        }

        return response.getEntity(Application[].class);
    }
}
