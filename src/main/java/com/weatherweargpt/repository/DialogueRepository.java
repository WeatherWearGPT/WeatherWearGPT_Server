package com.weatherweargpt.repository;

import com.weatherweargpt.entity.Dialogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DialogueRepository extends JpaRepository<Dialogue, Long> {
    // User 대신 UserEntity를 사용
    List<Dialogue> findByUserEntityUserIdOrderByConversationDateDesc(Long userId);  // 수정된 부분: findByUserUserId → findByUserEntityUserId

    // 가장 최근의 대화를 가져오는 메서드 (목적지 확인용)
    Dialogue findTopByUserEntityUserIdOrderByConversationDateDesc(Long userId);  // 수정된 부분: findTopByUserUserId → findTopByUserEntityUserId
}
