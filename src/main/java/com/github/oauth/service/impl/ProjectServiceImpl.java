package com.github.oauth.service.impl;

import com.google.cloud.firestore.Firestore;
import com.github.oauth.exception.ResourceNotFound;
import com.github.oauth.model.*;

import com.github.oauth.payload.ProjectDTO;
import com.github.oauth.repository.ProjectRepository;
import com.github.oauth.repository.TechRepository;
import com.github.oauth.service.GitHubService;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import java.util.List;
import java.util.stream.Collectors;

import com.github.oauth.service.ProjectService;

@Service
public class ProjectServiceImpl implements ProjectService {

    private ProjectRepository projectRepository;

    private ModelMapper modelMapper;

    private TechRepository techRepository;

    private Firestore firestore;

    private GitHubService githubService;

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    public ProjectServiceImpl(ProjectRepository projectRepository, ModelMapper modelMapper,
            TechRepository techRepository, Firestore firestore, GitHubService githubService) {
        this.projectRepository = projectRepository;
        this.modelMapper = modelMapper;
        this.techRepository = techRepository;
        this.firestore = firestore;
        this.githubService = githubService;
    }

    @Override
    @Transactional
    public ProjectDTO createNewProject(ProjectDTO projectDTO, User user) {
        try {
            // Create GitHub repository
            String repoName = projectDTO.getProjectName().toLowerCase().replaceAll("\\s+", "-");
            String repoUrl = githubService.createRepository(repoName, projectDTO.getDescription(), true)
                    .getHtmlUrl().toString();
            logger.info("Created GitHub repository: {}", repoUrl);

            // Create project entity
            Project project = new Project();
            project.setProjectName(projectDTO.getProjectName());
            project.setCreator(user);
            project.setProjectStatus(ProjectStatus.OPEN);
            project.setDescription(projectDTO.getDescription());
            project.setGithubRepository(repoUrl);

            Set<User> members = new HashSet<>();
            members.add(user);
            project.setMembers(members);

            Set<Tech> techStack = new HashSet<>();
            if (projectDTO.getTechStack() != null) {
                projectDTO.getTechStack().forEach(tech -> {
                    Tech foundTech = techRepository.findByTechName(tech);
                    if (foundTech == null)
                        throw new ResourceNotFound("Tech not found: " + tech);
                    else
                        techStack.add(foundTech);
                });
            }
            project.setTechStack(techStack);

            Project savedProject = projectRepository.save(project);

            // Update user's projects
            user.getCreatedProjects().add(savedProject);
            user.getProjects().add(savedProject);

            ProjectDTO savedProjectDTO = modelMapper.map(savedProject, ProjectDTO.class);
            savedProjectDTO.setTechStack(projectDTO.getTechStack());

            return savedProjectDTO;
        } catch (Exception e) {
            logger.error("Error creating project and GitHub repository", e);
            throw new RuntimeException("Failed to create project and GitHub repository", e);
        }
    }

    @Override
    public ProjectDTO updateProject(ProjectDTO projectDTO, User user) {
        Project project = projectRepository.findById(projectDTO.getProjectId())
                .orElseThrow(
                        () -> new ResourceNotFound("Project not found with projectId " + projectDTO.getProjectId()));

        if (project.getCreator().getId().equals(user.getId())) {
            project.setProjectName(projectDTO.getProjectName()); // projectName
            project.setDescription(projectDTO.getDescription()); // description
            Set<Tech> stack = new HashSet<>();
            if (projectDTO.getTechStack() != null) {
                projectDTO.getTechStack().forEach(
                        tech -> {
                            Tech technology = techRepository.findByTechName(tech);
                            if (technology == null)
                                throw new ResourceNotFound("Tech not found " + tech);
                            else
                                stack.add(technology);
                        });
            }
            project.setTechStack(stack); // techStack
            projectRepository.save(project);
            return projectDTO;
        } else {
            throw new RuntimeException("User with userId " + user.getId() +
                    " is not the creator of the project with projectId " + projectDTO.getProjectId());
        }
    }

    @Override
    @Transactional
    public String deleteProject(Long projectId, User user) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFound("Project not found with projectId: " + projectId));

            if (project.getCreator().getId().equals(user.getId())) {
                // Delete GitHub repository
                String repoName = project.getProjectName().toLowerCase().replaceAll("\\s+", "-");
                githubService.deleteRepository(repoName);
                logger.info("Deleted GitHub repository: {}", repoName);

                // Delete project from database
                projectRepository.delete(project);
                user.getProjects().remove(project);
                user.getCreatedProjects().remove(project);
                return "Project deleted";
            } else {
                return "You are not the creator of the project";
            }
        } catch (Exception e) {
            logger.error("Error deleting project and GitHub repository", e);
            throw new RuntimeException("Failed to delete project and GitHub repository", e);
        }
    }

    @Override
    @Transactional
    public List<ProjectDTO> searchProjectsByTechStack(Set<String> techNames) {
        try {
            // Convert tech names to Tech entities
            Set<Tech> techStack = techNames.stream()
                    .map(name -> {
                        Tech tech = techRepository.findByTechName(name);
                        if (tech == null)
                            throw new ResourceNotFound("Tech not found " + name);
                        return tech;
                    })
                    .collect(Collectors.toSet());

            // Search projects with matching tech stack
            List<Project> projects = projectRepository.findByTechStackOrderByMatchCount(techStack);

            // Convert to DTOs with tech names only
            return projects.stream()
                    .map(project -> {
                        ProjectDTO dto = modelMapper.map(project, ProjectDTO.class);
                        // Convert Set<Tech> to Set<String> containing only tech names
                        Set<String> techNamesOnly = project.getTechStack().stream()
                                .map(Tech::getTechName)
                                .collect(Collectors.toSet());
                        dto.setTechStack(techNamesOnly);
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error searching projects by tech stack", e);
            throw new RuntimeException("Failed to search projects by tech stack", e);
        }
    }

    @Override
    public ProjectDTO getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFound("Project not found with ID: " + projectId));

        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setProjectId(project.getProjectId());
        projectDTO.setProjectName(project.getProjectName());
        projectDTO.setDescription(project.getDescription());
        projectDTO.setGithubRepository(project.getGithubRepository());

        // Convert Set<Tech> to Set<String> containing only tech names
        Set<String> techNames = project.getTechStack().stream()
                .map(Tech::getTechName)
                .collect(Collectors.toSet());
        projectDTO.setTechStack(techNames);

        return projectDTO;
    }
}