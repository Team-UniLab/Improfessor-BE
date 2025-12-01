package org.unilab.improfessorbe.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

	private Long id;

	private String nickname;

	private String university;

	private String major;

	private String recommendNickname;

}
