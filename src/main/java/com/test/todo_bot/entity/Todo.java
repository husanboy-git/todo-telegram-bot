package com.test.todo_bot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "todos")
@Getter
@Setter
@RequiredArgsConstructor
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    public static Todo of(String title, String description) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDescription(description);
        return todo;
    }
}
