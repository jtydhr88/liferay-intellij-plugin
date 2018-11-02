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

package com.liferay.ide.idea.ui.actions;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import icons.LiferayIcons;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

/**
 * @author Joye Luo
 * @author Simon Jiang
 */
public class InitBundleMavenAction extends AbstractLiferayMavenGoalAction {

	public InitBundleMavenAction() {
		super("InitBundle", "Run initBundle goal", LiferayIcons.LIFERAY_ICON);

		goals = Arrays.asList("bundle-support:init");
	}

	@Override
	public boolean isEnabledAndVisible(AnActionEvent event) {
		Project project = event.getProject();

		VirtualFile baseDir = project.getBaseDir();

		VirtualFile gradleFile = baseDir.findChild("pom.xml");

		if (baseDir.equals(getVirtualFile(event)) && (gradleFile != null)) {
			return true;
		}

		return false;
	}

	@Override
	protected void handleProcessTerminated(
		@NotNull final String executorIdLocal, @NotNull final ExecutionEnvironment environmentLocal,
		@NotNull ProcessHandler handler) {

		configExcludeFolder();
	}

}