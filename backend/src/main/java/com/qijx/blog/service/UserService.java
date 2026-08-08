package com.qijx.blog.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Comment;
import com.qijx.blog.entity.Role;
import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.repository.ArticleRepository;
import com.qijx.blog.repository.CommentRepository;
import com.qijx.blog.dto.UpdateUserEnabledRequest;
import com.qijx.blog.dto.UserResponse;
import com.qijx.blog.dto.ChangePasswordRequest;
import com.qijx.blog.entity.User;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
        UserRepository userRepository,
        ArticleRepository articleRepository,
        CommentRepository commentRepository,
        CurrentUserService currentUserService
    ){
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.currentUserService = currentUserService;
    }

    public List<UserResponse> listUsers(String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        if(currentUser.getRole() != Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can view users");
        }

        return userRepository.findAll().stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    user.isEnabled()
                ))
                .toList();
    }

    public UserResponse updateUserEnabled(Long id, String authorizationHeader, UpdateUserEnabledRequest request){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        if(currentUser.getRole() != Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can update users");
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if(targetUser.getId().equals(currentUser.getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot disable yourself");
        }

        if(targetUser.getRole() == Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot update another admin");
        }
        
        userRepository.updateEnabled(id, request.getEnabled());

        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        return new UserResponse(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole(), updatedUser.isEnabled());
    }

    public void changeMyPassword(String authorizationHeader, ChangePasswordRequest request){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());

        userRepository.updatePasswordHash(user.getId(), newPasswordHash);
    }

    public List<Article> listMyArticles(String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        return articleRepository.findByAuthorId(currentUser.getId());
    }

    public List<Comment> listMyComments(String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        return commentRepository.findByAuthorId(currentUser.getId());
    }

    public List<Article> listMyFavoriteArticles(String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        return articleRepository.findFavoritedByUserId(currentUser.getId());
    }
}
