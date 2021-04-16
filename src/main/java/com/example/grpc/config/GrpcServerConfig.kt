package com.example.grpc.config

import lombok.RequiredArgsConstructor
import com.example.grpc.interceptor.GrpcInterceptor
import com.example.grpc.service.GrpcService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@RequiredArgsConstructor
open class GrpcServerConfig {

    @Value("\${grpc.port:8888}")
    lateinit var grpcPort: Number

    @Autowired
    lateinit var handler: GrpcInterceptor

    @Autowired
    lateinit var rpcService: GrpcService

    // Grpc Java는 Netty 기반 http2.0 프로토콜을 사용
    @Bean
    open fun grpcServer(): Server {
        return ServerBuilder
            .forPort(grpcPort.toInt())
            .addService(rpcService)
            .intercept(handler)
            .build()
    }
}