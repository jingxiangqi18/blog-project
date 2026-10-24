package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.dto.UpdateSectionEnabledRequest;
import com.qijx.blog.entity.Section;
import com.qijx.blog.service.SectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sections")
public class SectionController {
    private final SectionService sectionService;

    public SectionController(SectionService sectionService){
        this.sectionService = sectionService;
    }

    @GetMapping
    public List<Section> listSections(){
        return sectionService.listSections();
    }

    @GetMapping("/admin/all")
    public List<Section> listAllSections(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return sectionService.listAllSections(authorizationHeader);
    }

    @GetMapping("/{slug}")
    public Section getSection(@PathVariable String slug){
        return sectionService.getEnabledSectionBySlug(slug);
    }

    @PostMapping
    public Section createSection(
        @Valid @RequestBody Section section,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return sectionService.createSection(section, authorizationHeader);
    }

    @PutMapping("/{id}")
    public Section updateSection(
        @PathVariable Long id,
        @Valid @RequestBody Section section,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return sectionService.updateSection(id, section, authorizationHeader);
    }

    @PatchMapping("/{id}/enabled")
    public Section updateEnabled(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSectionEnabledRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return sectionService.updateEnabled(id, request.enabled(), authorizationHeader);
    }
}
