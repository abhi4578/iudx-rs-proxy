package iudx.rs.proxy.authenticator.authorization;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.rs.proxy.apiserver.util.RequestType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ApiTest {

    @ParameterizedTest
    @EnumSource
    public void test(Api api, VertxTestContext vertxTestContext)
    {
        assertNotNull(api);
        vertxTestContext.completeNow();
    }


}