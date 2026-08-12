package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.entity.KnowledgeCard;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.KnowledgeCardRepository;

@Service
public class KnowledgeCardService {
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final CurrentUserService currentUserService;

    public KnowledgeCardService(
        KnowledgeCardRepository knowledgeCardRepository,
        CurrentUserService currentUserService
    ){
        this.knowledgeCardRepository = knowledgeCardRepository;
        this.currentUserService = currentUserService;
    }

    public List<KnowledgeCard> listKnowledgeCards(String keyword){
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return knowledgeCardRepository.findAll(normalizedKeyword);
    }

    public KnowledgeCard getKnowledgeCard(Long id){
        return knowledgeCardRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge card not found"));
    }

    public KnowledgeCard createKnowledgeCard(KnowledgeCard knowledgeCard, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        LocalDateTime now = LocalDateTime.now();

        normalize(knowledgeCard);
        knowledgeCard.setCreatedBy(currentUser.getId());
        knowledgeCard.setCreatedAt(now);
        knowledgeCard.setUpdatedAt(now);

        KnowledgeCard savedCard = knowledgeCardRepository.save(knowledgeCard);
        return getKnowledgeCard(savedCard.getId());
    }

    public KnowledgeCard updateKnowledgeCard(
        Long id,
        KnowledgeCard knowledgeCard,
        String authorizationHeader
    ){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        KnowledgeCard existingCard = getKnowledgeCard(id);
        checkPermission(existingCard, currentUser);

        normalize(knowledgeCard);
        knowledgeCard.setUpdatedAt(LocalDateTime.now());
        knowledgeCardRepository.update(id, knowledgeCard);
        return getKnowledgeCard(id);
    }

    public void deleteKnowledgeCard(Long id, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        KnowledgeCard existingCard = getKnowledgeCard(id);
        checkPermission(existingCard, currentUser);
        knowledgeCardRepository.deleteById(id);
    }

    private void checkPermission(KnowledgeCard knowledgeCard, User currentUser){
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isCreator = knowledgeCard.getCreatedBy().equals(currentUser.getId());

        if(!isAdmin && !isCreator){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to operate this knowledge card");
        }
    }

    private void normalize(KnowledgeCard knowledgeCard){
        knowledgeCard.setTitle(knowledgeCard.getTitle().trim());
        knowledgeCard.setSummary(knowledgeCard.getSummary().trim());
        knowledgeCard.setContent(knowledgeCard.getContent().trim());
    }
}
