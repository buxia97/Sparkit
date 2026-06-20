package com.sparkit.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.management.*;
import java.io.File;
import java.lang.management.*;
import java.util.*;

/**
 * 服务监控服务
 * 包括：CPU、内存、磁盘、JVM、数据库连接池等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final JdbcTemplate jdbcTemplate;

    /** 获取系统信息 */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        info.put("osName", os.getName());
        info.put("osVersion", os.getVersion());
        info.put("arch", os.getArch());
        info.put("availableProcessors", os.getAvailableProcessors());
        info.put("systemLoadAverage", String.format("%.2f", os.getSystemLoadAverage()));
        return info;
    }

    /** 获取 CPU 信息 */
    public Map<String, Object> getCpuInfo() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpu.put("processCpuLoad", String.format("%.2f%%", sunOs.getProcessCpuLoad() * 100));
            cpu.put("systemCpuLoad", String.format("%.2f%%", sunOs.getSystemCpuLoad() * 100));
            cpu.put("processCpuTime", sunOs.getProcessCpuTime());
        }
        return cpu;
    }

    /** 获取内存信息 */
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> memory = new LinkedHashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        memory.put("heapUsed", formatBytes(heap.getUsed()));
        memory.put("heapMax", formatBytes(heap.getMax()));
        memory.put("heapCommitted", formatBytes(heap.getCommitted()));
        memory.put("heapUsagePercent", String.format("%.1f%%",
                (double) heap.getUsed() / heap.getMax() * 100));

        memory.put("nonHeapUsed", formatBytes(nonHeap.getUsed()));
        memory.put("nonHeapCommitted", formatBytes(nonHeap.getCommitted()));

        Runtime runtime = Runtime.getRuntime();
        memory.put("totalMemory", formatBytes(runtime.totalMemory()));
        memory.put("freeMemory", formatBytes(runtime.freeMemory()));
        memory.put("maxMemory", formatBytes(runtime.maxMemory()));

        return memory;
    }

    /** 获取 JVM 信息 */
    public Map<String, Object> getJvmInfo() {
        Map<String, Object> jvm = new LinkedHashMap<>();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        jvm.put("vmName", runtime.getVmName());
        jvm.put("vmVendor", runtime.getVmVendor());
        jvm.put("vmVersion", runtime.getVmVersion());
        jvm.put("startTime", new Date(runtime.getStartTime()).toString());
        jvm.put("upTime", formatUptime(runtime.getUptime()));

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        jvm.put("threadCount", threadMXBean.getThreadCount());
        jvm.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        jvm.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

        // GC 信息
        List<Map<String, String>> gcInfo = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, String> gcMap = new LinkedHashMap<>();
            gcMap.put("name", gc.getName());
            gcMap.put("count", String.valueOf(gc.getCollectionCount()));
            gcMap.put("time", gc.getCollectionTime() + "ms");
            gcInfo.add(gcMap);
        }
        jvm.put("gcInfo", gcInfo);

        return jvm;
    }

    /** 获取磁盘信息 */
    public Map<String, Object> getDiskInfo() {
        Map<String, Object> disk = new LinkedHashMap<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                Map<String, Object> drive = new LinkedHashMap<>();
                drive.put("path", root.getAbsolutePath());
                drive.put("total", formatBytes(root.getTotalSpace()));
                drive.put("free", formatBytes(root.getFreeSpace()));
                drive.put("usable", formatBytes(root.getUsableSpace()));
                drive.put("usagePercent", String.format("%.1f%%",
                        (1 - (double) root.getUsableSpace() / root.getTotalSpace()) * 100));
                disk.put(root.getAbsolutePath(), drive);
            }
        }
        return disk;
    }

    /** 获取数据库连接池信息 */
    public Map<String, Object> getDbInfo() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            // 数据库版本
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            db.put("dbVersion", version);

            // 连接池信息（HikariCP）
            db.put("poolInfo", getHikariPoolInfo());
        } catch (Exception e) {
            log.debug("获取数据库信息失败: {}", e.getMessage());
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> getHikariPoolInfo() {
        Map<String, Object> pool = new LinkedHashMap<>();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (*");
            Set<ObjectName> poolNames = mBeanServer.queryNames(poolName, null);
            if (poolNames != null && !poolNames.isEmpty()) {
                ObjectName objectName = poolNames.iterator().next();
                pool.put("activeConnections", mBeanServer.getAttribute(objectName, "ActiveConnections"));
                pool.put("idleConnections", mBeanServer.getAttribute(objectName, "IdleConnections"));
                pool.put("totalConnections", mBeanServer.getAttribute(objectName, "TotalConnections"));
                pool.put("pendingThreads", mBeanServer.getAttribute(objectName, "ThreadsAwaitingConnection"));
            }
        } catch (Exception e) {
            pool.put("error", "无法获取 HikariCP 连接池信息");
        }
        return pool;
    }

    /** 获取所有监控信息汇总 */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("system", getSystemInfo());
        metrics.put("cpu", getCpuInfo());
        metrics.put("memory", getMemoryInfo());
        metrics.put("jvm", getJvmInfo());
        metrics.put("disk", getDiskInfo());
        metrics.put("db", getDbInfo());
        return metrics;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatUptime(long uptime) {
        long days = uptime / (1000 * 60 * 60 * 24);
        long hours = (uptime / (1000 * 60 * 60)) % 24;
        long minutes = (uptime / (1000 * 60)) % 60;
        return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
    }
}