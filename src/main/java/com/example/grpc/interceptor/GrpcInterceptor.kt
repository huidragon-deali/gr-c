package com.example.grpc.interceptor

import lombok.RequiredArgsConstructor
import com.example.grpc.constant.ContextKeys
import org.springframework.util.ObjectUtils
import java.util.UUID
import com.example.grpc.interceptor.GrpcInterceptor.ServerCallListener
import io.grpc.*
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@RequiredArgsConstructor
open class GrpcInterceptor : ServerInterceptor {

    @Autowired
    private val keys: ContextKeys? = null;

    // 클라이언트에서 전송한 Metadata에서 "app-transaction"을 추출
    private fun getTransactionId(metadata: Metadata): String? {
        val key = Metadata.Key.of("app-transaction", Metadata.ASCII_STRING_MARSHALLER)
        val transactionId = metadata.get(key)
        return if (!ObjectUtils.isEmpty(transactionId)) transactionId else UUID.randomUUID().toString()
    }

    override fun <ReqT, RespT> interceptCall(
        serverCall: ServerCall<ReqT, RespT>, metadata: Metadata,
        serverCallHandler: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        // grpc 하나의 요청 라이프 사이클에서 사용할 Context 데이터를 설정
        var ctx = Context.current()
        ctx = ctx.withValue(keys!!.APP_TRANSACTION, getTransactionId(metadata))
        ctx = ctx.withValue(keys.METHOD, serverCall.methodDescriptor.fullMethodName)
        ctx = ctx.withValue(keys.REQUEST_START_TIME, System.currentTimeMillis())

        // Contexts.interceptCall api를 사용하면 Context 객체의 일관성을 유지할수 있다.
        val listener = Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler)

        // MDC사용을 위한 wapper
        return ServerCallListener(listener, ctx)
    }

    // grpc는 어떤 쓰레드가 로직을 수행할지 알수가 없으므로, 내부적으로 MDC를 사용하는 경우 Context에서 MDC로 데이터를 복사해 주어야 하는 로직이 필요
    internal inner class ServerCallListener<ReqT>(listener: ServerCall.Listener<ReqT>?, private val context: Context) :
        SimpleForwardingServerCallListener<ReqT>(listener) {
        // 실제 구현체를 호출하기 전 이벤트
        override fun onHalfClose() {
            val previous = context.attach()
            keys!!.transactionIdToMDC()
            try {
                super.onHalfClose()
            } finally {
                context.detach(previous)
            }
        }
    }
}