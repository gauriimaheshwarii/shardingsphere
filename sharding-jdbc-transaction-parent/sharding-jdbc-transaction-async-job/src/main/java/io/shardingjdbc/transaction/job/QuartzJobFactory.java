/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.transaction.job;

import io.shardingjdbc.transaction.datasource.impl.RdbTransactionLogDataSource;
import io.shardingjdbc.transaction.storage.TransactionLogStorageFactory;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.JobBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Soft transaction base quartz job factory.
 *
 * @author wangkai
 */
@RequiredArgsConstructor
public final class QuartzJobFactory {

    private final QuartzJobConfiguration quartzJobConfiguration;

    /**
     * start job.
     *
     * @throws SchedulerException quartz scheduler exception
     */
    public void start() throws SchedulerException {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.scheduleJob(buildJobDetail(), buildTrigger());
        scheduler.start();
    }

    private JobDetail buildJobDetail() {
        JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(quartzJobConfiguration.getJobConfig().getName() + "-Job").build();
        jobDetail.getJobDataMap().put("quartzJobConfiguration", quartzJobConfiguration);
        jobDetail.getJobDataMap().put("transactionLogStorage",
                TransactionLogStorageFactory.createTransactionLogStorage(new RdbTransactionLogDataSource(quartzJobConfiguration.getDefaultTransactionLogDataSource())));
        return jobDetail;
    }

    private Trigger buildTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(quartzJobConfiguration.getJobConfig().getName() + "-Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(quartzJobConfiguration.getJobConfig().getCron())).build();
    }
}
