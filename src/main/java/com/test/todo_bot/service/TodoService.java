package com.test.todo_bot.service;

import com.test.todo_bot.entity.Todo;
import com.test.todo_bot.entity.TodoDto;
import com.test.todo_bot.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;

    public List<TodoDto> getTodoList() {
        List<Todo> todos = todoRepository.findAll();
        return todos.stream().map(TodoDto::toDto).toList();
    }

    public TodoDto getTodoById(Long todoId) {
        Todo todo = getTodoEntityById(todoId);
        return TodoDto.toDto(todo);
    }

    public TodoDto addTodo(String title, String description) {
        return TodoDto.toDto(todoRepository.save(Todo.of(title, description)));
    }

    public TodoDto updateTodo(Long id, String title, String description) {
        Todo todo = getTodoEntityById(id);
        todo.setTitle(title);
        todo.setDescription(description);
        return TodoDto.toDto(todoRepository.save(todo));
    }

    public void deleteTodo(Long id) {
        Todo todo = getTodoEntityById(id);
        todoRepository.delete(todo);
    }

    private Todo getTodoEntityById(Long id) {
        return todoRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("할 일을 찾을 수 없습니다. ID: " + id));
    }
}
