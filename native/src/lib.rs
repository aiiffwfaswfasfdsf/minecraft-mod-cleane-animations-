//! Animation math core for the Clean Animations Minecraft mod.
//!
//! Every formula here has an exact twin in `JavaAnimMath` on the Java side;
//! keep both in sync (a JUnit parity test enforces 1e-4 agreement).
#![forbid(unsafe_op_in_unsafe_fn)]

use std::f32::consts::PI;

/// ABI version of this library.
#[no_mangle]
pub extern "C" fn ca_version() -> u32 {
    1
}

const BACK_OVERSHOOT: f32 = 1.70158;

#[inline]
fn clamp01(t: f32) -> f32 {
    if t.is_nan() {
        0.0
    } else {
        t.clamp(0.0, 1.0)
    }
}

/// Safe easing implementation. Kinds: 0 linear, 1 easeOutCubic, 2 easeInOutCubic,
/// 3 easeOutBack, 4 easeOutElastic, 5 easeOutQuint, 6 easeInOutSine.
pub fn ease(kind: u32, t: f32) -> f32 {
    let t = clamp01(t);
    match kind {
        1 => 1.0 - (1.0 - t).powi(3),
        2 => {
            if t < 0.5 {
                4.0 * t * t * t
            } else {
                1.0 - (-2.0 * t + 2.0).powi(3) / 2.0
            }
        }
        3 => {
            let c1 = BACK_OVERSHOOT;
            let c3 = c1 + 1.0;
            1.0 + c3 * (t - 1.0).powi(3) + c1 * (t - 1.0).powi(2)
        }
        4 => {
            if t <= 0.0 {
                0.0
            } else if t >= 1.0 {
                1.0
            } else {
                let c4 = (2.0 * PI) / 3.0;
                2.0_f32.powf(-10.0 * t) * ((t * 10.0 - 0.75) * c4).sin() + 1.0
            }
        }
        5 => 1.0 - (1.0 - t).powi(5),
        6 => -((PI * t).cos() - 1.0) / 2.0,
        _ => t,
    }
}

#[no_mangle]
pub extern "C" fn ca_ease(kind: u32, t: f32) -> f32 {
    ease(kind, t)
}

/// Safe spring step. Semi-implicit Euler with substeps of at most 1/120 s.
pub fn spring_step(
    mut pos: f32,
    mut vel: f32,
    target: f32,
    stiffness: f32,
    damping: f32,
    dt: f32,
) -> (f32, f32) {
    if !(dt > 0.0) {
        return (pos, vel);
    }
    const MAX_STEP: f32 = 1.0 / 120.0;
    let steps = ((dt / MAX_STEP).ceil() as u32).clamp(1, 1000);
    let h = dt / steps as f32;
    for _ in 0..steps {
        let accel = stiffness * (target - pos) - damping * vel;
        vel += accel * h;
        pos += vel * h;
    }
    (pos, vel)
}

/// # Safety
/// `out` must point to at least two writable `f32` values.
#[no_mangle]
pub unsafe extern "C" fn ca_spring_step(
    pos: f32,
    vel: f32,
    target: f32,
    stiffness: f32,
    damping: f32,
    dt: f32,
    out: *mut f32,
) {
    let (p, v) = spring_step(pos, vel, target, stiffness, damping, dt);
    if out.is_null() {
        return;
    }
    // SAFETY: caller guarantees `out` has room for two floats.
    unsafe {
        out.write(p);
        out.add(1).write(v);
    }
}

/// Safe keyframe sampler. Value between key i and i+1 is eased by `easing[i]`;
/// clamps to the first/last value outside the time range.
pub fn sample_track(times: &[f32], values: &[f32], easing: &[u32], t: f32) -> f32 {
    let n = times.len().min(values.len());
    if n == 0 {
        return 0.0;
    }
    if n == 1 || t <= times[0] {
        return values[0];
    }
    if t >= times[n - 1] {
        return values[n - 1];
    }
    let mut i = 0;
    while i + 2 < n && t >= times[i + 1] {
        i += 1;
    }
    let span = times[i + 1] - times[i];
    let local = if span > 0.0 { (t - times[i]) / span } else { 1.0 };
    let kind = easing.get(i).copied().unwrap_or(0);
    let e = ease(kind, local);
    values[i] + (values[i + 1] - values[i]) * e
}

/// # Safety
/// `times` and `values` must point to `count` readable `f32`s and `easing` to
/// `count - 1` (or more) readable `u32`s. A null pointer or zero count yields 0.
#[no_mangle]
pub unsafe extern "C" fn ca_sample_track(
    times: *const f32,
    values: *const f32,
    easing: *const u32,
    count: u32,
    t: f32,
) -> f32 {
    if times.is_null() || values.is_null() || count == 0 {
        return 0.0;
    }
    let n = count as usize;
    // SAFETY: caller guarantees the buffers are valid for `count` elements.
    let (ts, vs) = unsafe {
        (
            std::slice::from_raw_parts(times, n),
            std::slice::from_raw_parts(values, n),
        )
    };
    let es: &[u32] = if easing.is_null() || n < 2 {
        &[]
    } else {
        // SAFETY: caller guarantees `easing` holds at least `count - 1` elements.
        unsafe { std::slice::from_raw_parts(easing, n - 1) }
    };
    sample_track(ts, vs, es, t)
}

/// Block placement transform: [scaleX, scaleY, scaleZ, offX, offY, offZ, alpha(unused = 1)].
///
/// Styles: 0 Settle, 1 Pop, 2 Slide, 3 Drop. `progress` is clamped to [0,1].
/// Offsets are in blocks, relative to the final block position.
pub fn place_transform(style: u32, progress: f32, dx: f32, dy: f32, dz: f32) -> [f32; 7] {
    let p = clamp01(progress);
    match style {
        // Scale 0.6 -> 1 with overshoot, slide in from -dir by 0.35 blocks.
        0 => {
            let s = 0.6 + 0.4 * ease(3, p);
            let k = 0.35 * (1.0 - ease(1, p));
            [s, s, s, -dx * k, -dy * k, -dz * k, 1.0]
        }
        // Scale 0 -> 1 with overshoot.
        1 => {
            let s = ease(3, p);
            [s, s, s, 0.0, 0.0, 0.0, 1.0]
        }
        // Slide in from 1 block along dir, no scale.
        2 => {
            let k = 1.0 - ease(1, p);
            [1.0, 1.0, 1.0, dx * k, dy * k, dz * k, 1.0]
        }
        // Fall from +0.5 y; brief squash on landing (last 30% of the animation).
        3 => {
            const FALL_END: f32 = 0.7;
            if p < FALL_END {
                let f = p / FALL_END;
                // ease-in quad: accelerating fall
                [1.0, 1.0, 1.0, 0.0, 0.5 * (1.0 - f * f), 0.0, 1.0]
            } else {
                let q = (p - FALL_END) / (1.0 - FALL_END);
                let squash = 0.08 * (PI * q).sin();
                [1.0 + squash * 0.5, 1.0 - squash, 1.0 + squash * 0.5, 0.0, 0.0, 0.0, 1.0]
            }
        }
        _ => [1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0],
    }
}

/// # Safety
/// `out` must point to at least seven writable `f32` values.
#[no_mangle]
pub unsafe extern "C" fn ca_place_transform(
    style: u32,
    progress: f32,
    dir_x: f32,
    dir_y: f32,
    dir_z: f32,
    out: *mut f32,
) {
    if out.is_null() {
        return;
    }
    let r = place_transform(style, progress, dir_x, dir_y, dir_z);
    for (i, v) in r.iter().enumerate() {
        // SAFETY: caller guarantees seven writable floats.
        unsafe { out.add(i).write(*v) };
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn version_is_one() {
        assert_eq!(ca_version(), 1);
    }

    #[test]
    fn ease_endpoints() {
        for k in 0..=6 {
            assert!(ease(k, 0.0).abs() < 1e-5, "kind {k} at 0");
            assert!((ease(k, 1.0) - 1.0).abs() < 1e-5, "kind {k} at 1");
            assert_eq!(ease(k, -3.0), ease(k, 0.0));
            assert_eq!(ease(k, 9.0), ease(k, 1.0));
        }
    }

    #[test]
    fn back_overshoots() {
        assert!(ease(3, 0.8) > 1.0);
    }

    #[test]
    fn spring_converges() {
        let (mut p, mut v) = (0.0, 0.0);
        for _ in 0..600 {
            (p, v) = spring_step(p, v, 1.0, 120.0, 18.0, 1.0 / 60.0);
        }
        assert!((p - 1.0).abs() < 1e-3 && v.abs() < 1e-3);
    }

    #[test]
    fn spring_ffi_writes_out() {
        let mut out = [0.0f32; 2];
        unsafe { ca_spring_step(0.0, 0.0, 1.0, 100.0, 10.0, 0.05, out.as_mut_ptr()) };
        assert!(out[0] > 0.0 && out[1] > 0.0);
    }

    #[test]
    fn track_sampling() {
        let t = [0.0, 1.0, 2.0];
        let v = [0.0, 10.0, 0.0];
        let e = [0, 0];
        assert_eq!(sample_track(&t, &v, &e, -1.0), 0.0);
        assert_eq!(sample_track(&t, &v, &e, 5.0), 0.0);
        assert!((sample_track(&t, &v, &e, 0.5) - 5.0).abs() < 1e-5);
        assert!((sample_track(&t, &v, &e, 1.5) - 5.0).abs() < 1e-5);
        let r = unsafe { ca_sample_track(t.as_ptr(), v.as_ptr(), e.as_ptr(), 3, 1.0) };
        assert!((r - 10.0).abs() < 1e-5);
        assert_eq!(unsafe { ca_sample_track(std::ptr::null(), v.as_ptr(), e.as_ptr(), 3, 1.0) }, 0.0);
    }

    #[test]
    fn place_ends_at_identity() {
        for s in 0..=3 {
            let r = place_transform(s, 1.0, 0.0, 1.0, 0.0);
            assert!((r[0] - 1.0).abs() < 1e-4 && (r[1] - 1.0).abs() < 1e-4 && (r[2] - 1.0).abs() < 1e-4, "style {s}");
            assert!(r[3].abs() < 1e-4 && r[4].abs() < 1e-4 && r[5].abs() < 1e-4, "style {s}");
            assert_eq!(r[6], 1.0);
        }
    }

    #[test]
    fn place_starts() {
        let settle = place_transform(0, 0.0, 0.0, 1.0, 0.0);
        assert!((settle[0] - 0.6).abs() < 1e-5 && (settle[4] + 0.35).abs() < 1e-5);
        assert_eq!(place_transform(1, 0.0, 0.0, 1.0, 0.0)[0], 0.0);
        assert!((place_transform(2, 0.0, 1.0, 0.0, 0.0)[3] - 1.0).abs() < 1e-5);
        assert!((place_transform(3, 0.0, 0.0, 1.0, 0.0)[4] - 0.5).abs() < 1e-5);
    }
}
