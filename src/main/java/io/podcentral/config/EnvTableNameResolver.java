package io.podcentral.config;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.DefaultTableNameResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EnvTableNameResolver extends DefaultTableNameResolver {
  @Override
  public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
    String defaultTableName = super.getTableName(clazz, config);
    log.trace("DefaultTableName for {}={}", clazz.getName(), defaultTableName);
    String envTableName = System.getenv(defaultTableName.toUpperCase() + "_TABLE");
    if (envTableName != null) {
      log.trace("EnvTableName for {}={}", clazz.getName(), envTableName);
      return envTableName;
    } else {
      return defaultTableName;
    }
  }
}
