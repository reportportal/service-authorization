package com.epam.reportportal.auth.store.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Andrei Varabyeu
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "project_user", schema = "public")
public class ProjectUser implements Serializable {

	@EmbeddedId
	private ProjectUserId id;

	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("project_id")
	private Project project;

	@Column(name = "project_role")
	@Enumerated(EnumType.STRING)
	private ProjectRole role;

	public ProjectUserId getId() {
		return id;
	}

	public void setId(ProjectUserId id) {
		this.id = id;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public ProjectRole getRole() {
		return role;
	}

	public void setRole(ProjectRole role) {
		this.role = role;
	}
}