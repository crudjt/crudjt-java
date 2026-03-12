// This binding was generated automatically to ensure consistency across languages
// Generated using ChatGPT (GPT-5) from the canonical Ruby SDK
// API is stable and production-ready

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;

public final class GrpcServer {

    private Server server;

    public void start(String address, int port) throws IOException {
        server = NettyServerBuilder
                .forAddress(new InetSocketAddress(address, port))
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
