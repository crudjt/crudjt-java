import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

import crudjt.MsgPackUtils;

public final class GrpcServer {

    private Server server;

    public void start(String address, int port) throws IOException {
        System.out.println("gRPC starting on 127.0.0.1:50051");

        server = ServerBuilder
                .forPort(port)
                .addService(new TokenServiceImpl())
                .build()
                .start();
    }

    public void block() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
