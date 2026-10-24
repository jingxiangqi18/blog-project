package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.entity.Tag;
import com.qijx.blog.service.TagService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService){
        this.tagService = tagService;
    }

    @GetMapping
    public List<Tag> listTags(@RequestParam(required = false) String keyword){
        return tagService.listTags(keyword);
    }

    @GetMapping("/{id}")
    public Tag getTag(@PathVariable Long id){
        return tagService.getTag(id);
    }

    @GetMapping("/slug/{slug}")
    public Tag getTagBySlug(@PathVariable String slug){
        return tagService.getTagBySlug(slug);
    }

    @PostMapping
    public Tag createTag(
        @Valid @RequestBody Tag tag,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return tagService.createTag(tag, authorizationHeader);
    }

    @PutMapping("/{id}")
    public Tag updateTag(
        @PathVariable Long id,
        @Valid @RequestBody Tag tag,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return tagService.updateTag(id, tag, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteTag(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        tagService.deleteTag(id, authorizationHeader);
    }
}
