package com.example.grpc.runner

import io.grpc.Server
import lombok.RequiredArgsConstructor
import org.springframework.boot.ApplicationRunner
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import kotlin.Throws
import org.springframework.boot.ApplicationArguments
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.util.ObjectUtils
import java.lang.Exception

@Profile("!test")
@Component
@RequiredArgsConstructor
open class ServerStartRunner : ApplicationRunner, DisposableBean {

    @Autowired
    private val grpcServer: Server? = null

    // grpc는 별개의 netty서버를 구동시켜야 하므로 runner 사용
    @Throws(Exception::class)
    override fun run(args: ApplicationArguments) {
        grpcServer!!.start()
        grpcServer.awaitTermination()
    }

    @Throws(Exception::class)
    override fun destroy() {
        if (!ObjectUtils.isEmpty(grpcServer)) {
            grpcServer!!.shutdown()
        }
    }
}