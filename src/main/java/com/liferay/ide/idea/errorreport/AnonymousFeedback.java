/*
 * Copyright (c) 2017 Patrick Scheibe
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.liferay.ide.idea.errorreport;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus;

import com.liferay.ide.idea.core.MessagesBundle;
import com.liferay.ide.idea.util.CoreUtil;

import java.io.IOException;

import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import org.jetbrains.annotations.Nullable;

/**
 * Provides functionality to create and send GitHub issues when an exception is thrown by a plugin.
 *
 * @author patrick
 */
class AnonymousFeedback {

	/**
	 * Makes a connection to GitHub. Checks if there is an issue that is a duplicate and based on this, creates either a
	 * new issue or comments on the duplicate (if the user provided additional information).
	 *
	 * @param environmentDetails Information collected by {@link IdeaInformationProxy}
	 * @return The report info that is then used in {@link GitHubErrorReporter} to show the user a balloon with the link
	 * of the created issue.
	 */
	static SubmittedReportInfo _sendFeedback(LinkedHashMap<String, String> environmentDetails) {
		final Logger logger = Logger.getInstance(AnonymousFeedback.class.getName());

		final SubmittedReportInfo result;

		try {
			ClassLoader classLoader = AnonymousFeedback.class.getClassLoader();

			final URL resource = classLoader.getResource(_TOKEN_FILE);

			if (resource == null) {
				logger.info("Could not find token file");

				throw new IOException("Could not decrypt access token");
			}

			final String gitAccessToken = GitHubAccessTokenScrambler._decrypt(resource);
			GitHubClient client = new GitHubClient();

			client.setOAuth2Token(gitAccessToken);

			RepositoryId repoID = new RepositoryId(_GIT_REPO_USER, _GIT_REPO);
			IssueService issueService = new IssueService(client);

			Issue newGitHubIssue = _createNewGitHubIssue(environmentDetails);

			Issue duplicate = _findFirstDuplicate(newGitHubIssue.getTitle(), issueService, repoID);

			boolean newIssue = true;

			if (duplicate != null) {
				newGitHubIssue = duplicate;
				newIssue = false;
			}
			else {
				newGitHubIssue = issueService.createIssue(repoID, newGitHubIssue);
			}

			final long id = newGitHubIssue.getNumber();

			final String htmlUrl = newGitHubIssue.getHtmlUrl();

			final String message = MessagesBundle.message(
				newIssue ? "git.issue.text" : "git.issue.duplicate.text", htmlUrl, id);

			result = new SubmittedReportInfo(
				htmlUrl, message, newIssue ? SubmissionStatus.NEW_ISSUE : SubmissionStatus.DUPLICATE);

			return result;
		}
		catch (Exception e) {
			return new SubmittedReportInfo(
				null, MessagesBundle.message("report.error.connection.failure"), SubmissionStatus.FAILED);
		}
	}

	/**
	 * Turns collected information of an error into a new (offline) GitHub issue
	 *
	 * @param details A map of the information. Note that I remove items from there when they should not go in the issue
	 *                body as well. When creating the body, all remaining items are iterated.
	 * @return The new issue
	 */
	private static Issue _createNewGitHubIssue(LinkedHashMap<String, String> details) {
		String errorMessage = details.get("error.message");

		if (CoreUtil.isNullOrEmpty(errorMessage)) {
			errorMessage = "Unspecified error";
		}

		details.remove("error.message");

		String errorHash = details.get("error.hash");

		if (errorHash == null) {
			errorHash = "";
		}

		details.remove("error.hash");

		final Issue gitHubIssue = new Issue();
		final String body = _generateGitHubIssueBody(details, true);
		gitHubIssue.setTitle(MessagesBundle.message("git.issue.title", errorHash, errorMessage));
		gitHubIssue.setBody(body);

		Label label = new Label();

		label.setName(_ISSUE_LABEL);

		gitHubIssue.setLabels(Collections.singletonList(label));

		return gitHubIssue;
	}

	/**
	 * Collects all issues on the repo and finds the first duplicate that has the same title. For this to work, the title
	 * contains the hash of the stack trace.
	 *
	 * @param uniqueTitle Title of the newly created issue. Since for auto-reported issues the title is always the same,
	 *                    it includes the hash of the stack trace. The title is used so that I don't have to match
	 *                    something in the whole body of the issue.
	 * @param service     Issue-service of the GitHub lib that lets you access all issues
	 * @param repo        The repository that should be used
	 * @return The duplicate if one is found or null
	 * @throws IOException Problems when connecting to GitHub
	 */
	@Nullable
	private static Issue _findFirstDuplicate(String uniqueTitle, final IssueService service, RepositoryId repo) {
		Map<String, String> searchParameters = new HashMap<>(2);

		searchParameters.put(IssueService.FILTER_STATE, "all");

		final PageIterator<Issue> pages = service.pageIssues(repo, searchParameters);

		for (Collection<Issue> page : pages) {
			for (Issue issue : page) {
				if (uniqueTitle.equals(issue.getTitle())) {
					return issue;
				}
			}
		}

		return null;
	}

	/**
	 * Creates the body of the GitHub issue. It will contain information about the system, details provided by the user
	 * and the full stack trace. Everything is formatted using markdown.
	 *
	 * @param details Details provided by {@link IdeaInformationProxy}
	 * @return A markdown string representing the GitHub issue body.
	 */
	private static String _generateGitHubIssueBody(
		LinkedHashMap<String, String> details, final boolean includeStacktrace) {

		String errorDescription = details.get("error.description");

		if (errorDescription == null) {
			errorDescription = "";
		}

		details.remove("error.description");

		String stackTrace = details.get("error.stacktrace");

		if (CoreUtil.isNullOrEmpty(stackTrace)) {
			stackTrace = "invalid stacktrace";
		}

		details.remove("error.stacktrace");

		StringBuilder result = new StringBuilder();

		if (!errorDescription.isEmpty()) {
			result.append(errorDescription);
			result.append("\n\n----------------------\n\n");
		}

		for (Entry<String, String> entry : details.entrySet()) {
			result.append("- ");
			result.append(entry.getKey());
			result.append(": ");
			result.append(entry.getValue());
			result.append("\n");
		}

		if (includeStacktrace) {
			result.append("\n```\n");
			result.append(stackTrace);
			result.append("\n```\n");
		}

		return result.toString();
	}

	private AnonymousFeedback() {
	}

	private static final String _GIT_REPO = "liferay-intellij-plugin";

	private static final String _GIT_REPO_USER = "liferay";

	private static final String _ISSUE_LABEL = "auto-generated";

	private static final String _TOKEN_FILE = "scrambledToken.bin";

}