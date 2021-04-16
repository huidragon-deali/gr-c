package com.example.grpc.service

import com.dealicious.grpc.GrpcServiceGrpc.GrpcServiceImplBase
import com.dealicious.grpc.GrpcModel
import com.google.common.collect.Lists
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Exception

@Service
open class GrpcService : GrpcServiceImplBase() {
    // GrpcServiceImplBase = 프로토콜버퍼파일에 명시한 service의 추상클래스
    var log = LoggerFactory.getLogger(this.javaClass)!!

    // 1요청, 1응답
    override fun getOne(request: GrpcModel.Request, responseObserver: StreamObserver<GrpcModel.Response>) {
        try {
            log.info("getOne: request: {}", request)
            // 객체의 경우 has메서드 제공, validation 직접처리 필요
            if (request.hasCustomObject()) {
                // exception
            }

            // grpc가 구성해준 모델클래스
            val response = GrpcModel.Response
                .newBuilder()
                .addAllValue(Lists.newArrayList(1.0, 2.0, 3.0, 4.0, 5.0))
                .build()

            // 하나의 요청에 대한 하나의 응답을 처리한다.
            responseObserver.onNext(response)

            // 응답에 대한 완료후 호출해야 한다
            responseObserver.onCompleted()
        } catch (e: Exception) {

            // 로직수행중 에러가 발생할 경우 클라이언트에 StatusRuntimeException 반환
            // Status는 httpStatus code와 같은 grpc의 프로토콜
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription(e.message)
                    .withCause(e) // 해당 cause는 클라이언트로는 전송되지 않는다
                    .asRuntimeException()
            )
        }
    }

    // 클라이언트 스트리밍 - 클라이언트에서 완료이벤트가 전송되지 않으면, 서버는 계속 응답을 기다리게 된다
    // n요청: 1응답
    override fun clientStream(responseObserver: StreamObserver<GrpcModel.Response>): StreamObserver<GrpcModel.Request?> {
        return object : StreamObserver<GrpcModel.Request?> {
            override fun onNext(value: GrpcModel.Request?) {
                //클라이언트로부터 여러번의 onNext가 호출된다
                log.info("clientStream onNext request: {}", value)
            }

            override fun onError(t: Throwable) {
                //스트리밍중 에러 처리
                log.info("clientStream onError: {}", t.message)
            }

            override fun onCompleted() {
                log.info("clientStream onCompleted")

                // 클라이언트로부터 스트림 완료 응답이 오면 1번의 응답을 보낼수 있다
                val response = GrpcModel.Response
                    .newBuilder()
                    .addAllValue(Lists.newArrayList(1.0, 2.0, 3.0, 4.0, 5.0))
                    .build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }
        }
    }

    // 서버 스트리밍 - 클라이언트의 요청이 시작되면 서버는 완료이벤트를 전송하기 전까지 클라이언트는 계속 응답을 대기 한다
    // 1요청: n응답
    override fun serverStream(request: GrpcModel.Request, responseObserver: StreamObserver<GrpcModel.Response>) {
        log.info("serverStream: request: {}", request)
        val response = GrpcModel.Response
            .newBuilder()
            .addAllValue(Lists.newArrayList(1.0, 2.0, 3.0, 4.0, 5.0))
            .build()

        // 클라이언트에의 요청은 1번이지만 서버는 여러번 응답을 스트리밍 할수 있다
        responseObserver.onNext(response)
        responseObserver.onNext(response)
        responseObserver.onNext(response)
        responseObserver.onNext(response)
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    // 양방향 스트리밍 - 클라이언트에서 완료이벤트가 전송하기 전까지, 서로간의 스트리밍을 진행한다.
    // n요청: n응답
    override fun biStream(responseObserver: StreamObserver<GrpcModel.Response>): StreamObserver<GrpcModel.Request?> {

        return object : StreamObserver<GrpcModel.Request?> {
            override fun onNext(value: GrpcModel.Request?) {
                log.info("biStream onNext request: {}", value)

                // 클라이언트로부터 데이터가 올 때마다 onNext가 호출된다
                // 1개의 요청이 올 때마다 n번의 응답을 스트리밍 전송한다
                val response = GrpcModel.Response
                    .newBuilder()
                    .addAllValue(Lists.newArrayList(1.0, 2.0, 3.0, 4.0, 5.0))
                    .build()
                responseObserver.onNext(response)
                responseObserver.onNext(response)
                responseObserver.onNext(response)
            }

            override fun onError(t: Throwable) {
                log.info("biStream onError request: {}", t.message)
            }

            override fun onCompleted() {
                log.info("biStream onComplete")
                // 클라이언트의 완료요청이 오면 서버도 완료응답을 진행한다
                responseObserver.onCompleted()
            }
        }
    }
}