package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.Tag;
import com.qijx.blog.repository.TagRepository;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;

    public TagService(TagRepository tagRepository, CurrentUserService currentUserService){
        this.tagRepository = tagRepository;
        this.currentUserService = currentUserService;
    }

    public List<Tag> listTags(String keyword){
        return tagRepository.findAll(keyword == null ? "" : keyword.trim());
    }

    public Tag getTag(Long id){
        return tagRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public Tag getTagBySlug(String slug){
        return tagRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public List<Long> validateTagIds(List<Long> tagIds){
        if(tagIds == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tagIds must be an array; use [] to remove all tags");
        }
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(tagIds));
        if(uniqueIds.stream().anyMatch(id -> id == null || id <= 0)
            || tagRepository.countByIds(uniqueIds) != uniqueIds.size()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tag IDs do not exist");
        }
        return uniqueIds;
    }

    public Tag createTag(Tag tag, String authorizationHeader){
        checkAdmin(authorizationHeader);
        tag.setName(tag.getName().trim());
        tag.setCreatedAt(LocalDateTime.now());
        try{
            return tagRepository.save(tag);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag slug already exists");
        }
    }

    public Tag updateTag(Long id, Tag tag, String authorizationHeader){
        checkAdmin(authorizationHeader);
        getTag(id);
        tag.setName(tag.getName().trim());
        try{
            tagRepository.update(id, tag);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag slug already exists");
        }
        return getTag(id);
    }

    public void deleteTag(Long id, String authorizationHeader){
        checkAdmin(authorizationHeader);
        getTag(id);
        tagRepository.deleteById(id);
    }

    private void checkAdmin(String authorizationHeader){
        if(currentUserService.getCurrentEnabledUser(authorizationHeader).getRole() != Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can manage tags");
        }
    }
}
