package com.minar.birday.utilities

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.minar.birday.R

// AGSL progressive blur: the radius ramps from 0 to blurRadius across the last "band" pixels before
// the chosen edge, so the content dissolves under the navbar instead of being cut off by it.
// The dither jitters the sample grid, otherwise the low sample count shows up as banding.
private val PROGRESSIVE_BLUR_SKSL = """
    uniform shader content;
    uniform float blurRadius;
    uniform float band;
    uniform float extent;

    half4 main(float2 fragCoord) {
        float progress = 1.0 - clamp((extent - fragCoord.y) / band, 0.0, 1.0);
        progress = pow(progress, 1.5);
        float radius = progress * blurRadius;
        if (radius <= 0.0) {
            return content.eval(fragCoord);
        }
        half4 accum = half4(0.0);
        float weightSum = 0.0;
        float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
        float2 jitter = float2(dither - 0.5, fract(dither * 1.618) - 0.5);
        const int SAMPLES = 4;
        float offsetScale = radius / float(SAMPLES);
        for (int x = -SAMPLES; x <= SAMPLES; x++) {
            for (int y = -SAMPLES; y <= SAMPLES; y++) {
                float2 offset = (float2(float(x), float(y)) + jitter) * offsetScale;
                float distSq = dot(offset, offset);
                float radiusSq = radius * radius;
                if (distSq <= radiusSq) {
                    float weight = exp(-3.0 * distSq / radiusSq);
                    accum += content.eval(fragCoord + offset) * weight;
                    weightSum += weight;
                }
            }
        }
        return accum / weightSum;
    }
""".trimIndent()

// RuntimeShader is API 33, while Birday supports API 26: below Tiramisu the option simply isn't
// offered, instead of faking the effect with a gradient that would look like a different feature.
val isProgressiveBlurAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Blurs the bottom [bandPx] pixels of this view with a ramp, or clears any previously
 * applied effect when [enabled] is false. The shader needs the view size, so it is rebuilt on every
 * layout pass: the render effect is cheap to recreate, and the view size changes with insets,
 * rotation and the keyboard.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun View.applyBottomProgressiveBlur(
    enabled: Boolean,
    bandPx: Float,
    blurRadius: Float = 18f,
) {
    // Drop the listener left by a previous call, otherwise toggling the option stacks them up
    (getTag(R.id.tag_progressive_blur_listener) as? View.OnLayoutChangeListener)?.let {
        removeOnLayoutChangeListener(it)
        setTag(R.id.tag_progressive_blur_listener, null)
    }
    if (!enabled) {
        setRenderEffect(null)
        return
    }

    fun rebuild() {
        // A zero height view has nothing to blur yet, and the shader would divide by zero
        if (height <= 0) return
        val shader = RuntimeShader(PROGRESSIVE_BLUR_SKSL)
        shader.setFloatUniform("blurRadius", blurRadius)
        shader.setFloatUniform("band", bandPx)
        shader.setFloatUniform("extent", height.toFloat())
        setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
    }

    val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> rebuild() }
    addOnLayoutChangeListener(listener)
    setTag(R.id.tag_progressive_blur_listener, listener)
    rebuild()
}
