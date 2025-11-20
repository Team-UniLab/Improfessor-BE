package org.unilab.improfessorbe.domain.problem.domain;

import java.time.LocalDateTime;

import org.unilab.improfessorbe.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long roundId;

	private String type;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "answer", columnDefinition = "TEXT")
	private String answer;

	private LocalDateTime savedAt;

	private Problem(Long roundId, String type, String content,
		String description, String answer) {
		this.roundId = roundId;
		this.type = type;
		this.content = content;
		this.description = description;
		this.answer = answer;
	}

	public static Problem create(Long roundId, String type, String content,
		String description, String answer) {
		return new Problem(roundId, type, content, description, answer);
	}

	public void save() {
		this.savedAt = LocalDateTime.now();
	}

	public void unsave() {
		this.savedAt = null;
	}

	public boolean isSaved() {
		return this.savedAt != null;
	}

}
