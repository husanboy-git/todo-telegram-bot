package com.test.todo_bot.entity;

public record TodoDto(
        Long id,
        String title,
        String description
) {
    public static TodoDto toDto(Todo todo) {
        return new TodoDto(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription()
        );
    }
}
