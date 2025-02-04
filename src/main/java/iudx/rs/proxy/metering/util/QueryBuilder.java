package iudx.rs.proxy.metering.util;

import static iudx.rs.proxy.metering.util.Constants.API;
import static iudx.rs.proxy.metering.util.Constants.API_QUERY;
import static iudx.rs.proxy.metering.util.Constants.CONSUMER_ID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.rs.proxy.metering.util.Constants.CONSUMER_ID_TIME_INTERVAL_READ_QUERY;
import static iudx.rs.proxy.metering.util.Constants.CONSUMER_ID;
import static iudx.rs.proxy.metering.util.Constants.DATABASE_TABLE_NAME;
import static iudx.rs.proxy.metering.util.Constants.END_TIME;
import static iudx.rs.proxy.metering.util.Constants.ERROR;
import static iudx.rs.proxy.metering.util.Constants.ID;
import static iudx.rs.proxy.metering.util.Constants.ID_QUERY;
import static iudx.rs.proxy.metering.util.Constants.IID;
import static iudx.rs.proxy.metering.util.Constants.INVALID_DATE_DIFFERENCE;
import static iudx.rs.proxy.metering.util.Constants.INVALID_DATE_TIME;
import static iudx.rs.proxy.metering.util.Constants.INVALID_PROVIDER_ID;
import static iudx.rs.proxy.metering.util.Constants.LAST_ID;
import static iudx.rs.proxy.metering.util.Constants.LATEST_ID;
import static iudx.rs.proxy.metering.util.Constants.ORDER_BY_AND_LIMIT;
import static iudx.rs.proxy.metering.util.Constants.PROVIDER_ID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.rs.proxy.metering.util.Constants.PROVIDER_ID_TIME_INTERVAL_READ_QUERY;
import static iudx.rs.proxy.metering.util.Constants.PROVIDER_ID;
import static iudx.rs.proxy.metering.util.Constants.QUERY_KEY;
import static iudx.rs.proxy.metering.util.Constants.RESOURCE_ID;
import static iudx.rs.proxy.metering.util.Constants.RESOURCE_QUERY;
import static iudx.rs.proxy.metering.util.Constants.START_TIME;
import static iudx.rs.proxy.metering.util.Constants.USER_ID;
import static iudx.rs.proxy.metering.util.Constants.USER_ID_QUERY;
import static iudx.rs.proxy.metering.util.Constants.WHERE;
import static iudx.rs.proxy.metering.util.Constants.WRITE_QUERY;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildReadingQuery(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String resourceId = request.getString(RESOURCE_ID);
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String providerID = request.getString(PROVIDER_ID);
    String consumerID = request.getString(CONSUMER_ID);
    String iid = request.getString(IID);
    String databaseTableName = request.getString(DATABASE_TABLE_NAME);
    StringBuilder query, tempQuery;

    if (providerID != null && checkProviderId(iid, providerID)) {
      return new JsonObject().put(ERROR, INVALID_PROVIDER_ID);
    }
    /* check if the time is valid based on ISO 8601 format. */
    ZonedDateTime zdt;
    try {
      zdt = ZonedDateTime.parse(startTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
      zdt = ZonedDateTime.parse(endTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid Date exception: " + e.getMessage());
      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
    }

    ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
    ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

    LOGGER.trace(
        "PERIOD between given time "
            + (zonedDateTimeDifference(startZDT, endZDT)));

    if (zonedDateTimeDifference(startZDT, endZDT) > 14
        || zonedDateTimeDifference(startZDT, endZDT) <= 0) {
      LOGGER.error(INVALID_DATE_DIFFERENCE);
      return new JsonObject().put(ERROR, INVALID_DATE_DIFFERENCE);
    }

    long fromTime = getEpochTime(startZDT);
    LOGGER.debug("Epoch fromTime: " + fromTime);

    long toTime = getEpochTime(endZDT);

    if (providerID != null) {
      query =
          new StringBuilder(
              PROVIDER_ID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", Long.toString(fromTime))
                  .replace("$2", Long.toString(toTime))
                  .replace("$3", providerID));
    } else {
      query =
          new StringBuilder(
              CONSUMER_ID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", Long.toString(fromTime))
                  .replace("$2", Long.toString(toTime))
                  .replace("$3", userId));
    }
    if (consumerID != null) {
      tempQuery = query;
      tempQuery.append(USER_ID_QUERY.replace("$6", consumerID));
    }
    if (api != null && resourceId != null) {
      tempQuery = query;
      for (String s :
          Arrays.asList(API_QUERY.replace("$5", api), RESOURCE_QUERY.replace("$4", resourceId))) {
        tempQuery.append(s);
      }
    } else if (api != null) {
      tempQuery = query;
      tempQuery.append(API_QUERY.replace("$5", api));
    } else if (resourceId != null) {
      tempQuery = query;
      tempQuery.append(RESOURCE_QUERY.replace("$4", resourceId));
    } else {
      tempQuery = query;
    }
    LOGGER.trace("Info: QUERY " + tempQuery);
    tempQuery.append(ORDER_BY_AND_LIMIT);
    return new JsonObject().put(QUERY_KEY, tempQuery);
  }

  public JsonObject buildCountQuery(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String resourceId = request.getString(RESOURCE_ID);
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String providerID = request.getString(PROVIDER_ID);
    String consumerID = request.getString(CONSUMER_ID);
    String iid = request.getString(IID);
    String databaseTableName = request.getString(DATABASE_TABLE_NAME);

    StringBuilder query, tempQuery;

    if (providerID != null && checkProviderId(iid, providerID)) {
      return new JsonObject().put(ERROR, INVALID_PROVIDER_ID);
    }
    /* check if the time is valid based on ISO 8601 format. */
    ZonedDateTime zdt;
    try {
      zdt = ZonedDateTime.parse(startTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
      zdt = ZonedDateTime.parse(endTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid Date exception: " + e.getMessage());
      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
    }

    ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
    ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

    LOGGER.trace(
        "PERIOD between given time "
            + (zonedDateTimeDifference(startZDT, endZDT)));

    if (zonedDateTimeDifference(startZDT, endZDT) > 14
        || zonedDateTimeDifference(startZDT, endZDT) <= 0) {
      LOGGER.error(INVALID_DATE_DIFFERENCE);
      return new JsonObject().put(ERROR, INVALID_DATE_DIFFERENCE);
    }

    long fromTime = getEpochTime(startZDT);
    LOGGER.debug("Epoch fromTime: " + fromTime);

    long toTime = getEpochTime(endZDT);

    if (providerID != null) {
      query =
          new StringBuilder(
              PROVIDER_ID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", Long.toString(fromTime))
                  .replace("$2", Long.toString(toTime))
                  .replace("$3", providerID));
    } else {
      query =
          new StringBuilder(
              CONSUMER_ID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", Long.toString(fromTime))
                  .replace("$2", Long.toString(toTime))
                  .replace("$3", userId));
    }
    if (consumerID != null) {
      tempQuery = query;
      tempQuery.append(USER_ID_QUERY.replace("$6", consumerID));
    }
    if (api != null && resourceId != null) {
      tempQuery = query;
      for (String s :
          Arrays.asList(API_QUERY.replace("$5", api), RESOURCE_QUERY.replace("$4", resourceId))) {
        tempQuery.append(s);
      }
    } else if (api != null) {
      tempQuery = query;
      tempQuery.append(API_QUERY.replace("$5", api));
    } else if (resourceId != null) {
      tempQuery = query;
      tempQuery.append(RESOURCE_QUERY.replace("$4", resourceId));
    } else {
      tempQuery = query;
    }
    tempQuery.insert(tempQuery.length(), ";");
    LOGGER.trace("Info: QUERY " + tempQuery);
    return new JsonObject().put(QUERY_KEY, tempQuery);
  }

  public JsonObject buildWritingQuery(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    String userId = request.getString(USER_ID);
    String resourceId = request.getString(ID);
    String providerID =
        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
    String api = request.getString(API);
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = getEpochTime(zst);
    String isoTime =
        LocalDateTime.now()
            .atZone(ZoneId.of("Asia/Kolkata"))
            .truncatedTo(ChronoUnit.SECONDS)
            .toString();
    long response_size = request.getLong(Constants.RESPONSE_SIZE);
    String databaseTableName = request.getString(DATABASE_TABLE_NAME);

    StringBuilder query =
        new StringBuilder(
            WRITE_QUERY
                .replace("$0", databaseTableName)
                .replace("$1", primaryKey)
                .replace("$2", api)
                .replace("$3", userId)
                .replace("$4", Long.toString(time))
                .replace("$5", resourceId)
                .replace("$6", isoTime)
                .replace("$7", providerID)
                .replace("$8", Long.toString(response_size)));

    LOGGER.trace("Info: Query " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }

  public String buildTempReadQuery(JsonObject request) {
    StringBuilder tempQuery = new StringBuilder(request.getString(QUERY_KEY));
    String lastId = request.getString(LAST_ID);
    String latestId = request.getString(LATEST_ID);
    if (tempQuery.toString().contains(lastId)) {
      return tempQuery.toString().replace(lastId, latestId);
    } else {
      tempQuery.insert(tempQuery.indexOf(WHERE) + 5, ID_QUERY.replace("$7", lastId));
    }
    return tempQuery.toString();
  }

  private long zonedDateTimeDifference(ZonedDateTime d1, ZonedDateTime d2) {
    return ChronoUnit.DAYS.between(d1, d2);
  }

  private boolean checkProviderId(String iid, String providerID) {
    return !iid.substring(0, iid.indexOf('/', iid.indexOf('/') + 1)).equals(providerID);
  }

  private long getEpochTime(ZonedDateTime time) {
    return time.toInstant().toEpochMilli();
  }
}