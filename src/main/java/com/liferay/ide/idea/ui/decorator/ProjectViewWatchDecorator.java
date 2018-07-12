/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.ide.idea.ui.decorator;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskState;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;

import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

/**
 * @author Simon Jiang
 */

public class ProjectViewWatchDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        final VirtualFile virtualFile = node.getVirtualFile();
        Project project = node.getProject();


        if (virtualFile != null && ProjectRootsUtil.isModuleContentRoot(virtualFile,project)) {

            String canonicalPath = virtualFile.getCanonicalPath();

            ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);

            List<ExternalSystemTask> taskList = processingManager.findTasksOfState(GradleConstants.SYSTEM_ID, ExternalSystemTaskState.IN_PROGRESS);

            if (taskList != null) {

                for( ExternalSystemTask task : taskList){

                    if ( task instanceof ExternalSystemExecuteTaskTask){

                        ExternalSystemExecuteTaskTask executeTask = (ExternalSystemExecuteTaskTask)task;

                        List<String> tasksToExecute = executeTask.getTasksToExecute();

                        if ( tasksToExecute.contains("watch")){
                            String externalProjectPath = executeTask.getExternalProjectPath();

                            if ( canonicalPath.equals( externalProjectPath)){
                                String existedLocationString = data.getLocationString();
                                data.setLocationString(existedLocationString!=null? existedLocationString +" [watching]" : "[watching]");
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {

    }
}