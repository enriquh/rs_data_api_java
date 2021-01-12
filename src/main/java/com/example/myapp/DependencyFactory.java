
package com.example.myapp;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of RedshiftDataClient
     */
    public static RedshiftDataClient redshiftDataClient() {
        return RedshiftDataClient.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.US_EAST_1)
                       .httpClientBuilder(UrlConnectionHttpClient.builder())
                       .build();
    }
}
