package de.keksuccino.fancymenu.util;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PerformanceUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final OperatingSystemMXBean OS_BEAN = createOperatingSystemBean();

    /**
     * The current CPU usage of the Java Virtual Machine.<br><br>
     *
     * Returns a value between 0.0D (0% usage) and 1.0D (100% usage).<br>
     * Returns 0.0D when the system failed to provide a valid value.
     */
    public static double getJvmCpuUsage() {
        if (OS_BEAN == null) return 0D;
        return sanitizeCpuLoad(OS_BEAN.getProcessCpuLoad());
    }

    /**
     * The current CPU usage of the Operating System.<br><br>
     *
     * Returns a value between 0.0D (0% usage) and 1.0D (100% usage).<br>
     * Returns 0.0D when the system failed to provide a valid value.
     */
    public static double getOsCpuUsage() {
        if (OS_BEAN == null) return 0D;
        return sanitizeCpuLoad(OS_BEAN.getCpuLoad());
    }

    private static double sanitizeCpuLoad(double cpuLoad) {
        if (!Double.isFinite(cpuLoad)) return 0D;
        if (cpuLoad < 0.0D) return 0.0D;
        if (cpuLoad > 1.0D) return 1.0D;
        return cpuLoad;
    }

    private static OperatingSystemMXBean createOperatingSystemBean() {
        try {
            return ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to initialize OperatingSystemMXBean for performance metrics", ex);
            return null;
        }
    }

}
