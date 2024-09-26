package com.weatherweargpt.dto;

import com.weatherweargpt.entity.Dialogue;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DialogueDTO {
    private Long dialogId;
    private Long userId;
    private Integer sequenceNumber;
    private String questionAsked;
    private String userResponse;
    private String destination;
    private LocalDateTime conversationDate;

    public DialogueDTO(Long dialogId, Long userId, Integer sequenceNumber,
                       String questionAsked, String userResponse,
                       String destination, LocalDateTime conversationDate) {
        this.dialogId = dialogId;
        this.userId = userId;
        this.sequenceNumber = sequenceNumber;
        this.questionAsked = questionAsked;
        this.userResponse = userResponse;
        this.destination = destination;
        this.conversationDate = conversationDate;
    }

    // Entity를 DTO로 변환하는 정적 메서드
    public static DialogueDTO fromEntity(Dialogue dialogue) {
        return new DialogueDTO(
                dialogue.getDialogId(),
                dialogue.getUserEntity() != null ? dialogue.getUserEntity().getUserId() : null,  // 수정된 부분: getUser()에서 getUserEntity()로 변경
                dialogue.getSequenceNumber(),
                dialogue.getQuestionAsked(),
                dialogue.getUserResponse(),
                dialogue.getDestination(),
                dialogue.getConversationDate()
        );
    }
}
