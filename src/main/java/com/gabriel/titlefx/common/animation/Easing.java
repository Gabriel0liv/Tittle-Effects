package com.gabriel.titlefx.common.animation;

import java.util.Locale;

public enum Easing {
    LINEAR {
        @Override
        public float ease(float t) {
            return t;
        }
    },
    EASE_IN {
        @Override
        public float ease(float t) {
            return t * t;
        }
    },
    EASE_OUT {
        @Override
        public float ease(float t) {
            return t * (2 - t);
        }
    },
    EASE_IN_OUT {
        @Override
        public float ease(float t) {
            return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
        }
    },
    EASE_OUT_BACK {
        @Override
        public float ease(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            float tm1 = t - 1f;
            return 1f + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1;
        }
    },
    EASE_OUT_BOUNCE {
        @Override
        public float ease(float t) {
            float n1 = 7.5625f;
            float d1 = 2.75f;

            if (t < 1f / d1) {
                return n1 * t * t;
            } else if (t < 2f / d1) {
                float tr = t - 1.5f / d1;
                return n1 * tr * tr + 0.75f;
            } else if (t < 2.5f / d1) {
                float tr = t - 2.25f / d1;
                return n1 * tr * tr + 0.9375f;
            } else {
                float tr = t - 2.625f / d1;
                return n1 * tr * tr + 0.984375f;
            }
        }
    };

    public abstract float ease(float t);

    public static Easing fromString(String name) {
        try {
            return Easing.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
