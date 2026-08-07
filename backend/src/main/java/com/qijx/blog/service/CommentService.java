package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Comment;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.CommentRepository;

@Service
public class CommentService {
    private final ArticleService articleService;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;

    public CommentService(CommentRepository commentRepository, ArticleService articleService, CurrentUserService currentUserService){
        this.commentRepository = commentRepository;
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    public Comment createComment(Long articleId, Comment comment, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        articleService.getArticle(articleId);

        LocalDateTime now = LocalDateTime.now();

        comment.setArticleId(articleId);
        comment.setAuthorId(currentUser.getId());
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
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Comment existingComment = getCommentInArticle(articleId, id);

        checkCommentPermission(existingComment, currentUser);

        commentRepository.deleteByIdAndArticleId(articleId, id);
    }

    public Comment updateComment(Long articleId, Long id, Comment comment, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        articleService.getArticle(articleId);
        Comment existingComment = getCommentInArticle(articleId, id);

        checkCommentPermission(existingComment, currentUser);

        getCommentInArticle(articleId, id);

        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.update(articleId, id, comment);

        return getCommentInArticle(articleId, id);
    }

    private void checkCommentPermission(Comment comment, User currentUser){
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        boolean isAuthor = comment.getAuthorId() != null && comment.getAuthorId().equals(currentUser.getId());

        if(!isAdmin && !isAuthor){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to conduct this operation");
        }
    }
}
