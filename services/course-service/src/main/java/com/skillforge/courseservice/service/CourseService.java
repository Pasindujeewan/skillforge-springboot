package com.skillforge.courseservice.service;

import com.skillforge.courseservice.dto.CourseCreateRequest;
import com.skillforge.courseservice.dto.CourseResponse;
import com.skillforge.courseservice.entity.Course;
import com.skillforge.courseservice.repository.CourseRepository;
import com.skillforge.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse createCourse(
            CourseCreateRequest request,
            AuthenticatedUser instructor
    ) {
        Course course = new Course(
                instructor.userId(),
                request.title().trim(),
                request.description().trim()
        );

        return CourseResponse.from(courseRepository.save(course));
    }
}
