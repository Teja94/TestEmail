package com.nearsoft.pikmail.api

import com.mixpanel.mixpanelapi.ClientDelivery
import com.mixpanel.mixpanelapi.MessageBuilder
import com.mixpanel.mixpanelapi.MixpanelAPI
import com.nearsoft.ipapiklient.models.IpCheckResult
import org.json.JSONObject


class AnalyticsManager {

    private val messageBuilder: MessageBuilder = MessageBuilder(System.getenv("MIXPANEL_TOKEN"))
    private val mixpanel = MixpanelAPI()

    fun trackSuccess(email: String, ipCheckResult: IpCheckResult, size: Int?) = track(email, EventType.SUCCESS, ipCheckResult, "Size" to size)

    fun trackError(email: String, ipCheckResult: IpCheckResult, throwable: Throwable) = track(email, EventType.ERROR, ipCheckResult, "Message" to throwable.message)

    private fun track(distinctId: String, eventType: EventType, ipCheckResult: IpCheckResult, vararg properties: Pair<String, Any?>) {
        val delivery = ClientDelivery().apply {
            val eventData = JSONObject(if (ipCheckResult.isSuccess()) ipCheckResult.ipInfo else ipCheckResult.ipError).apply {
                properties.forEach { (key, value) -> put(key, value) }
            }
            addMessage(messageBuilder.set(distinctId, JSONObject(mapOf("\$email" to distinctId))))
            addMessage(messageBuilder.event(distinctId, eventType.name, eventData))
            addMessage(messageBuilder.increment(distinctId, mapOf((if (eventType == EventType.SUCCESS) "Counter" else "ErrorCounter") to 1L)))
        }
        mixpanel.deliver(delivery)
    }

    private enum class EventType { SUCCESS, ERROR }

}