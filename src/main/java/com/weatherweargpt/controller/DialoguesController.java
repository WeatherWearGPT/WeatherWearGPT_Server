package com.weatherweargpt.controller;

import com.weatherweargpt.entity.Dialogue;
import com.weatherweargpt.service.DialogueService;
import com.weatherweargpt.service.GPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gpt/dialogues")
public class DialoguesController {

    private final DialogueService dialogueService;
    private final GPTService gptService;

    @Autowired
    public DialoguesController(DialogueService dialogueService, GPTService gptService) {
        this.dialogueService = dialogueService;
        this.gptService = gptService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Dialogue> saveDialogue(@RequestBody Dialogue dialogue, @PathVariable Long userId) {
        Dialogue savedDialogue = dialogueService.saveDialogue(dialogue, userId);
        return ResponseEntity.ok(savedDialogue);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Dialogue>> getDialogues(@PathVariable Long userId) {
        List<Dialogue> dialogues = dialogueService.getDialoguesByUserId(userId);
        return ResponseEntity.ok(dialogues);
    }

    @DeleteMapping("/{dialogId}")
    public ResponseEntity<Void> deleteDialogue(@PathVariable Long dialogId) {
        dialogueService.deleteDialogue(dialogId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/start/{userId}")
    public ResponseEntity<Dialogue> startNewChat(@PathVariable Long userId) {
        String initialQuestion = gptService.generateInitialQuestion();
        Dialogue newDialogue = new Dialogue();
        newDialogue.setQuestionAsked(initialQuestion);  // question 필드 대신 사용
        newDialogue.setUserResponse(null);
        Dialogue savedDialogue = dialogueService.saveDialogue(newDialogue, userId);
        return ResponseEntity.ok(savedDialogue);
    }

    @PostMapping("/respond/{userId}")
    public ResponseEntity<Dialogue> handleUserResponse(@PathVariable Long userId, @RequestBody String userResponse) {
        // 이전 대화 업데이트
        List<Dialogue> previousDialogues = dialogueService.getDialoguesByUserId(userId);
        if (!previousDialogues.isEmpty()) {
            Dialogue previousDialogue = previousDialogues.get(0);
            previousDialogue.setUserResponse(userResponse);
            dialogueService.saveDialogue(previousDialogue, userId);
        }

        // 새 대화 생성
        String nextQuestion = gptService.processUserResponse(userId, userResponse);
        Dialogue newDialogue = new Dialogue();
        newDialogue.setQuestionAsked(nextQuestion);  // question 필드 대신 사용
        newDialogue.setUserResponse(null);
        Dialogue savedDialogue = dialogueService.saveDialogue(newDialogue, userId);
        return ResponseEntity.ok(savedDialogue);
    }
}
