package com.weatherweargpt.repository;

import com.weatherweargpt.entity.UserEntity;  // 수정된 부분: User → UserEntity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {  // 수정된 부분: User → UserEntity
    UserEntity findByUserName(String userName);  // 수정된 부분: User → UserEntity, 필드명 변경 없음
    Boolean existsByUserName(String userName);  // 수정된 부분: existsByUsername → existsByUserName
}