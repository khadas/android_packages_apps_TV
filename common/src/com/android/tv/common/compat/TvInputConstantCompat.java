package com.android.tv.common.compat;

/** Temp TIF Compatibility for {@link TvInputManager} constants. */
public class TvInputConstantCompat {

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>SIGNAL_STRENGTH_NOT_USED means the TV Input does not report signal strength. Each onTune
     * command implicitly resets the TV App's signal strength state to SIGNAL_STRENGTH_NOT_USED.
     */
    public static final int SIGNAL_STRENGTH_NOT_USED = -3;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>SIGNAL_STRENGTH_ERROR means exception/error when handling signal strength.
     */
    public static final int SIGNAL_STRENGTH_ERROR = -2;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>SIGNAL_STRENGTH_UNKNOWN means the TV Input supports signal strength, but does not
     * currently know what the strength is.
     */
    public static final int SIGNAL_STRENGTH_UNKNOWN = -1;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>0 - 100 represents signal strength percentage. Strength is divided into 5 levels. (0 - 4)
     * SIGNAL_STRENGTH_0_OF_4_UPPER_BOUND is the upper boundary of level 0. [0%, 15%] And the lower
     * boundary of level 1. (15%, 30%]
     */
    public static final int SIGNAL_STRENGTH_0_OF_4_UPPER_BOUND = 20;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>0 - 100 represents strength percentage. Strength is divided into 5 levels. (0 - 4)
     * SIGNAL_STRENGTH_1_OF_4_UPPER_BOUND is the upper boundary of level 1. (15%, 30%] And the lower
     * boundary of level 2. (30%, 45%]
     */
    public static final int SIGNAL_STRENGTH_1_OF_4_UPPER_BOUND = 40;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>0 - 100 represents strength percentage. Strength is divided into 5 levels. (0 - 4)
     * SIGNAL_STRENGTH_2_OF_4_UPPER_BOUND is the upper boundary of level 3. (30%, 45%] And the lower
     * boundary of level 3. (45%, 60%]
     */
    public static final int SIGNAL_STRENGTH_2_OF_4_UPPER_BOUND = 60;

    /**
     * Status for {@link TisSessionCompat#notifySignalStrength(int)} and
     * {@link TvViewCompat.TvInputCallback#onTimeShiftStatusChanged(String, int)}:
     *
     * <p>0 - 100 represents strength percentage. Strength is divided into 5 levels. (0 - 4)
     * SIGNAL_STRENGTH_3_OF_4_UPPER_BOUND is the upper boundary of level 3. (45%, 60%] And the lower
     * boundary of level 4. (60%, 100%]
     */
    public static final int SIGNAL_STRENGTH_3_OF_4_UPPER_BOUND = 80;
}
