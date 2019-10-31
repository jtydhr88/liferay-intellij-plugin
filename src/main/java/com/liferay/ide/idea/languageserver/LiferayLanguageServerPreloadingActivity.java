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

package com.liferay.ide.idea.languageserver;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;

import com.liferay.ide.idea.util.SocketSupport;

import java.io.File;

import java.util.Properties;

import org.jetbrains.annotations.NotNull;

import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

/**
 * @author Dominik Marks
 * @author Terry Jia
 */
public class LiferayLanguageServerPreloadingActivity extends PreloadingActivity implements SocketSupport {

	@Override
	public void preload(@NotNull ProgressIndicator progressIndicator) {
		Properties properties = System.getProperties();

		File temp = new File(properties.getProperty("user.home"), ".liferay-intellij-plugin");

		File liferayPropertiesServerJar = new File(temp, "liferay-properties-server-all.jar");

		if (liferayPropertiesServerJar.exists()) {
			int port = findUnusedPort(10000, 60000);

			String[] args = {
				"java", "-DliferayLanguageServerPort=" + port, "-jar", liferayPropertiesServerJar.getAbsolutePath()
			};

			RawCommandServerDefinition rawCommandServerDefinition = new RawCommandServerDefinition("properties", args);

			IntellijLanguageClient.addServerDefinition(rawCommandServerDefinition);
		}
	}

}