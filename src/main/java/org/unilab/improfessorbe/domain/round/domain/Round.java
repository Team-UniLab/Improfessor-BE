package org.unilab.improfessorbe.domain.round.domain;

import org.unilab.improfessorbe.global.common.BaseEntity;

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
public class Round extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	private String name;

	private Round(Long userId, String name) {
		this.userId = userId;
		this.name = name;
	}

	public static Round create(Long userId, String roundName) {
		return new Round(userId, roundName);
	}

	public boolean isDeleted() {
		return this.getDeletedAt() != null;
	}

	public boolean isExpired() {
		return this.getCreatedAt().plusDays(7).isBefore(java.time.LocalDateTime.now());
	}
}