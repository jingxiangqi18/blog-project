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

import com.qijx.blog.entity.KnowledgeCard;
import com.qijx.blog.service.KnowledgeCardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/knowledge-cards")
public class KnowledgeCardController {
    private final KnowledgeCardService knowledgeCardService;

    public KnowledgeCardController(KnowledgeCardService knowledgeCardService){
        this.knowledgeCardService = knowledgeCardService;
    }

    @GetMapping
    public List<KnowledgeCard> listKnowledgeCards(
        @RequestParam(required = false) String keyword
    ){
        return knowledgeCardService.listKnowledgeCards(keyword);
    }

    @GetMapping("/{id}")
    public KnowledgeCard getKnowledgeCard(@PathVariable Long id){
        return knowledgeCardService.getKnowledgeCard(id);
    }

    @PostMapping
    public KnowledgeCard createKnowledgeCard(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody KnowledgeCard knowledgeCard
    ){
        return knowledgeCardService.createKnowledgeCard(knowledgeCard, authorizationHeader);
    }

    @PutMapping("/{id}")
    public KnowledgeCard updateKnowledgeCard(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody KnowledgeCard knowledgeCard
    ){
        return knowledgeCardService.updateKnowledgeCard(id, knowledgeCard, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteKnowledgeCard(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        knowledgeCardService.deleteKnowledgeCard(id, authorizationHeader);
    }
}
