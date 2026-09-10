package com.personal.financetracker.ui.common

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.view.children
import com.personal.financetracker.R

/** Small motion helpers. All of them respect the system animator-duration scale (they use ValueAnimator / ViewPropertyAnimator). */
object Anim {
    const val FAST = 180L
    const val NORMAL = 320L
    const val SLOW = 600L
    val decel = DecelerateInterpolator(2f)
    val overshoot = OvershootInterpolator(1.4f)
}

/**
 * Animates a numeric label from its last shown value to [target]. The previous value is kept on the
 * view's tag, so repeated updates (new period, new data) roll smoothly instead of jumping.
 */
fun TextView.countTo(target: Double, duration: Long = Anim.SLOW, format: (Double) -> String) {
    val from = (getTag(R.id.tag_count_value) as? Double) ?: 0.0
    (getTag(R.id.tag_count_animator) as? ValueAnimator)?.cancel()
    if (from == target || !isAttachedToWindow) {
        text = format(target); setTag(R.id.tag_count_value, target); return
    }
    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        this.duration = duration
        interpolator = Anim.decel
        addUpdateListener {
            val t = it.animatedValue as Float
            val v = from + (target - from) * t
            text = format(v)
        }
        doOnEnd { text = format(target); setTag(R.id.tag_count_value, target) }
    }
    setTag(R.id.tag_count_animator, anim)
    anim.start()
}

private fun ValueAnimator.doOnEnd(block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = block()
    })
}

/** Fade + rise into place. Safe to call repeatedly; it resets first. */
fun View.fadeInUp(delay: Long = 0, duration: Long = Anim.NORMAL, distanceDp: Float = 14f) {
    animate().cancel()
    alpha = 0f
    translationY = dp(distanceDp)
    animate().alpha(1f).translationY(0f)
        .setStartDelay(delay).setDuration(duration).setInterpolator(Anim.decel).start()
}

/** Staggers [fadeInUp] across the visible children of a container. */
fun ViewGroup.staggerChildren(step: Long = 45, initialDelay: Long = 0, maxItems: Int = 12) {
    var i = 0
    children.forEach { child ->
        if (child.visibility != View.VISIBLE) return@forEach
        if (i < maxItems) child.fadeInUp(delay = initialDelay + i * step) else { child.alpha = 1f; child.translationY = 0f }
        i++
    }
}

/** Quick press-and-release pop, e.g. for a checkbox tick. */
fun View.pop() {
    animate().cancel()
    scaleX = 0.7f; scaleY = 0.7f
    animate().scaleX(1f).scaleY(1f).setDuration(Anim.NORMAL).setInterpolator(Anim.overshoot).start()
}
