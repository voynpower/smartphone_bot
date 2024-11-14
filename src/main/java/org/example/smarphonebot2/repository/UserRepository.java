package org.example.smarphonebot2.repository;

import org.example.smarphonebot2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByChatId(Long chatId);

    long countByIsSubscribedTrue();

    long countByIsSubscribedFalse();

    // Adminlarni sanash
    long countByIsAdminTrue();


}

