package com.weatherweargpt.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Dialogue")
@Data
public class Dialogue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DialogId")  // Primary Key (PK)
    private Long dialogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", referencedColumnName = "UserId", foreignKey = @ForeignKey(name = "FK_User_Dialogue"))  // Foreign Key (FK)
    @JsonBackReference  // 순환 참조 방지
    private UserEntity userEntity;  // 수정된 부분: User에서 UserEntity로 변경

    @Column(name = "sequence_number")
    private Integer sequenceNumber;  // 대화 순서

    @Column(name = "question_asked", nullable = false, columnDefinition = "LONGTEXT")
    private String questionAsked;  // GPT가 한 질문 (더 긴 데이터 타입 사용)

    @Column(name = "user_response", nullable = true, columnDefinition = "LONGTEXT")
    private String userResponse;  // 사용자의 응답 (더 긴 데이터 타입 사용)

    @Column(name = "Destination")
    private String destination;  // 추가된 목적지

    @Column(name = "conversation_date")
    private LocalDateTime conversationDate;  // 대화 날짜
}
