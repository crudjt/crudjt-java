import token.TokenServiceGrpc;

import io.grpc.stub.StreamObserver;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessagePacker;

import token.TokenServiceOuterClass.CreateTokenRequest;
import token.TokenServiceOuterClass.CreateTokenResponse;

import token.TokenServiceOuterClass.ReadTokenRequest;
import token.TokenServiceOuterClass.ReadTokenResponse;

import token.TokenServiceOuterClass.UpdateTokenRequest;
import token.TokenServiceOuterClass.UpdateTokenResponse;

import token.TokenServiceOuterClass.DeleteTokenRequest;
import token.TokenServiceOuterClass.DeleteTokenResponse;

import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import org.msgpack.value.*;
import java.util.*;

import crudjt.MsgPackUtils;
import com.google.protobuf.ByteString;
import java.util.Collections;


public class TokenServiceImpl
    extends TokenServiceGrpc.TokenServiceImplBase {

    @Override
    public void createToken(
            CreateTokenRequest request,
            StreamObserver<CreateTokenResponse> responseObserver
    ) {
        try {
            byte[] packedData = request.getPackedData().toByteArray();

            Map<String, Object> data = MsgPackUtils.unpack(packedData);

            Long ttl = request.getTtl();
            Long silence_read = request.getSilenceRead();

            String value = CRUDJT.original_create(data, ttl, silence_read);

            CreateTokenResponse response = CreateTokenResponse
                    .newBuilder()
                    .setToken(value)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void readToken(ReadTokenRequest request, StreamObserver<ReadTokenResponse> responseObserver) {
        try {
            String rawToken = request.getToken();

            Map<String, Object> resultHash = CRUDJT.original_read(rawToken);

            Map<String, Object> safeMap =
                    (resultHash == null) ? Collections.emptyMap() : resultHash;

            byte[] packedData = MsgPackUtils.pack(safeMap);

            ReadTokenResponse response = ReadTokenResponse.newBuilder()
                    .setPackedData(ByteString.copyFrom(packedData))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
            return;
        }
    }

    @Override
    public void updateToken(UpdateTokenRequest request, StreamObserver<UpdateTokenResponse> responseObserver) {
        try {
            String rawToken = request.getToken();
            byte[] packedData = request.getPackedData().toByteArray();

            Map<String, Object> unpackedData = MsgPackUtils.unpack(packedData);

            Long ttl = request.getTtl();
            Long silence_read = request.getSilenceRead();

            boolean result = CRUDJT.original_update(rawToken, unpackedData, ttl, silence_read);

            UpdateTokenResponse response = UpdateTokenResponse.newBuilder()
                    .setResult(result)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteToken(DeleteTokenRequest request, StreamObserver<DeleteTokenResponse> responseObserver) {
        try {
          String rawToken = request.getToken();

          boolean result = CRUDJT.original_delete(rawToken);

          DeleteTokenResponse response = DeleteTokenResponse.newBuilder()
                  .setResult(result)
                  .build();

          responseObserver.onNext(response);
          responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
