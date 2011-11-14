package net.hockeyapp.bamboo;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork.TextProvider;

public class UploadTaskConfigurator extends AbstractTaskConfigurator
{
    private TextProvider textProvider;

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("apitoken", params.getString("apitoken"));
        config.put("ipa", params.getString("ipa"));
        config.put("dsym", params.getString("dsym"));
        config.put("notes", params.getString("notes"));
        config.put("notes_type", params.getString("notes_type"));
        config.put("download", params.getString("download"));
        config.put("notify", params.getString("notify"));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);

        context.put("apitoken", taskDefinition.getConfiguration().get("apitoken"));
        context.put("ipa", taskDefinition.getConfiguration().get("ipa"));
        context.put("dsym", taskDefinition.getConfiguration().get("dsym"));
        context.put("notes", taskDefinition.getConfiguration().get("notes"));
        context.put("notes_type", taskDefinition.getConfiguration().get("notes_type"));
        context.put("download", taskDefinition.getConfiguration().get("download"));
        context.put("notify", taskDefinition.getConfiguration().get("notify"));
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        
        context.put("ipa", taskDefinition.getConfiguration().get("ipa"));
        context.put("dsym", taskDefinition.getConfiguration().get("dsym"));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        String apiToken = params.getString("apitoken");
        if (StringUtils.isEmpty(apiToken)) {
            errorCollection.addError("apitoken", textProvider.getText("hockey.apitoken.error"));
        }
        
        String ipa = params.getString("ipa");
        if (StringUtils.isEmpty(ipa)) {
            errorCollection.addError("ipa", textProvider.getText("hockey.ipa.error"));
        }
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
