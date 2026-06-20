package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.service.CommentService;
import com.qijx.blog.entity.Comment;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")

public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }
    
    @PostMapping
    public Comment createComment(@PathVariable Long articleId, @Valid @RequestBody Comment comment){
        return commentService.createComment(articleId, comment);
    }

    @GetMapping
    public List<Comment> listCommentsByArticleId(@PathVariable Long articleId){
        return commentService.listCommentsByArticleId(articleId);
    }

    @PutMapping("/{id}")
    public Comment updateComment(@PathVariable Long articleId, @PathVariable Long id, @Valid @RequestBody Comment comment){
        return commentService.updateComment(articleId, id, comment);
    }

    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Long articleId, @PathVariable Long id){
        commentService.deleteComment(articleId, id);
    }
}
