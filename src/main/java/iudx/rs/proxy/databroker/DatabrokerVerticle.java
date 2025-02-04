package iudx.rs.proxy.databroker;

import static iudx.rs.proxy.common.Constants.DATABROKER_SERVICE_ADDRESS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;

public class DatabrokerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(DatabrokerVerticle.class);

  private RabbitMQOptions config;
  private WebClientOptions webConfig;

  private String dataBrokerIP;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;

  private String publishExchange;
  private String replyQueue;


  private RabbitMQClient rmqClient;

  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private DatabrokerService brokerService;

  @Override
  public void start() throws Exception {


    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort =
        config().getInteger("dataBrokerManagementPort");
    dataBrokerVhost = config().getString("internalVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval =
        config().getInteger("networkRecoveryInterval");

    publishExchange = config().getString("adapterQueryPublishExchange");
    replyQueue = config().getString("adapterQueryReplyQueue");
    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setVirtualHost(dataBrokerVhost);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    rmqClient = RabbitMQClient.create(vertx, config);

    rmqClient
        .start()
        .onSuccess(rmqClientStarthandler -> {
          brokerService = new DatabrokerServiceImpl(vertx, rmqClient, publishExchange, replyQueue);
          RPCAdapterConsumer adapterConsumer = new RPCAdapterConsumer(rmqClient);

          binder = new ServiceBinder(vertx);
          consumer = binder.setAddress(DATABROKER_SERVICE_ADDRESS).register(DatabrokerService.class,
              brokerService);

          // adapterConsumer.start();

          LOGGER.info("Databroker Verticle deployed.");
        }).onFailure(rmqClientStartHandler -> {
          LOGGER.error("RMQ client startup failure failed, {}", rmqClientStartHandler);
        });
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
