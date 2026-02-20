package com.haraldsson.syntropy.world;

/**
 * OpenSimplex2 noise (public domain).
 * Fast, high-quality 2D gradient noise for terrain generation.
 */
public final class SimplexNoise {
    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Y = 0x598CD327003817B5L;
    private static final long HASH_SEED = 0x53A3F72DEEC546F5L;
    private static final int N_GRADS_2D = 128;
    private static final double SQRT3 = 1.7320508075688772;
    private static final double G2 = (3 - SQRT3) / 6;

    private static final double[] GRADIENTS_2D;

    static {
        double[] grad2 = {
             1, 0, -1, 0, 0, 1, 0, -1,
             0.7071067811865476, 0.7071067811865476,
            -0.7071067811865476, 0.7071067811865476,
             0.7071067811865476, -0.7071067811865476,
            -0.7071067811865476, -0.7071067811865476,
             0.9238795325112867, 0.3826834323650898,
             0.3826834323650898, 0.9238795325112867,
            -0.3826834323650898, 0.9238795325112867,
            -0.9238795325112867, 0.3826834323650898,
            -0.9238795325112867, -0.3826834323650898,
            -0.3826834323650898, -0.9238795325112867,
             0.3826834323650898, -0.9238795325112867,
             0.9238795325112867, -0.3826834323650898,
        };
        GRADIENTS_2D = new double[N_GRADS_2D * 2];
        for (int i = 0; i < GRADIENTS_2D.length; i++) {
            GRADIENTS_2D[i] = grad2[i % grad2.length];
        }
    }

    private SimplexNoise() {
    }

    /**
     * 2D simplex-style noise. Returns a value roughly in [-1, 1].
     */
    public static double noise2(long seed, double x, double y) {
        double s = (x + y) * ((SQRT3 - 1) / 2);
        double xs = x + s;
        double ys = y + s;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        double xsi = xs - xsb;
        double ysi = ys - ysb;

        double t = (xsi + ysi) * G2;
        double x0 = xsi - t;
        double y0 = ysi - t;

        int i1, j1;
        if (x0 > y0) {
            i1 = 1; j1 = 0;
        } else {
            i1 = 0; j1 = 1;
        }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        double n0 = contribution(seed, xsb, ysb, x0, y0);
        double n1 = contribution(seed, xsb + i1, ysb + j1, x1, y1);
        double n2 = contribution(seed, xsb + 1, ysb + 1, x2, y2);

        return 70.0 * (n0 + n1 + n2);
    }

    private static double contribution(long seed, int xsv, int ysv, double dx, double dy) {
        double attn = 0.5 - dx * dx - dy * dy;
        if (attn <= 0) return 0;
        int gi = gradIndex(seed, xsv, ysv);
        attn *= attn;
        return attn * attn * (GRADIENTS_2D[gi] * dx + GRADIENTS_2D[gi + 1] * dy);
    }

    private static int gradIndex(long seed, int xsv, int ysv) {
        long hash = seed ^ (xsv * PRIME_X) ^ (ysv * PRIME_Y);
        hash *= HASH_SEED;
        hash ^= hash >> 16;
        return (int) ((hash & 0x7FFFFFFFL) % N_GRADS_2D) * 2;
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}

