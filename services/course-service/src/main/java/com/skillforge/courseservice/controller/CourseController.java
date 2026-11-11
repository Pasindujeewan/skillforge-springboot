package com.skillforge.courseservice.controller;

import com.skillforge.courseservice.dto.CourseCreateRequest;
import com.skillforge.courseservice.dto.CourseResponse;
import com.skillforge.courseservice.security.CurrentUserService;
import com.skillforge.courseservice.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseService.createCourse(
                request,
                currentUserService.getCurrentUser(authentication)
        );
    }
}
