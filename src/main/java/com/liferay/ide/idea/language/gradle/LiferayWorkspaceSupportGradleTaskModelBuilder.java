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

package com.liferay.ide.idea.language.gradle;

import java.io.File;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * @author Dominik Marks
 *
 * Class to extract information from the Liferay Workspace gradle plugin
 */
public class LiferayWorkspaceSupportGradleTaskModelBuilder implements ModelBuilderService {

	public LiferayWorkspaceSupportGradleTaskModelBuilder() {
	}

	@Override
	public Object buildAll(String modelName, Project project) {
		LiferayWorkspaceGradleTaskModelImpl liferayWorkspaceGradleTaskModel = new LiferayWorkspaceGradleTaskModelImpl();

		Map<Project, Set<Task>> allTasks = project.getAllTasks(false);

		for (Map.Entry<Project, Set<Task>> tasks : allTasks.entrySet()) {
			for (Task task : tasks.getValue()) {
				String taskName = task.getName();

				if (taskName.equals("initBundle")) {
					try {

						//get getDestinationDir of CopyTask via reflection
						Class<? extends Task> taskClass = task.getClass();

						Method getDestinationDir = taskClass.getDeclaredMethod("getDestinationDir");

						File destinationDir = (File)getDestinationDir.invoke(task);

						String path = "bundles";

						if (destinationDir != null) {
							path = destinationDir.getPath();
						}

						liferayWorkspaceGradleTaskModel.setLiferayHome(path);
					}
					catch (Exception e) {
						//ignore
					}
				}
			}
		}

		return liferayWorkspaceGradleTaskModel;
	}

	@Override
	public boolean canBuild(String modelName) {
		String liferayWorkspaceGradleTaskModelClassName = LiferayWorkspaceGradleTaskModel.class.getName();

		return liferayWorkspaceGradleTaskModelClassName.equals(modelName);
	}

	@NotNull
	@Override
	public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
		ErrorMessageBuilder gradleImportError = ErrorMessageBuilder.create(project, e, "Gradle import error");

		return gradleImportError.withDescription("Unable to import Liferay Workspace configuration");
	}

}