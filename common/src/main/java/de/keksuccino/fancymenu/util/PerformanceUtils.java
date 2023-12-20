package de.keksuccino.fancymenu.util;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class PerformanceUtils {

    protected static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    /**
     * The current CPU usage of the Java Virtual Machine.<br><br>
     *
     * Returns a value between 0.0D (0% usage) and 1.0D (100% usage).<br>
     * Returns a NEGATIVE VALUE in case the system failed to get the current CPU usage.
     */
    public static double getJvmCpuUsage() {
        if (OS_BEAN == null) return 0D;
        return OS_BEAN.getProcessCpuLoad();
    }

    /**
     * The current CPU usage of the Operating System.<br><br>
     *
     * Returns a value between 0.0D (0% usage) and 1.0D (100% usage).<br>
     * Returns a NEGATIVE VALUE in case the system failed to get the current CPU usage.
     */
    public static double getOsCpuUsage() {
        if (OS_BEAN == null) return 0D;
        return OS_BEAN.getCpuLoad();
    }

}
