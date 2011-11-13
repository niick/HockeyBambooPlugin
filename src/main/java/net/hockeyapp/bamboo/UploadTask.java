package net.hockeyapp.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.utils.process.ExternalProcess;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class UploadTask implements TaskType
{
    private final ProcessService processService;
 
    public UploadTask(final ProcessService processService)
    {
        this.processService = processService;
    }
    
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        final String apiToken = taskContext.getConfigurationMap().get("apitoken");
        buildLogger.addBuildLogEntry("Using API Token: " + apiToken);

        final String ipaPath = taskContext.getConfigurationMap().get("ipa");
        final String dsymPath = taskContext.getConfigurationMap().get("dsym");
        final String notes = taskContext.getConfigurationMap().get("notes");
        final String download = taskContext.getConfigurationMap().get("download");
        final String notify = taskContext.getConfigurationMap().get("notify");
        
        List command = null;
        if (StringUtils.isEmpty(dsymPath)) {
          command = Arrays.asList("/usr/bin/curl", 
                                  "-H", "X-HockeyAppToken: " + apiToken,
                                  "-F", "ipa=@" + ipaPath,
                                  "-F", "notes=" + notes,
                                  "-F", "status=" + (download.equalsIgnoreCase("true") ? "2" : "1"),
                                  "-F", "notify=" + (notify.equalsIgnoreCase("true") ? "1" : "0"),
                                  "https://rink.hockeyapp.net/api/2/apps/upload");
        }
        else {
          command = Arrays.asList("/usr/bin/curl", 
                                  "-H", "X-HockeyAppToken: " + apiToken,
                                  "-F", "ipa=@" + ipaPath,
                                  "-F", "dsym=@" + dsymPath,
                                  "-F", "notes=" + notes,
                                  "-F", "status=" + (download.equalsIgnoreCase("true") ? "2" : "1"),
                                  "-F", "notify=" + (notify.equalsIgnoreCase("true") ? "1" : "0"),
                                  "https://rink.hockeyapp.net/api/2/apps/upload");
        }
                                       

        ExternalProcess process = processService.createProcess(taskContext, 
                new ExternalProcessBuilder()
                .command(command)
                .workingDirectory(taskContext.getWorkingDirectory()));
 
        process.execute();
        
        return TaskResultBuilder.create(taskContext).checkReturnCode(process, 0).build();
    }
}