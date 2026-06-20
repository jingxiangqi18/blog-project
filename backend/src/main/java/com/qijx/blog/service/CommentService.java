package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Comment;
import com.qijx.blog.repository.CommentRepository;

@Service
public class CommentService {
    private final ArticleService articleService;
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository, ArticleService articleService){
        this.commentRepository = commentRepository;
        this.articleService = articleService;
    }

    public Comment createComment(Long articleId, Comment comment){
        articleService.getArticle(articleId);

        LocalDateTime now = LocalDateTime.now();

        comment.setArticleId(articleId);
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

    public void deleteComment(Long articleId, Long id){
        
        getCommentInArticle(articleId, id);

        commentRepository.deleteByIdAndArticleId(articleId, id);
    }

    public Comment updateComment(Long articleId, Long id, Comment comment){
        articleService.getArticle(articleId);
        getCommentInArticle(articleId, id);



        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.update(articleId, id, comment);

        return getCommentInArticle(articleId, id);
    }
}
