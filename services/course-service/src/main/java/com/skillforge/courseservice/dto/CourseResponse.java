package com.skillforge.courseservice.dto;

import com.skillforge.courseservice.entity.Course;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        Long instructorId,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getInstructorId(),
                course.getTitle(),
                course.getDescription(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
