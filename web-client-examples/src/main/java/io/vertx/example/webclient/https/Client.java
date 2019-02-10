package io.vertx.example.webclient.https;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.example.util.Runner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.concurrent.atomic.AtomicReference;

/*
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Client extends AbstractVerticle {

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(Client.class);
  }

  @Override
  public void start() throws Exception {

    // Create the web client and enable SSL/TLS with a trust store
    WebClient client = WebClient.create(vertx,
      new WebClientOptions()
        .setSsl(true)
        .setTrustStoreOptions(new JksOptions()
          .setPath("client-truststore.jks")
          .setPassword("wibble")
        )
    );

    AtomicReference<Integer> counter = new AtomicReference<>(0);
    vertx.setPeriodic(1000, id -> {
      long millisStartTime = System.currentTimeMillis();

      TimeData timeData = new TimeData();
      timeData.id = counter.getAndSet(counter.get() + 1);
      timeData.milliStart = millisStartTime;

      client.put(8443, "localhost", "/")
        .as(BodyCodec.json(TimeData.class))
        .sendJson(timeData, ar -> {
          if (ar.succeeded()) {
            long finalTime = System.currentTimeMillis();
            HttpResponse<TimeData> response = ar.result();
            TimeData timedataResponse = response.body();
            long oneWay = timedataResponse.milliServerRecieved - timedataResponse.milliStart;
            long roundTrip = finalTime - timedataResponse.milliStart;
            System.out.println(timedataResponse.id + " Succeeded" +
              ": to there " + oneWay + "m : round trip :" + roundTrip + "m." );
          } else {
            ar.cause().printStackTrace();
          }
        });
    });
  }
}
