package iudx.rs.proxy.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.rs.proxy.apiserver.util.RequestType;
import iudx.rs.proxy.apiserver.validation.ValidatorsHandlersFactory;
import iudx.rs.proxy.apiserver.validation.types.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationHandler implements Handler<RoutingContext> {


  private static final Logger LOGGER = LogManager.getLogger(ValidationHandler.class);

  private RequestType requestType;
  private Vertx vertx;


  public ValidationHandler(Vertx vertx, RequestType apiRequestType) {
    this.vertx = vertx;
    this.requestType = apiRequestType;
  }

  @Override
  public void handle(RoutingContext context) {
    ValidatorsHandlersFactory validationFactory = new ValidatorsHandlersFactory();
    MultiMap parameters = context.request().params();
    Map<String, String> pathParams = context.pathParams();
    parameters.addAll(pathParams);
    
    List<Validator> validations = validationFactory.build(requestType, parameters);
    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.debug("validator :" + validator.getClass().getName());
      validator.isValid();
      /*if (!validator.isValid()) {
        error(context);
        return;
      }*/
    }
    context.next();
  }

 /* private void error(RoutingContext context) {
    context.response().putHeader("content-type", "application/json")
        .setStatusCode(HttpStatus.SC_BAD_REQUEST)
        .end(getBadRequestMessage().toString());
  }
  private JsonObject getBadRequestMessage() {
    return new JsonObject()
        .put("type", 400)
        .put("title", "Bad Request")
        .put("details", "Bad query");
  }*/
}
