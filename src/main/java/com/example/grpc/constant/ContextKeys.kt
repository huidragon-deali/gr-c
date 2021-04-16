package com.example.grpc.constant

import io.grpc.Context
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class ContextKeys {
    // 인스턴스로 생성되어있어야 Context 키에 대한 밸류를 얻어올수 있다,
    var APP_TRANSACTION = Context.key<String>("app-transaction")!!
    var METHOD = Context.key<String>("grpc-request-method")!!
    var REQUEST_START_TIME = Context.key<Long>("request-time")!!
    fun transactionIdToMDC() {
        val appId = APP_TRANSACTION.get()
        MDC.put("app-transaction", appId)
    }
}