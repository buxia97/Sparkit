package com.sparkit.job.quartz;

import com.sparkit.job.model.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * Quartz 调度管理器
 * 管理 Quartz 任务的创建、暂停、恢复、删除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzManager {

    private static final String JOB_KEY_PREFIX = "TASK_";

    private final Scheduler scheduler;

    /**
     * 创建或更新定时任务
     */
    @SuppressWarnings("unchecked")
    public void scheduleJob(Job job) throws SchedulerException {
        String jobKeyName = JOB_KEY_PREFIX + job.getId();
        JobKey jobKey = JobKey.jobKey(jobKeyName);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobKeyName);

        // 如果已存在则先删除
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        Class<? extends org.quartz.Job> jobClass;
        try {
            jobClass = (Class<? extends org.quartz.Job>) Class.forName(job.getInvokeTarget());
        } catch (ClassNotFoundException e) {
            log.error("任务类未找到: {}", job.getInvokeTarget(), e);
            throw new SchedulerException("任务类未找到: " + job.getInvokeTarget());
        }

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(job.getJobName())
                .usingJobData("jobId", job.getId())
                .storeDurably(true)
                .build();

        CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        if (job.getMisfirePolicy() != null) {
            switch (job.getMisfirePolicy()) {
                case 1 -> cronBuilder = cronBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                case 2 -> cronBuilder = cronBuilder.withMisfireHandlingInstructionDoNothing();
                default -> cronBuilder = cronBuilder.withMisfireHandlingInstructionFireAndProceed();
            }
        }

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(cronBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        if (job.getStatus() != null && job.getStatus() == 0) {
            scheduler.pauseJob(jobKey);
        }

        log.info("Quartz 任务已调度: jobId={} cron={}", job.getId(), job.getCronExpression());
    }

    /**
     * 暂停任务
     */
    public void pauseJob(Long jobId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(JOB_KEY_PREFIX + jobId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
            log.info("Quartz 任务已暂停: jobId={}", jobId);
        }
    }

    /**
     * 恢复任务
     */
    public void resumeJob(Long jobId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(JOB_KEY_PREFIX + jobId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
            log.info("Quartz 任务已恢复: jobId={}", jobId);
        }
    }

    /**
     * 删除任务
     */
    public void deleteJob(Long jobId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(JOB_KEY_PREFIX + jobId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.info("Quartz 任务已删除: jobId={}", jobId);
        }
    }

    /**
     * 立即执行一次任务
     */
    public void runOnce(Long jobId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(JOB_KEY_PREFIX + jobId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey);
            log.info("Quartz 任务立即执行: jobId={}", jobId);
        }
    }

    /**
     * 检查任务是否存在
     */
    public boolean exists(Long jobId) throws SchedulerException {
        return scheduler.checkExists(JobKey.jobKey(JOB_KEY_PREFIX + jobId));
    }
}