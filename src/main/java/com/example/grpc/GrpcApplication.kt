package com.example.grpc

import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.jvm.JvmStatic
import org.springframework.boot.SpringApplication

@SpringBootApplication
open class GrpcApplication

fun main(args: Array<String>) {
    SpringApplication.run(GrpcApplication::class.java, *args)
}