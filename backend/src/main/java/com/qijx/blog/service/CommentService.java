package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Comment;
import com.qijx.blog.repository.CommentRepository;
import com.qijx.blog.service.JwtService;

import io.jsonwebtoken.Claims;

@Service
public class CommentService {
    private final ArticleService articleService;
    private final CommentRepository commentRepository;
    private final JwtService jwtService;

    public CommentService(CommentRepository commentRepository, ArticleService articleService, JwtService jwtService){
        this.commentRepository = commentRepository;
        this.articleService = articleService;
        this.jwtService = jwtService;
    }

    public Comment createComment(Long articleId, Comment comment, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        Long userId = claims.get("userId", Long.class);

        articleService.getArticle(articleId);

        LocalDateTime now = LocalDateTime.now();

        comment.setArticleId(articleId);
        comment.setAuthorId(userId);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        return commentRepository.save(articleId, comment);
    }

    public List<Comment> listCommentsByArticleId(Long articleId){
        articleService.getArticle(articleId);
        return commentRepository.findByArticleId(articleId);
    }

    public Comment getCommentInArticle(Long articleId, Long id){
        return commentRepository.findByIdAndArticleId(articleId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    public void deleteComment(Long articleId, Long id, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        Comment existingComment = getCommentInArticle(articleId, id);

        checkCommentPermission(existingComment, claims);

        commentRepository.deleteByIdAndArticleId(articleId, id);
    }

    public Comment updateComment(Long articleId, Long id, Comment comment, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        articleService.getArticle(articleId);
        Comment existingComment = getCommentInArticle(articleId, id);

        checkCommentPermission(existingComment, claims);

        getCommentInArticle(articleId, id);

        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.update(articleId, id, comment);

        return getCommentInArticle(articleId, id);
    }

    private void checkCommentPermission(Comment comment, Claims claims){
        Long currentUserId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        boolean isAdmin = "ADMIN".equals(role);

        boolean isAuthor = comment.getAuthorId() != null && comment.getAuthorId().equals(currentUserId);

        if(!isAdmin && !isAuthor){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to conduct this operation");
        }
    }
}
