package io.vertx.example.grpc.ssl;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.JksOptions;
import io.vertx.example.grpc.util.Runner;
import io.vertx.grpc.VertxChannelBuilder;

import java.sql.Time;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Client extends AbstractVerticle {

  public static void main(String[] args) {
    Runner.runExample(Client.class);
  }

  @Override
  public void start() throws Exception {
    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "localhost", 8080)
      .useSsl(options -> options.setSsl(true)
        .setUseAlpn(true)
        .setTrustStoreOptions(new JksOptions()
          .setPath("tls/client-truststore.jks")
          .setPassword("wibble")))
      .build();
    GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);

    AtomicReference<Integer> counter = new AtomicReference<>(0);
    vertx.setPeriodic(1000, id -> {
      // This handler will get called every second
      Timestamp startTime = getCurrentTimestamp();

      HelloRequest request = HelloRequest.newBuilder()
        .setName("Borg")
        .setId(counter.getAndSet(counter.get() + 1))
        .setStartTime(startTime)
        .build();
      stub.sayHello(request, asyncResponse -> {
        if (asyncResponse.succeeded()) {
          Timestamp endTime =getCurrentTimestamp();
          HelloReply reply = asyncResponse.result();
          Duration there = getDurationDifference(asyncResponse.result().getStartTime(), reply.getServerReceivedTime());
          Duration roundTrip = getDurationDifference(asyncResponse.result().getStartTime(), endTime);
          System.out.println(reply.getId() + " Succeeded : Name " + reply.getMessage() +
            ": to there " + there.getSeconds() + "s," + there.getNanos()/1000000 + "m : round trip :" + roundTrip.getSeconds() + "s," + roundTrip.getNanos()/1000000 + "m." );
        } else {
          asyncResponse.cause().printStackTrace();
        }
      });
    });
  }

  private static Timestamp getCurrentTimestamp() {
    long millis = System.currentTimeMillis();
    return Timestamp.newBuilder().setSeconds(millis / 1000)
      .setNanos((int) ((millis % 1000) * 1000000)).build();
  }

  private static Duration getDurationDifference(Timestamp before, Timestamp after) {
    Duration.Builder durationBuilder = Duration.newBuilder();
    long dseconds = after.getSeconds() - before.getSeconds();
    int dnanos = after.getNanos() - before.getNanos();

    if (dseconds < 0 && dnanos > 0) {
      dseconds += 1;
      dnanos -= 1000000000;
    } else if (dseconds > 0 && dnanos < 0) {
      dseconds -= 1;
      dnanos += 1000000000;
    }
    durationBuilder.setSeconds(dseconds);
    durationBuilder.setNanos(dnanos);
    return  durationBuilder.build();
  }
}
