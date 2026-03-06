package com.hv.cabinet.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 高频RFID消息批处理器：按时间窗/条数聚合，减少UI刷新与解析调用频率。
 */
class InventoryEventBatcher(
    private val scope: CoroutineScope,
    private val flushWindowMs: Long = 300L,
    private val maxBatchSize: Int = 32,
    private val onFlush: (List<String>) -> Unit
) {
    private val buffer = LinkedHashSet<String>()
    private var flushJob: Job? = null

    @Synchronized
    fun offer(code: String) {
        val normalized = code.trim()
        if (normalized.isBlank()) return
        buffer.add(normalized)
        if (buffer.size >= maxBatchSize) {
            flushLocked()
            return
        }
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(flushWindowMs)
            flush()
        }
    }

    @Synchronized
    fun flush() {
        flushLocked()
    }

    @Synchronized
    fun clear() {
        flushJob?.cancel()
        flushJob = null
        buffer.clear()
    }

    private fun flushLocked() {
        flushJob?.cancel()
        flushJob = null
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        onFlush(batch)
    }
}
