package org.example.smarphonebot2.controller;

import org.apache.commons.io.FileUtils;
import org.example.smarphonebot2.entity.Model;
import org.example.smarphonebot2.service.ProductService;
import org.example.smarphonebot2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Component
public class SmartPhoneBot2 extends TelegramLongPollingBot {

    private final ProductService productService;
    private final UserService userService;

    Map<Long, String> adminSteps = new HashMap<>();
    Map<Long, Long> adminTargetChatIds = new HashMap<>();
    Map<Long, String> addModelSteps = new HashMap<>();
    Map<Long, String> addModelNames = new HashMap<>();
    Map<Long, Double> addModelPrice = new HashMap<>();
    Map<Long, String> addModelDescription = new HashMap<>();
    Map<Long, String> addModelImages = new HashMap<>();
    Map<Long, String> updateModelSteps = new HashMap<>();
    Map<Long, Long> updateModelIds = new HashMap<>();


    @Autowired
    public SmartPhoneBot2(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @Value("${telegram.bot.token}")
    private String BOT_TOKEN;

    @Value("${telegram.bot.username}")
    private String BOT_NAME;

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

   public static String correctPassword = "1995hakimov19"; // Parol har bir joyda mavjud

    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // Rasm mavjudligini tekshirish
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            if (largestPhoto != null) {
                try {
                    // Faylni saqlash
                    String saveFilePath = saveFileToFolder(largestPhoto.getFileId(), "model_image.png");
                    // returnPhotoToUser(update.getMessage().getChatId(), saveFilePath);  // Foydalanuvchiga rasmni qaytarish

                    // Mahsulotni qo'shish uchun fayl manzilini saqlash
                    addModelImages.put(update.getMessage().getChatId(), saveFilePath);
                    sendMessage(update.getMessage().getChatId(), "Biror text yuboring!");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    sendMessage(update.getMessage().getChatId(), "Rasmni yuklashda xatolik yuz berdi.");
                }
            }
        }
        else if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getFrom().getUserName();

            if (messageText.equals("/start")) {
                userService.subscribeUser(chatId);
                sendMessage(chatId, "Assalomu Aleykum SmartPhone Botimizga xush kelibsiz \n\n" +
                        "O'zingiz yoqtirgan brendlardan birini tanlang!");
                sendMenuBrends(chatId, "Tanlang!");
            }
            else {
                userService.unsubscribeUser(chatId);
            }

            if (messageText.equals("/contact_admin")) {
                sendAdminLink(chatId);
            }

            if (messageText.equals("/statistika")) {
                long activeSubscribedCount = userService.getSubscribedUserCount();
                long unsubscribedCount = userService.getUnsubscribedUserCount();
                long adminCount = userService.getAdminCount();

                sendMessage(chatId, "\uD83D\uDC6B Faol obunachilar soni: " + activeSubscribedCount +
                        "\n\uD83D\uDE45\u200D♀\uFE0F Obunadan chiqqan foydalanuvchilar: " + unsubscribedCount +
                        "\n\uD83D\uDC68\u200D\uD83D\uDCBB Adminlar soni: " + adminCount);
            }
            else if (messageText.equals("/my_channel")) {
                sendChannelLink(chatId);
            }

            else if (messageText.startsWith("/admin")) {
                // Admin qilish jarayonini boshlash
                if (userService.isAdmin(chatId)) {
                    sendMessage(chatId, "Siz allaqachon adminsiz.");
                } else {
                    sendMessage(chatId, "Iltimos, admin bo'lish uchun parolni kiriting:");
                    adminSteps.put(chatId, "awaiting_password"); // Parolni kutish jarayonini boshlash
                }
            }
            // Admin qilish jarayonida ekanligini tekshiramiz
            else if (adminSteps.containsKey(chatId)) {
                String currentStep = adminSteps.get(chatId);
                switch (currentStep) {
                    case "awaiting_password":
                        // Foydalanuvchidan parolni tekshirish
                        if (messageText.equals(correctPassword)) {
                            // Foydalanuvchini admin qilish
                            userService.makeAdmin(chatId); // Foydalanuvchini admin sifatida belgilash
                            sendMessage(chatId, "Siz endi adminsiz!");
                        } else {
                            sendMessage(chatId, "Noto'g'ri parol. Admin bo'lish jarayoni tugatildi.");
                        }
                        // Jarayonni tugatish
                        adminSteps.remove(chatId);
                        break;

                    case "awaiting_revoke_password":
                        // Foydalanuvchidan parolni tekshirish
                        if (messageText.equals(correctPassword)) {
                            // Foydalanuvchini adminlikdan chiqarish
                            userService.revokeAdmin(chatId); // Hozirgi foydalanuvchini adminlikdan chiqaramiz
                            sendMessage(chatId, "Siz adminlikdan chiqarildingiz.");
                        } else {
                            sendMessage(chatId, "Noto'g'ri parol. Jarayon tugatildi.");
                        }
                        // Jarayonni tugatish
                        adminSteps.remove(chatId); // Jarayonni tugatamiz
                        break;

                    default:
                        // Agar qadam to'g'ri kelmasa, foydalanuvchini xabardor qilish
                        sendMessage(chatId, "Jarayonni boshqarishda xato yuz berdi. Iltimos, qaytadan urinib ko'ring.");
                        adminSteps.remove(chatId);
                        break;
                }
            }
            else if (messageText.startsWith("/r_admin")) {
                // Adminlikdan chiqarish jarayonini boshlash
                if (userService.isAdmin(chatId)) {
                    sendMessage(chatId, "Iltimos, adminlikdan chiqish uchun parolni kiriting:");
                    adminSteps.put(chatId, "awaiting_revoke_password"); // Parol kiritishni kutish jarayoni
                } else {
                    sendMessage(chatId, "Faqat adminlar o'zlarini adminlikdan chiqarishi mumkin.");
                }
            } else if (adminSteps.containsKey(chatId)) {
                String currentStep = adminSteps.get(chatId);
                switch (currentStep) {
                    case "awaiting_revoke_password":
                        // Foydalanuvchidan parolni tekshirish
                        if (messageText.equals(correctPassword)) {
                            // Foydalanuvchini adminlikdan chiqarish
                            userService.revokeAdmin(chatId); // Hozirgi foydalanuvchini adminlikdan chiqaramiz
                            sendMessage(chatId, "Siz adminlikdan chiqarildingiz.");
                        } else {
                            sendMessage(chatId, "Noto'g'ri parol. Jarayon tugatildi.");
                        }
                        // Jarayonni tugatish
                        adminSteps.remove(chatId); // Jarayonni tugatamiz
                        break;
                }
            }


            else if (messageText.equals("\uD83D\uDCF1  Samsung")) {
                sendMenuBrendModelsSamsung(chatId, "Modellardan birini tanlang!");
            }
            else if (messageText.equals("\uD83D\uDCF1  Iphone")) {
                sendMenuBrendModelsIphone(chatId, "Modellardan birini tanlang!");
            }
            else if (messageText.equals("\uD83D\uDCF1  Harxil SmartPhonelar")) {
                sendMenuBrendModelsOthers(chatId, "Modellardan birini tanlang!");
            }

            else if (messageText.equals("/samsung_model")) {
                if (userService.isAdmin(chatId)) {
                    sendMessage(chatId, "Model nomini kiriting:");
                    addModelSteps.put(chatId, "name");
                }
                else {
                    sendMessage(chatId, "Faqat adminlar model qo'shish imkoniyatiga egalar!!");
                }
            }
            else if (messageText.equals("/iphone_model")) {
                if (userService.isAdmin(chatId)) {
                    sendMessage(chatId, "Model nomini kiriting:");
                    addModelSteps.put(chatId, "name");
                }
                else {
                    sendMessage(chatId, "Faqat adminlar model qo'shish imkoniyatiga egalar!!");
                }
            }
            else if (messageText.equals("/harxil_model")) {
                if (userService.isAdmin(chatId)) {
                    sendMessage(chatId, "Model nomini kiriting:");
                    addModelSteps.put(chatId, "name");
                }
                else {
                    sendMessage(chatId, "Faqat adminlar model qo'shish imkoniyatiga egalar!!");
                }
            }

            else if (addModelSteps.containsKey(chatId)) {
                String currentStep = addModelSteps.get(chatId);
                switch (currentStep) {
                    case "name":
                        addModelNames.put(chatId, messageText);
                        sendMessage(chatId, "Model narxini kiriting:");
                        addModelSteps.put(chatId, "price");
                        break;

                    case "price":
                        try {
                            double modelPrice = Double.parseDouble(messageText);
                            addModelPrice.put(chatId, modelPrice);
                            sendMessage(chatId, "Modelga tarif kiriting:");
                            addModelSteps.put(chatId, "description");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Iltimos to'g'ri narxni kiriting!!");
                        }
                        break;

                    case "description":
                        addModelDescription.put(chatId, messageText);
                        sendMessage(chatId, "Model Rasmni kiriting:");
                        addModelSteps.put(chatId, "image");
                        break;

                    case "image":
                        // Rasm tekshiruvi tepadan amalga oshirilgan
                        if (!addModelImages.containsKey(chatId)) {
                            sendMessage(chatId, "Iltimos, rasm yuboring.");
                        } else {
                            String modelName = addModelNames.get(chatId);
                            double modelPrice = addModelPrice.get(chatId);
                            String modelDescription = addModelDescription.get(chatId);
                            String modelImagePath = addModelImages.get(chatId);

                            // Modelni qo'shish
                            productService.addModel(modelName, modelPrice, modelDescription, modelImagePath);

                            // Inline tugmalarni yaratish
                            List<InlineKeyboardButton> buttons = Arrays.asList(
                                    InlineKeyboardButton.builder()
                                            .text("\uD83D\uDDD1 Delete")
                                            .callbackData("delete_") // Mahsulotni ko'rish uchun callback
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text("\uD83D\uDD04 Update")
                                            .callbackData("update_")
                                            .build()
                            );



                            // Inline tugmalarni joylashtirish
                            InlineKeyboardMarkup inlineKeyboard = InlineKeyboardMarkup.builder()
                                    .keyboard(Arrays.asList(buttons))
                                    .build();

                            // Rasmi va ma'lumotlarni yuborish
                            sendPhoto(chatId, modelImagePath, " \uD83C\uDF89 --TABRIKLAMIZ--  \uD83D\uDC4F MAHSULOT QO'SHILDI  \uD83C\uDF89 \n\n" +
                                    "\uD83D\uDCF1  Nomi: " + modelName + "\n" +
                                    "\uD83D\uDCB0  Narxi: " + modelPrice + "\n" +
                                    "✍\uFE0F Tasnifi: " + modelDescription, inlineKeyboard);  // Inline tugmalarni qo'shish

                            // Tozalash
                            addModelSteps.remove(chatId);
                            addModelNames.remove(chatId);
                            addModelPrice.remove(chatId);
                            addModelDescription.remove(chatId);
                            addModelImages.remove(chatId);
                            updateModelIds.remove(chatId);
                        }
                        break;


                }
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("model_")) {
                String modelName = callbackData.substring(6); // "model_"ni olib taashlab, model(tugma) nomini kritamiz.
                sendModelListByName(chatId, modelName);
            }
            else if (callbackData.startsWith("del_")) {
                // Model Idsini olish
                String modelName = callbackData.substring(4);
                long modelId = Long.parseLong(modelName);

                // Modelni o'chirish
                productService.deleteModelById(modelId);

                long messageId = update.getCallbackQuery().getMessage().getMessageId();

                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId((int) messageId);

                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                SendMessage sendMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("Mahsulot muvaffaqiyatli o'chirildi.")
                        .build();

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (callbackData.startsWith("upd_")) {
                // Model nomini yangilash jarayonini boshlash (birincha qadam)
                String modelName = callbackData.substring(4);
                long modelId = Long.parseLong(modelName);


                Model model = productService.getModelById(modelId);

                //long messageId = update.getCallbackQuery().getMessage().getMessageId();

                SendMessage sendMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("Siz " + model.getName() + "Mahsulotni yanglilashni tanladingiz. \n\n" +
                                "Iltimos, Mahsulotning yangi nomini kiriting: ")
                        .build();

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                /*updateModelSteps.put(chatId, "name");
                updateModelIds.put(chatId, modelId);*/

                addModelSteps.put(chatId, "name");
                updateModelIds.put(chatId, modelId);

                productService.deleteModelById(modelId);
            }
            else if (callbackData.startsWith("order_")) {
                String modelName = callbackData.substring(6); // "order_" ni olib tashlab, model(tugma) nomini olamiz
                long modelId = Long.parseLong(modelName);

                // Test rejimida to'lovni amalga oshirish
                boolean isTestMode = true; // Test rejimini belgilash

                if (isTestMode) {
                    // Test rejimida muvaffaqiyatli buyurtma berilgani haqida xabar yuborish
                    SendMessage sendMessage = SendMessage.builder()
                            .chatId(chatId)
                            .text("Test rejimida buyurtma muvaffaqiyatli berildi. \n\nAsl buyurtma berish uchun admin bilan bog'laning - ( /contact_admin ) .")
                            .build();

                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Haqiqiy buyurtma berish jarayonini boshlash
                    // Bu yerda haqiqiy to'lovni amalga oshirish uchun kodni yozing
                    // ...
                }
            }



        }
    }

    private void sendAdminLink(long chatId) {
        String adminLink = "https://t.me/hasanboyhakimov2";
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Buyruqni bosing: \n" + adminLink)
                .build();

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendChannelLink(Long chatId) {
        String channelUrl = "https://t.me/shoh_keramika1";
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Kanalga o'tish uchun quyidagi buyruqni bosing: \n" + channelUrl)
                .build();

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendModelListByName(long chatId, String modelName) {
        List<Model> models = productService.getProductByName(modelName);

        if (models.isEmpty()) {
            sendMessage(chatId, modelName + " mahsuloti topilmadi.");
        } else {
            for (Model model : models) {

                // Model ma'lumotlarini tayyorlash
                String modelImagePath = model.getImageFile();
                String caption = "\uD83D\uDCF1 Model nomi: " + model.getName() + "\n" +
                        "\uD83D\uDCB0 Narxi: " + model.getPrice() + "\n" +
                        "\uD83D\uDCDD Tarifi: " + model.getDescription();

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();

                if (userService.isAdmin(chatId)) {
                    // Adminlar uchun "DELETE" va "UPDATE" tugmalari
                    List<InlineKeyboardButton> inlineRow = new ArrayList<>();

                    InlineKeyboardButton deleteButton = new InlineKeyboardButton();
                    deleteButton.setText("\uD83D\uDDD1 O'CHIRISH");
                    deleteButton.setCallbackData("del_" + model.getId());
                    inlineRow.add(deleteButton);

                    InlineKeyboardButton updateButton = new InlineKeyboardButton();
                    updateButton.setText("\uD83D\uDD04 TUZATISH");
                    updateButton.setCallbackData("upd_" + model.getId());
                    inlineRow.add(updateButton);

                    inlineRows.add(inlineRow);
                } else {
                    // Oddiy foydalanuvchilar uchun "Buyurtma berish" tugmasi
                    List<InlineKeyboardButton> inlineRow = new ArrayList<>();

                    InlineKeyboardButton orderButton = new InlineKeyboardButton();
                    orderButton.setText("\uD83D\uDED2 Buyurtma berish");
                    orderButton.setCallbackData("order_" + model.getId());
                    inlineRow.add(orderButton);

                    inlineRows.add(inlineRow);
                }

                inlineKeyboardMarkup.setKeyboard(inlineRows);

                // Rasmi va tugmachalarni yuborish
                sendPhoto(chatId, modelImagePath, caption, inlineKeyboardMarkup); // Tugmachalar faqat kerakli foydalanuvchilar uchun
            }
        }
    }





    private void sendPhoto(long chatId, String photoPath, String caption, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));
        sendPhoto.setPhoto(new InputFile(new java.io.File(photoPath))); // Fayl yo'li bilan rasmni jo'natish
        sendPhoto.setCaption(caption); // Rasmingiz bilan birga matnni yuboring

        // Tugmachalarni o'rnatish
        if (inlineKeyboardMarkup != null) {
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup); // Tugmachalar mavjud bo'lsa, ularni qo'shamiz
        }

        try {
            execute(sendPhoto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // Faylni saqlash
    private String saveFileToFolder(String fileId, String fileName) throws Exception {
        GetFile getFile = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
        String fileUrl = tgFile.getFileUrl(BOT_TOKEN);

        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        int fileSize = connection.getContentLength();

        if (fileSize > 20 * 1024 * 1024) { // 20 MB dan katta fayl
            throw new Exception("Fayl o'lchami 20 MB dan katta: " + fileSize + " bayt.");
        }

        InputStream inputStream = url.openStream();

        // Faylni yangi nom bilan saqlash
        java.io.File outputFile = new java.io.File("C:\\Users\\hakim\\Downloads\\" + System.currentTimeMillis() + "_" + fileName);

        FileUtils.copyInputStreamToFile(inputStream, outputFile);
        inputStream.close();
        return outputFile.getAbsolutePath();
    }

    private void sendMenuBrends(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("\uD83D\uDCF1 "+" Iphone"));
        row.add(new KeyboardButton("\uD83D\uDCF1 "+" Samsung"));
        row.add(new KeyboardButton("\uD83D\uDCF1 "+" Harxil SmartPhonelar"));
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    private void sendMenuBrendModelsIphone(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttonLists = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        // Samsung smartfonlarining barcha modellarini ro'yxati (Galaxy qismini olib tashladik)
        String[] models = {
                // Xiaomi Redmi seriyasi
                "iPhone6", "iPhone6Plus", "iPhone7", "iPhone7Plus", "iPhone8", "iPhone8Plus",
                "iPhoneX", "iPhoneXR", "iPhoneXS", "iPhoneXSMax",
                "iPhone11", "iPhone11Pro", "iPhone11ProMax",
                "iPhone12", "iPhone12Mini", "iPhone12Pro", "iPhone12ProMax",
                "iPhone13", "iPhone13Mini", "iPhone13Pro", "iPhone13ProMax",
                "iPhone14", "iPhone14Plus", "iPhone14Pro", "iPhone14ProMax",
                "iPhone15", "iPhone15Plus", "iPhone15Pro", "iPhone15ProMax",

        };

        // Har bir model uchun InlineKeyboardButton ob'ekti yaratish
        for (String model : models) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("\uD83D\uDCF1 " + model);
            button.setCallbackData("model_" + model);
            buttonList.add(button);

            // Agar 6 ta tugma qo'shilgan bo'lsa, yangi qator ochamiz
            if (buttonList.size() == 3) {
                buttonLists.add(new ArrayList<>(buttonList)); // Hozirgi tugmalar ro'yxatini tugmalar ro'yxatiga qo'shamiz
                buttonList.clear(); // Yangi tugmalar ro'yxati uchun bo'sh ro'yxat yaratamiz
            }
        }

        // Qolgan tugmalarni qo'shamiz
        if (!buttonList.isEmpty()) {
            buttonLists.add(new ArrayList<>(buttonList));
        }

        inlineKeyboardMarkup.setKeyboard(buttonLists);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
private void sendMenuBrendModelsOthers(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttonLists = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        // Samsung smartfonlarining barcha modellarini ro'yxati (Galaxy qismini olib tashladik)
        String[] models = {
                // Xiaomi Redmi seriyasi
                "RedmiNote7", "RedmiNote8", "RedmiNote9", "RedmiNote10",
                "RedmiNote11", "RedmiNote12",
                "Redmi9", "Redmi10", "Redmi11",

                // Huawei seriyasi
                "HuaweiP30", "HuaweiP40", "HuaweiP50",
                "HuaweiMate30", "HuaweiMate40", "HuaweiMate50",

        };

        // Har bir model uchun InlineKeyboardButton ob'ekti yaratish
        for (String model : models) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("\uD83D\uDCF1 " + model);
            button.setCallbackData("model_" + model);
            buttonList.add(button);

            // Agar 6 ta tugma qo'shilgan bo'lsa, yangi qator ochamiz
            if (buttonList.size() == 3) {
                buttonLists.add(new ArrayList<>(buttonList)); // Hozirgi tugmalar ro'yxatini tugmalar ro'yxatiga qo'shamiz
                buttonList.clear(); // Yangi tugmalar ro'yxati uchun bo'sh ro'yxat yaratamiz
            }
        }

        // Qolgan tugmalarni qo'shamiz
        if (!buttonList.isEmpty()) {
            buttonLists.add(new ArrayList<>(buttonList));
        }

        inlineKeyboardMarkup.setKeyboard(buttonLists);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMenuBrendModelsSamsung(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttonLists = new ArrayList<>();
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        // Samsung smartfonlarining barcha modellarini ro'yxati (Galaxy qismini olib tashladik)
        String[] models = {
                // S seriyasi
                "S8", "S8plus", "S9", "S9plus", "S10", "S10plus",
                "S20", "S20plus", "S20Ultra",
                "S21", "S21plus", "S21Ultra",
                "S22+", "S22Ultra",
                "S23+", "S23Ultra",

                // Note seriyasi
                "Note7", "Note8", "Note9",
                "Note10+", "Note20", "Note20Ultra",

                // A seriyasi
                "A20", "A21", "A23", "A24",
                "A30", "A31", "A32", "A33", "A34",
                "A40", "A41", "A50", "A51", "A52", "A53", "A54",

                // M seriyasi
                "M42", "M51", "M52", "M53",

                // Z seriyasi
                "ZFold", "ZFold2", "ZFold3",
                "ZFlip", "ZFlip3",

                // J seriyasi
                "J3", "J4", "J5", "J7"

        };

        // Har bir model uchun InlineKeyboardButton ob'ekti yaratish
        for (String model : models) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("\uD83D\uDCF1 " + model);
            button.setCallbackData("model_" + model);
            buttonList.add(button);

            // Agar 6 ta tugma qo'shilgan bo'lsa, yangi qator ochamiz
            if (buttonList.size() == 4) {
                buttonLists.add(new ArrayList<>(buttonList)); // Hozirgi tugmalar ro'yxatini tugmalar ro'yxatiga qo'shamiz
                buttonList.clear(); // Yangi tugmalar ro'yxati uchun bo'sh ro'yxat yaratamiz
            }
        }

        // Qolgan tugmalarni qo'shamiz
        if (!buttonList.isEmpty()) {
            buttonLists.add(new ArrayList<>(buttonList));
        }

        inlineKeyboardMarkup.setKeyboard(buttonLists);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Rasmni foydalanuvchiga qaytarish
    private void returnPhotoToUser(Long chatId, String filePath) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new java.io.File(filePath))); // Fayl yo'li bilan rasmni jo'natish
        try {
            execute(sendPhoto);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// =========== Rasmni Png formtda Kiritib ko'rsatish ===============

/*
*
*
* String saveFilePath = "";

                        if (message.hasPhoto()) {
                            List<PhotoSize> photos = message.getPhoto();
                            PhotoSize largestPhoto = photos.stream()
                                    .max(Comparator.comparing(PhotoSize::getFileSize))
                                    .orElse(null);

                            if (largestPhoto != null) {
                                try {
                                    saveFilePath = saveFileToFolder(largestPhoto.getFileId(), "rasm.png");
                                    returnPhotoToUser(chatId, saveFilePath);
                                } catch (TelegramApiException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }



                        }
*
* */

/*private void returnPhotoToUser(Long chatId, String fileId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(fileId)); // Fayl ID ni kiriting

        try {
            execute(sendPhoto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    /*private String saveFileToFolder(String fileId, String fileName) throws Exception {
        GetFile getFile = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
        String fileUrl = tgFile.getFileUrl(BOT_TOKEN);

        URL url = new URL(fileUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD"); // Fayl haqida ma'lumot olish uchun so'rov
        int fileSize = connection.getContentLength(); // Fayl o'lchamini olish

        // Fayl o'lchami 20 MB dan katta bo'lsa, xato xabari chiqarish
        if (fileSize > 20 * 1024 * 1024) { // 20 MB
            throw new Exception("Fayl o'lchami 20 MB dan katta: " + fileSize + " bayt.");
        }

        InputStream inputStream = url.openStream();
        java.io.File outputStream = new java.io.File("C:/gallery/" + fileName);

        FileUtils.copyInputStreamToFile(inputStream, outputStream);
        inputStream.close();
        return outputStream.getAbsolutePath();
    }*/
