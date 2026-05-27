package com.qijx.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.service.CommentService;

import jakarta.validation.Valid;

import com.qijx.blog.entity.Comment;

@RestController
@RequestMapping("/api/contents")

public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }
    
    @PostMapping
    public Comment createContent(@Valid @RequestBody Comment comment){
        return commentService.createComment(comment);
    }

    
}
