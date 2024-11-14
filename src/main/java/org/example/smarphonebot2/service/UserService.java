package org.example.smarphonebot2.service;

import lombok.Getter;
import org.example.smarphonebot2.entity.User;
import org.example.smarphonebot2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Adminlar sonini saqlash
    @Getter
    private long adminCount;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;

        this.adminCount = userRepository.countByIsAdminTrue();
    }


    public long getSubscribedUserCount() {
        return userRepository.countByIsSubscribedTrue();
    }

    public long getUnsubscribedUserCount() {
        return userRepository.countByIsSubscribedFalse();
    }

    // Foydalanuvchini chatId bo'yicha olish
    public Optional<User> getUserByChatId(Long chatId) {
        return Optional.ofNullable(userRepository.findByChatId(chatId));
    }

    public boolean isAdmin(Long chatId) {
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByChatId(chatId));

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.isAdmin();
        }

        return false;  // Agar user topilmasa, false qaytaradi
    }


    public void makeAdmin(Long chatId) {
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByChatId(chatId));
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAdmin(true); // Admin qilib qo'yamiz
            userRepository.save(user); // O'zgarishlarni saqlaymiz
        }
    }

    public void revokeAdmin(Long chatId) {
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByChatId(chatId));
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAdmin(false); // Admin huquqini olib tashlaymiz
            userRepository.save(user); // O'zgarishlarni saqlaymiz
        }
    }



    // Adminlar sonini olish
    public long getAdminCount() {
        return userRepository.countByIsAdminTrue();
    }


    // Obunani tasdiqlash
    public void subscribeUser(Long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user != null) {
            user.setSubscribed(true); // Obunani tasdiqlash
            userRepository.save(user);
        }
    }

    // Obunani bekor qilish
    public void unsubscribeUser(Long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user != null) {
            user.setSubscribed(false); // Obunani bekor qilish
            userRepository.save(user);
        }
    }
}