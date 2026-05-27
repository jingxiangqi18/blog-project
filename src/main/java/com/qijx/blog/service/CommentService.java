package com.qijx.blog.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.qijx.blog.entity.Comment;
import com.qijx.blog.repository.CommentRepository;

@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository){
        this.commentRepository = commentRepository;
    }

    public Comment createComment(Comment comment){
        LocalDateTime now = LocalDateTime.now();

        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        return commentRepository.save(comment);
    }
}
