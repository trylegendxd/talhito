package com.rushx.reelscheduler

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class InstagramAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var activeId: Long? = null
    private var step = Step.FIND_SHARE
    private var startedAt = 0L

    private enum class Step { FIND_SHARE, FIND_RECIPIENT_OR_SEARCH, FIND_RECIPIENT, FIND_SEND }

    private val shareWords = listOf("share", "partilhar", "compartilhar", "send", "enviar")
    private val searchWords = listOf("search", "pesquisar", "procurar")
    private val sendWords = listOf("send", "enviar")

    private val scanner = object : Runnable {
        override fun run() {
            scan()
            if (activeId != null) handler.postDelayed(this, 650)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.instagram.android") return
        val repo = JobRepository(this)
        val job = repo.activeJob() ?: return
        beginAutomation(job)
    }

    override fun onInterrupt() = Unit

    private fun launchInstagram(job: ScheduledReel): Boolean {
        beginAutomation(job)
        val reelIntent = Intent(Intent.ACTION_VIEW, Uri.parse(job.url)).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return try {
            startActivity(reelIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun beginAutomation(job: ScheduledReel) {
        if (activeId == job.id) return
        activeId = job.id
        step = Step.FIND_SHARE
        startedAt = System.currentTimeMillis()
        handler.removeCallbacks(scanner)
        handler.postDelayed(scanner, 1000)
    }

    private fun scan() {
        val repo = JobRepository(this)
        val job = repo.activeJob() ?: run { stopAutomation(); return }
        if (System.currentTimeMillis() - startedAt > 75_000L) {
            repo.upsert(job.copy(status = ScheduledReel.STATUS_FAILED))
            repo.setActiveJobId(null)
            Notifier.show(this, "Reel not sent", "Instagram’s interface didn’t match the automation.")
            stopAutomation()
            return
        }

        val root = rootInActiveWindow ?: return
        when (step) {
            Step.FIND_SHARE -> {
                val share = findByWords(root, shareWords)
                if (share != null && clickNodeOrParent(share)) {
                    step = Step.FIND_RECIPIENT_OR_SEARCH
                    handler.postDelayed({ scan() }, 900)
                }
            }
            Step.FIND_RECIPIENT_OR_SEARCH -> {
                val person = findRecipientResult(root, job.recipient)
                if (person != null && clickNodeOrParent(person)) {
                    step = Step.FIND_SEND
                    handler.postDelayed({ scan() }, 600)
                    return
                }
                val editable = findEditable(root) ?: findByWords(root, searchWords)
                if (editable != null && setText(editable, job.recipient)) {
                    step = Step.FIND_RECIPIENT
                    handler.postDelayed({ scan() }, 1200)
                }
            }
            Step.FIND_RECIPIENT -> {
                val person = findRecipientResult(root, job.recipient)
                if (person != null && clickNodeOrParent(person)) {
                    step = Step.FIND_SEND
                    handler.postDelayed({ scan() }, 600)
                }
            }
            Step.FIND_SEND -> {
                val send = findByWords(root, sendWords)
                if (send != null && clickNodeOrParent(send)) {
                    repo.upsert(job.copy(status = ScheduledReel.STATUS_SENT))
                    repo.setActiveJobId(null)
                    Notifier.show(this, "Reel sent", "Scheduled Reel sent to @${job.recipient}.")
                    stopAutomation()
                }
            }
        }
    }

    private fun stopAutomation() {
        activeId = null
        handler.removeCallbacks(scanner)
    }

    private fun normalized(s: CharSequence?): String =
        s?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""

    private fun labels(node: AccessibilityNodeInfo): List<String> = listOf(
        normalized(node.text),
        normalized(node.contentDescription),
        normalized(node.hintText)
    ).filter { it.isNotBlank() }

    private fun findByWords(root: AccessibilityNodeInfo, words: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var fallback: AccessibilityNodeInfo? = null
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            val ls = labels(n)
            if (ls.any { label -> words.any { w -> label == w } }) return n
            if (fallback == null && ls.any { label -> words.any { w -> label.contains(w) } }) fallback = n
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::add)
        }
        return fallback
    }

    private fun findRecipientResult(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val target = text.trim().removePrefix("@").lowercase(Locale.ROOT)
        if (target.isBlank()) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var fallback: AccessibilityNodeInfo? = null
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            if (!n.isEditable && hasClickableSelfOrParent(n)) {
                for (label in labels(n)) {
                    val clean = label.removePrefix("@").trim()
                    if (clean == target) return n
                    if (fallback == null && clean.contains(target)) fallback = n
                }
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::add)
        }
        return fallback
    }

    private fun hasClickableSelfOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(6) {
            if (current?.isClickable == true) return true
            current = current?.parent
        }
        return false
    }

    private fun findEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            if (n.isEditable) return n
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::add)
        }
        return null
    }

    private fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        val target = if (node.isEditable) node else findEditable(node) ?: node
        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return target.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        )
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(6) {
            if (current?.isClickable == true && current?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
            current = current?.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    companion object {
        @Volatile private var instance: InstagramAccessibilityService? = null

        fun launchScheduledJob(job: ScheduledReel): Boolean {
            return instance?.launchInstagram(job) == true
        }
    }
}
