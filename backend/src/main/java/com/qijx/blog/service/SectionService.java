package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.Section;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.SectionRepository;

@Service
public class SectionService {
    private final SectionRepository sectionRepository;
    private final CurrentUserService currentUserService;

    public SectionService(SectionRepository sectionRepository, CurrentUserService currentUserService){
        this.sectionRepository = sectionRepository;
        this.currentUserService = currentUserService;
    }

    public List<Section> listSections(){
        return sectionRepository.findAll(true);
    }

    public List<Section> listAllSections(String authorizationHeader){
        checkAdmin(authorizationHeader);
        return sectionRepository.findAll(false);
    }

    public Section getSection(Long id){
        return sectionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section does not exist"));
    }

    public Section getEnabledSectionBySlug(String slug){
        return sectionRepository.findBySlug(slug).filter(section -> Boolean.TRUE.equals(section.getEnabled()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enabled section not found"));
    }

    public Long resolveSectionId(Long sectionId, String sectionSlug){
        if(sectionSlug != null && !sectionSlug.isBlank()){
            Long slugId = getEnabledSectionBySlug(sectionSlug.trim()).getId();
            if(sectionId != null && !sectionId.equals(slugId)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section ID and slug do not match");
            }
            return slugId;
        }
        if(sectionId != null){
            Section section = getSection(sectionId);
            if(!section.getEnabled()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section is disabled");
            }
        }
        return sectionId;
    }

    public void validateAssignment(Long sectionId, Long previousSectionId){
        if(sectionId == null){
            return;
        }
        Section section = getSection(sectionId);
        if(!section.getEnabled() && !Objects.equals(sectionId, previousSectionId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign content to a disabled section");
        }
    }

    public Section createSection(Section section, String authorizationHeader){
        checkAdmin(authorizationHeader);
        section.setName(section.getName().trim());
        section.setCreatedAt(LocalDateTime.now());
        section.setUpdatedAt(section.getCreatedAt());
        try{
            return sectionRepository.save(section);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section slug already exists");
        }
    }

    public Section updateSection(Long id, Section section, String authorizationHeader){
        checkAdmin(authorizationHeader);
        getSection(id);
        section.setName(section.getName().trim());
        section.setUpdatedAt(LocalDateTime.now());
        try{
            sectionRepository.update(id, section);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section slug already exists");
        }
        return getSection(id);
    }

    public Section updateEnabled(Long id, boolean enabled, String authorizationHeader){
        checkAdmin(authorizationHeader);
        getSection(id);
        sectionRepository.updateEnabled(id, enabled, LocalDateTime.now());
        return getSection(id);
    }

    private void checkAdmin(String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        if(user.getRole() != Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can manage sections");
        }
    }
}
