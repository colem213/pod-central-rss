package io.podcentral.config;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.DefaultTableNameResolver;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class EnvTableNameResolver extends DefaultTableNameResolver {
  private Map<String, String> stageVariables;

  @Override
  public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
    String defaultTableName = super.getTableName(clazz, config);
    log.trace("DefaultTableName for {}={}", clazz.getName(), defaultTableName);
    String envTableName = stageVariables.get(defaultTableName.toUpperCase() + "_TABLE");
    if (envTableName != null) {
      log.trace("EnvTableName for {}={}", clazz.getName(), envTableName);
      return envTableName;
    } else {
      return defaultTableName;
    }
  }
}
