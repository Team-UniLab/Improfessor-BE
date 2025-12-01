package org.unilab.improfessorbe.domain.user.domain;

import org.unilab.improfessorbe.global.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class User extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	private String nickname;

	private String email;

	private String password;

	private String university;

	private String major;

	private Integer freeCount;

	private Integer recommendCount;

	private boolean isRecommend;

	@Enumerated(value = EnumType.STRING)
	private Role role;

	private String provider;

	private String providerId;

	public enum Role {
		ADMIN, USER
	}

	private User(String nickname, String email, String password, String university, String major) {
		this.nickname = nickname;
		this.email = email;
		this.password = password;
		this.university = university;
		this.major = major;
		this.freeCount = 5;
		this.recommendCount = 33;
		this.role = Role.USER;
		this.isRecommend = false;
		this.provider = "local";
		this.providerId = null;
	}

	private User(String nickname, String email, String provider, String providerId) {
		this.nickname = nickname;
		this.email = email;
		this.password = null;
		this.university = null;
		this.major = null;
		this.freeCount = 5;
		this.recommendCount = 33;
		this.role = Role.USER;
		this.isRecommend = false;
		this.provider = provider;
		this.providerId = providerId;
	}

	public static User create(String nickname, String email, String password, String university, String major) {
		return new User(nickname, email, password, university, major);
	}

	public static User createOAuth2User(String nickname, String email, String provider, String providerId) {
		return new User(nickname, email, provider, providerId);
	}

	public void updateUser(String nickname, String university, String major) {
		this.nickname = nickname;
		this.university = university;
		this.major = major;
	}

	public void decrementFreeCount() {
		this.freeCount--;
	}

	public void receiveRecommend() {
		this.freeCount += 3;
		this.recommendCount--;
	}

	public void recommendUser() {
		this.freeCount++;
		this.isRecommend = true;
	}

	public boolean canReceiveRecommendation() {
		return this.recommendCount > 0;
	}

	public boolean canRecommend() {
		return !this.isRecommend;
	}

	public void updateProvider(String provider) {
		this.provider = provider;
	}

	public void updateProviderId(String providerId) {
		this.providerId = providerId;
	}
}
