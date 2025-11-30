package org.unilab.improfessorbe.domain.problem.service;

@FunctionalInterface
public interface ProgressCallback {
	void onProgress(String stage, int progress, String message);
}