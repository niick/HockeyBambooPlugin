package net.hockeyapp.bamboo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import net.hockeyapp.bamboo.CountingMultipartEntity.ProgressListener;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.google.common.collect.Sets;

public class UploadTask implements TaskType {
  private static final String HOCKEY_URL = "https://rink.hockeyapp.net/api/2/apps/upload";

  @NotNull
  @java.lang.Override
  public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException {
    final BuildLogger buildLogger = taskContext.getBuildLogger();

    final String apiToken = taskContext.getConfigurationMap().get("apitoken");
    buildLogger.addBuildLogEntry("Using API Token: " + apiToken);

    TaskResultBuilder taskResult = TaskResultBuilder.create(taskContext);
    PostMethod method = createPostRequest(taskContext, buildLogger, apiToken);
    if ((method != null) && (sendPostRequest(method, buildLogger))) {
      taskResult.success();
    }
    else {
      taskResult.failed();
    }

    return taskResult.build();
  }

  private PostMethod createPostRequest(TaskContext taskContext, BuildLogger buildLogger, String apiToken) {
    final PostMethod method = new PostMethod(HOCKEY_URL);
    method.addRequestHeader("X-HockeyAppToken", apiToken);
    
    final Set<Part> parts = Sets.newHashSet();
    
    final String ipaPath = taskContext.getConfigurationMap().get("ipa");
    if (!addIPAPart(taskContext, parts, ipaPath)) {
      buildLogger.addBuildLogEntry("Could not load the specified IPA file '" + ipaPath + "'");
      return null;
    }
    
    final String dsymPath = taskContext.getConfigurationMap().get("dsym");
    if ((!StringUtils.isEmpty(dsymPath)) && (!addDSYMPart(taskContext, parts, dsymPath))) {
      buildLogger.addBuildLogEntry("Could not load the specified dSYM file '" + dsymPath + "'");
      return null;
    }
    
    final String notes = taskContext.getConfigurationMap().get("notes");
    parts.add(new StringPart("notes", notes));
    
    final String download = taskContext.getConfigurationMap().get("download");
    parts.add(new StringPart("download", (download.equalsIgnoreCase("true") ? "2" : "1")));
    
    final String notify = taskContext.getConfigurationMap().get("notify");
    parts.add(new StringPart("notify", (notify.equalsIgnoreCase("true") ? "1" : "0")));

    final HttpMethodParams params  = new HttpMethodParams();
    MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), params);
    CountingMultipartEntity countingEntity = new CountingMultipartEntity(requestEntity, getLogListener(buildLogger, requestEntity.getContentLength()));
    method.setRequestEntity(countingEntity);

    return method;
  }
  
  private Boolean sendPostRequest(PostMethod method, BuildLogger buildLogger) {
    final HttpClient client = new HttpClient();
    
    try {
      buildLogger.addBuildLogEntry("Uploading build to HockeyApp...");
      final int status = client.executeMethod(method);
      if (status == 201) {
        buildLogger.addBuildLogEntry("Upload completed!");
        return true;
      }
      else {
        final String body = method.getResponseBodyAsString();
        buildLogger.addErrorLogEntry("Upload failed with response:");
        buildLogger.addErrorLogEntry(body);
        return false;
      }
    }
    catch (IOException e) {
      buildLogger.addErrorLogEntry("Upload failed with IOException: " + e.getLocalizedMessage());
      return false;
    }
  }

  private ProgressListener getLogListener(final BuildLogger buildLogger, final long totalSize) {
    return new ProgressListener() {
      private long lastSize = 0;
      
      public void transferred(long size) {
        if ((long)((double)lastSize / (double)totalSize * 10.0) < (long)((double)size / (double)totalSize * 10.0)) {
          buildLogger.addBuildLogEntry("Upload progress: " + (long)((double)size / (double)totalSize * 100) + "%");
        }
        
        lastSize = size;
      }
    };
  }

  private boolean addDSYMPart(TaskContext taskContext, Set<Part> parts, String dsymPath) {
    final File pathToFile = new File(taskContext.getWorkingDirectory(), dsymPath);
    try {
      parts.add(new FilePart("dsym", pathToFile));
      return true;
    }
    catch (FileNotFoundException e) {
      return false;
    }
  }

  private Boolean addIPAPart(TaskContext taskContext, Set<Part> parts, String ipaPath) {
    final File pathToFile = new File(taskContext.getWorkingDirectory(), ipaPath);
    try {
      parts.add(new FilePart("ipa", pathToFile));
      return true;
    }
    catch (FileNotFoundException e) {
      return false;
    }
  }
}