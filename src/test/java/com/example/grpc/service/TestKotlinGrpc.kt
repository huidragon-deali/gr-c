package com.example.grpc.service

import com.dealicious.grpc.GrpcModel
import com.dealicious.grpc.GrpcServiceGrpc.*
import com.example.grpc.interceptor.GrpcInterceptor
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcCleanupRule
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.util.concurrent.Executors

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@SpringBootTest
class TestKotlinGrpc {

    var log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Rule @JvmField
    public var grpcCleanUp = GrpcCleanupRule()

    private val serverName = InProcessServerBuilder.generateName()
    private val serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor()
    private val channelBuilder = InProcessChannelBuilder.forName(serverName).directExecutor()

    @Autowired
    var service: GrpcService? = null

    @Autowired
    var interceptor: GrpcInterceptor? = null

    // 구글에서는 하나의 서버에 하나의 채널 사용을 권고
    var channel: ManagedChannel? = null

    @Before
    @Throws(IOException::class)
    fun before() {
        grpcCleanUp.register(serverBuilder.addService(service).intercept(interceptor).build().start())
        channel = grpcCleanUp.register(channelBuilder.build())
    }

    /***
     * blockingStub : 1:1, 1:n 2가지의 통신형태를 지원
     */
    @org.junit.Test
    fun blocking() {
        val blockingClient = newBlockingStub(channel)
        val request = GrpcModel.Request.newBuilder() //.setLongValue(1L)
                //.setStringValue("stringValue")
                .build()

        // 1:1
        val response = blockingClient.getOne(request)
        log.info(response.valueList.toString())

        // 1:n
        val itResponse = blockingClient.serverStream(request)
        itResponse.forEachRemaining { res: GrpcModel.Response -> log.info(res.valueList.toString()) }
    }

    /***
     * futureStub : 현재 1:1 방식만 지원, 이름그대로 future로 사용할때
     */
    @org.junit.Test
    fun future() {
        val futureClient = newFutureStub(channel)
        val request = GrpcModel.Request.newBuilder().setLongValue(1L).setStringValue("stringValue").build()
        val executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))
        val future = futureClient.getOne(request)
        Futures.addCallback(future,  object: FutureCallback<GrpcModel.Response> {

            override fun onFailure(t: Throwable) {}
            override fun onSuccess(result: GrpcModel.Response?) {
                log.info(result?.valueList.toString())
            }
        }, executor)
    }


    /***
     * asyncStub : 1:1, n:1, 1:n, n:n 4가지 방식지원
     */
    @org.junit.Test
    @Throws(InterruptedException::class)
    fun async() {
        val asyncClient = newStub(channel)
        val request = GrpcModel.Request.newBuilder().setLongValue(1L).setStringValue("stringValue").build()

        // stream 구독처리
        val responseObserver: StreamObserver<GrpcModel.Response> = object : StreamObserver<GrpcModel.Response> {
            override fun onNext(res: GrpcModel.Response) {
                log.info(res.valueList.toString())
            }
            override fun onError(t: Throwable) {
                log.error(t.message)
            }
            override fun onCompleted() {
                log.info("completed")
            }
        }

        // 1:1
        asyncClient.getOne(request, responseObserver)

        // n:1
        var reqeustObserver = asyncClient.clientStream(responseObserver)
        reqeustObserver.onNext(request)
        reqeustObserver.onNext(request)
        reqeustObserver.onNext(request)
        reqeustObserver.onNext(request)
        reqeustObserver.onCompleted()

        // 1:n
        asyncClient.serverStream(request, responseObserver)

        // n:n
        reqeustObserver = asyncClient.biStream(responseObserver)
        reqeustObserver.onNext(request)
        reqeustObserver.onCompleted()

        // async 확인용
        Thread.sleep(1000)
    }

    @org.junit.Test
    @Throws(IOException::class)
    fun metaData() {

        // http header 처럼 사용할수 있는 클래스
        val metadata = Metadata()

        // MetadataUtils.attachHeaders api를 사용하여 stub객체와 연결
        val client = MetadataUtils.attachHeaders(newBlockingStub(channel), metadata)
        metadata.put(Metadata.Key.of("app-transaction", Metadata.ASCII_STRING_MARSHALLER), "transactionValue")
    }
}