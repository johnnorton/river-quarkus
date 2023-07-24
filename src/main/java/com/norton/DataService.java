package com.norton;

import com.google.common.collect.Maps;
import io.calleridentity.dynamo.util.DynamoItemHelper;
import lombok.Builder;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

import static io.calleridentity.dynamo.util.DynamoItemHelper.SK;

@Builder
public class DataService
{

  private final DynamoDbClient dynamoDbClient;
  private static final String TABLE_NAME = "river";


  public void insertMessage(final String river, final String riverDate)
  {
    dynamoDbClient.putItem(putItemRequest(DynamoItemHelper.builder()
        .pk(river + riverDate)
        .sk(DateTime.now().toString())
        .build()));
  }

  public int secondsSinceLastMessageSent(final String river, final String riverDate)
  {
    final Map<String, AttributeValue> expressionAttributeValues = Maps.newHashMap();
    expressionAttributeValues.put(":pk", AttributeValue.builder().s(river + riverDate).build());
    final QueryResponse queryResponse = dynamoDbClient.query((b) -> b.tableName(TABLE_NAME)
        .limit(1)
        .scanIndexForward(false)
        .keyConditionExpression("PK = :pk").expressionAttributeValues(expressionAttributeValues));

    if (queryResponse.items().size() == 0)
    {
      return -1;
    }
    return Seconds
        .secondsBetween(DateTime.parse(queryResponse.items().get(0).get(SK).s()), DateTime.now())
        .getSeconds();
  }

  public static GetItemRequest getItemRequest(final Map<String, AttributeValue> key)
  {
    return GetItemRequest.builder().tableName(TABLE_NAME).key(key).consistentRead(true).build();
  }

  public static DeleteItemRequest deleteItemRequest(final Map<String, AttributeValue> key)
  {
    return DeleteItemRequest.builder().tableName(TABLE_NAME).key(key).build();
  }

  public static PutItemRequest putItemRequest(final Map<String, AttributeValue> values,
      final String conditionExpression)
  {
    final PutItemRequest.Builder builder =
        PutItemRequest.builder().tableName(TABLE_NAME).item(values);
    if (null != conditionExpression)
    {
      builder.conditionExpression(conditionExpression);
    }
    return builder.build();
  }

  public static PutItemRequest putItemRequest(final Map<String, AttributeValue> values)
  {
    return putItemRequest(values, null);

  }
}
