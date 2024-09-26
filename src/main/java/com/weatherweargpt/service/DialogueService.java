package com.weatherweargpt.service;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.entity.UserEntity;  // 수정된 부분: User → UserEntity
import com.weatherweargpt.repository.DialogueRepository;
import com.weatherweargpt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DialogueService {

    private final DialogueRepository dialogueRepository;
    private final UserRepository userRepository;

    @Autowired
    public DialogueService(DialogueRepository dialogueRepository, UserRepository userRepository) {
        this.dialogueRepository = dialogueRepository;
        this.userRepository = userRepository;
    }

    public Dialogue saveDialogue(Dialogue dialogue, Long userId) {
        UserEntity user = userRepository.findById(userId)  // 수정된 부분: User → UserEntity
                .orElseThrow(() -> new RuntimeException("User not found"));
        dialogue.setUserEntity(user);  // 수정된 부분: setUser()에서 setUserEntity()로 변경 필요
        dialogue.setConversationDate(LocalDateTime.now());

        // 시퀀스 넘버를 처음에 0으로 설정
        List<Dialogue> existingDialogues = getDialoguesByUserId(userId);
        int sequenceNumber = existingDialogues.isEmpty() ? 0 : existingDialogues.get(0).getSequenceNumber();

        if (dialogue.getUserResponse() != null) {
            // GPT와 사용자의 한 턴이 끝나면 시퀀스 넘버를 1씩 증가
            sequenceNumber++;
        }

        dialogue.setSequenceNumber(sequenceNumber);

        return dialogueRepository.save(dialogue);
    }

    public List<Dialogue> getDialoguesByUserId(Long userId) {
        return dialogueRepository.findByUserEntityUserIdOrderByConversationDateDesc(userId);  // 수정된 부분: findByUserUserId → findByUserEntityUserId
    }

    public void deleteDialogue(Long dialogId) {
        dialogueRepository.deleteById(dialogId);
    }
}
