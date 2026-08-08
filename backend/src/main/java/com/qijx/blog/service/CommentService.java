package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Comment;
import com.qijx.blog.dto.InteractionStatusResponse;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.CommentRepository;
import com.qijx.blog.repository.CommentLikeRepository;

@Service
public class CommentService {
    private final ArticleService articleService;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CurrentUserService currentUserService;

    public CommentService(
        CommentRepository commentRepository,
        CommentLikeRepository commentLikeRepository,
        ArticleService articleService,
        CurrentUserService currentUserService
    ){
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    public Comment createComment(Long articleId, Comment comment, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        articleService.getArticle(articleId);

        LocalDateTime now = LocalDateTime.now();

        comment.setArticleId(articleId);
        comment.setParentId(null);
        comment.setAuthorId(currentUser.getId());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        return commentRepository.save(articleId, comment);
    }

    public Comment createReply(Long articleId, Long parentId, Comment comment, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        articleService.getArticle(articleId);
        getCommentInArticle(articleId, parentId);

        LocalDateTime now = LocalDateTime.now();
        comment.setArticleId(articleId);
        comment.setParentId(parentId);
        comment.setAuthorId(currentUser.getId());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        Comment savedComment = commentRepository.save(articleId, comment);
        return getCommentInArticle(articleId, savedComment.getId());
    }

    public InteractionStatusResponse getLikeStatus(Long articleId, Long id, String authorizationHeader){
        Comment comment = getCommentInArticle(articleId, id);
        boolean liked = false;

        if(authorizationHeader != null && !authorizationHeader.isBlank()){
            User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
            liked = commentLikeRepository.hasLiked(comment.getId(), currentUser.getId());
        }

        return new InteractionStatusResponse(commentLikeRepository.count(comment.getId()), liked);
    }

    public InteractionStatusResponse likeComment(Long articleId, Long id, String authorizationHeader){
        Comment comment = getCommentInArticle(articleId, id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        commentLikeRepository.add(comment.getId(), currentUser.getId(), LocalDateTime.now());
        return new InteractionStatusResponse(commentLikeRepository.count(comment.getId()), true);
    }

    public InteractionStatusResponse unlikeComment(Long articleId, Long id, String authorizationHeader){
        Comment comment = getCommentInArticle(articleId, id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        commentLikeRepository.remove(comment.getId(), currentUser.getId());
        return new InteractionStatusResponse(commentLikeRepository.count(comment.getId()), false);
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
