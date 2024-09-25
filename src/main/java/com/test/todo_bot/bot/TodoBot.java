package com.test.todo_bot.bot;

import com.test.todo_bot.entity.Todo;
import com.test.todo_bot.entity.TodoDto;
import com.test.todo_bot.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodoBot extends TelegramLongPollingBot {

    private final TodoService todoService;

    private final HashMap<Long, String> userState = new HashMap<>();
    private final HashMap<Long, Long> tempTodoId = new HashMap<>();
    private final HashMap<Long, String> tempTitle = new HashMap<>();

    // 상태 초기화 메서드
    private void resetUserState(Long chatId) {
        userState.remove(chatId);
    }

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.bot.token}")
    private String token;

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommand(chatId);  // 봇 시작 시 환영 메시지 및 버튼 표시
                    break;
                case "/add":
                    startAddTodoProcess(chatId);
                    break;
                case "/update":
                    startUpdateTodoProcess(chatId);
                    break;
                case "/delete":
                    startDeleteTodoProcess(chatId);
                    break;
                case "/list":
                    handleListTodos(chatId);
                    break;
                case "/get":
                    startGetTodoProcess(chatId);
                    break;
                default:
                    if (userState.containsKey(chatId)) {
                        handleUserInput(chatId, messageText);
                    } else {
                        sendMessageWithKeyboard(chatId, "명령어를 선택해주세요.", createMainMenu());
                    }
                    break;
            }
        }
    }

    private void startCommand(Long chatId) {
        sendMessageWithKeyboard(chatId, "todo bot에 오신 것을 환영합니다!", createMainMenu());
    }

    private ReplyKeyboardMarkup createMainMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/add"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/update"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("/delete"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("/list"));

        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("/get"));

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);
        keyboardRows.add(row5);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private void sendMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startAddTodoProcess(Long chatId) {
        userState.put(chatId, "AWAITING_TITLE");  // 제목 입력 대기 상태로 설정
        sendMessage(chatId, "할 일의 제목을 입력해주세요:");
    }

    private void handleUserInput(Long chatId, String messageText) {
        String state = userState.get(chatId);

        if ("AWAITING_TITLE".equals(state)) {
            tempTitle.put(chatId, messageText);
            userState.put(chatId, "AWAITING_DESCRIPTION");
            sendMessage(chatId, "할 일의 설명을 입력해주세요:");
        } else if ("AWAITING_DESCRIPTION".equals(state)) {
            String title = tempTitle.get(chatId);
            String description = messageText;

            TodoDto todoDto = todoService.addTodo(title, description);
            sendMessage(chatId, "할 일이 성공적으로 추가되었습니다: " + todoDto.title() + " - " + todoDto.description());

            resetUserState(chatId);
            tempTitle.remove(chatId);
        } else if ("AWAITING_TODO_ID_FOR_GET".equals(state)) {
            handleGetTodoById(chatId, messageText);
        } else if ("AWAITING_TODO_ID_FOR_UPDATE".equals(state)) {
            handleUpdateTodoById(chatId, messageText);  // 수정할 할 일 ID 입력 처리
        } else if ("AWAITING_TODO_ID_FOR_DELETE".equals(state)) {
            handleDeleteTodoById(chatId, messageText);
        } else {
            // 새로운 제목과 설명을 입력받는 상태 처리
            handleUpdatedUserInput(chatId, messageText);
        }
    }

    private void handleGetTodoById(Long chatId, String messageText) {
        Long todoId;
        try {
            // 사용자가 입력한 할 일 ID를 Long 타입으로 변환
            todoId = Long.parseLong(messageText);
            // 할 일을 조회하고 결과를 반환
            TodoDto todo = todoService.getTodoById(todoId);
            // 조회된 할 일 정보를 사용자에게 전송
            sendMessage(chatId, "할 일 조회 결과: " + todo.title() + " - " + todo.description());
        } catch (NumberFormatException e) {
            // 입력한 ID가 숫자가 아닌 경우 처리
            sendMessage(chatId, "ID는 숫자여야 합니다.");
        } catch (IllegalArgumentException e) {
            // 잘못된 ID나 존재하지 않는 ID를 입력했을 때 처리
            sendMessage(chatId, e.getMessage());
        }

        // 사용자 상태 초기화
        resetUserState(chatId);
    }



    private void handleUpdateTodoById(Long chatId, String messageText) {
        Long todoId;
        try {
            todoId = Long.parseLong(messageText);
            tempTodoId.put(chatId, todoId);  // 입력받은 ID를 임시 저장
            userState.put(chatId, "AWAITING_NEW_TITLE");  // 상태를 제목 입력 대기 상태로 변경
            sendMessage(chatId, "수정할 할 일의 새로운 제목을 입력해주세요:");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "ID는 숫자여야 합니다.");
        }
    }



    private void startUpdateTodoProcess(Long chatId) {
        userState.put(chatId, "AWAITING_TODO_ID_FOR_UPDATE");
        sendMessage(chatId, "수정할 할 일의 ID를 입력해주세요:");
    }

    private void startDeleteTodoProcess(Long chatId) {
        userState.put(chatId, "AWAITING_TODO_ID_FOR_DELETE");
        sendMessage(chatId, "삭제할 할 일의 ID를 입력해주세요:");
    }

    private void handleListTodos(Long chatId) {
        List<TodoDto> todos = todoService.getTodoList();  // 할 일 목록 조회

        if (todos.isEmpty()) {
            sendMessage(chatId, "할 일이 없습니다.");
        } else {
            StringBuilder messageText = new StringBuilder("할 일 목록:\n");
            for (TodoDto todo : todos) {
                messageText.append(todo.id()).append(". ")
                        .append(todo.title()).append(" - ")
                        .append(todo.description()).append("\n");
            }
            sendMessage(chatId, messageText.toString());
        }
    }

    private void startGetTodoProcess(Long chatId) {
        userState.put(chatId, "AWAITING_TODO_ID_FOR_GET");
        sendMessage(chatId, "조회할 할 일의 ID를 입력해주세요:");
    }

    private void handleDeleteTodoById(Long chatId, String messageText) {
        Long todoId;
        try {
            todoId = Long.parseLong(messageText);
            todoService.deleteTodo(todoId);  // 삭제 처리
            sendMessage(chatId, "할 일이 성공적으로 삭제되었습니다.");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "ID는 숫자여야 합니다.");
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, e.getMessage());
        }

        resetUserState(chatId);
    }

    private void handleUpdatedUserInput(Long chatId, String messageText) {
        String state = userState.get(chatId);

        if ("AWAITING_NEW_TITLE".equals(state)) {
            tempTitle.put(chatId, messageText);
            userState.put(chatId, "AWAITING_NEW_DESCRIPTION");
            sendMessage(chatId, "새로운 설명을 입력해주세요:");
        } else if ("AWAITING_NEW_DESCRIPTION".equals(state)) {
            Long todoId = tempTodoId.get(chatId);
            String newTitle = tempTitle.get(chatId);
            String newDescription = messageText;

            try {
                TodoDto updatedTodo = todoService.updateTodo(todoId, newTitle, newDescription);  // 수정 기능 처리
                sendMessage(chatId, "할 일이 성공적으로 업데이트되었습니다: " + updatedTodo.title() + " - " + updatedTodo.description());
            } catch (Exception e) {
                sendMessage(chatId, "할 일을 업데이트하는 중 오류가 발생했습니다.");
            }

            resetUserState(chatId);
            tempTodoId.remove(chatId);
            tempTitle.remove(chatId);
        }
    }


    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));  // chatId는 String으로 변환하여 설정
        message.setText(text);

        try {
            execute(message);  // 메시지를 전송
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
