package com.test.todo_bot.bot;

import lombok.Getter;
import java.util.Optional;

@Getter
public class UserState {
    // 상태 정보 가져오기 및 설정
    private String currentStep;
    private String tempTitle;
    private Long tempTodoId;

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Optional<String> getTempTitle() {
        return Optional.ofNullable(tempTitle);
    }

    public void setTempTitle(String tempTitle) {
        this.tempTitle = tempTitle;
    }

    public Optional<Long> getTempTodoId() {
        return Optional.ofNullable(tempTodoId);
    }

    public void setTempTodoId(Long tempTodoId) {
        this.tempTodoId = tempTodoId;
    }
}
