package com.example.myapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;
import software.amazon.awssdk.services.redshiftdata.model.*;

import java.util.List;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<Object, Object> {
    private static RedshiftDataClient redshiftDataClient = null;

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.

        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
        redshiftDataClient = App.getRedshiftDataClient();
    }

    @Override
    public Object handleRequest(final Object input, final Context context) {
        try {
            RedshiftDataClient client;

            client = App.getRedshiftDataClient();

            String secretArn = System.getenv("RS_SECRET_ARN");
            String clusterId = System.getenv("RS_CLUSTER_ID");
            String databaseName = System.getenv("RS_CLUSTER_DATABASE");
            String sql = System.getenv("SQL_STATEMENT");

            ExecuteStatementRequest executeStatementRequest= ExecuteStatementRequest.builder().clusterIdentifier(clusterId).database(databaseName)
                    .secretArn(secretArn)
                    .sql(sql).build();

            ExecuteStatementResponse response = client.executeStatement(executeStatementRequest);

            String id = response.id();

            DescribeStatementRequest describeStatementRequest = DescribeStatementRequest.builder().id(id).build();

            DescribeStatementResponse describeStatementResponse = client.describeStatement(describeStatementRequest);

            while (describeStatementResponse.status() == StatusString.PICKED || describeStatementResponse.status() == StatusString.STARTED || describeStatementResponse.status() == StatusString.SUBMITTED) {
                describeStatementResponse = client.describeStatement(describeStatementRequest);
                System.out.println("Checking status for statement with id "+ id + " which is in " + describeStatementResponse.status().toString() + " status");
                Thread.sleep(5000);
            }

            if (describeStatementResponse.status() == StatusString.FINISHED){
                GetStatementResultRequest getStatementResultRequest = GetStatementResultRequest.builder().id(id).build();
                GetStatementResultResponse getStatementResultResponse = client.getStatementResult(getStatementResultRequest);
                int counter = 1;
                if (getStatementResultResponse.hasRecords() == true) {
                    for (List<Field> row: getStatementResultResponse.records()){
                        String line = "";
                        for (Field field: row){
                            line+= " " + field.toString();
                        }
                        System.out.println("Row " + Integer.toString(counter));
                        System.out.println(line);
                        counter++;
                    }
                }

            } else {
                System.out.println("There was an error running the query with id "+id+" status was "+describeStatementResponse.status()+ " and error was "+ describeStatementResponse.error());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return input;
    }

    public static RedshiftDataClient getRedshiftDataClient() {
        if (redshiftDataClient == null)
            redshiftDataClient = RedshiftDataClient.builder().region(Region.of(System.getenv("RS_CLUSTER_REGION"))).credentialsProvider(DefaultCredentialsProvider.builder().profileName("MyAccount").build()).build();

        return redshiftDataClient;
    }

    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        app.handleRequest(null,null);
    }


}
