package com.anarimonov.localseenbot.bot;

import com.anarimonov.localseenbot.entity.Channel;
import com.anarimonov.localseenbot.entity.Service;
import com.anarimonov.localseenbot.entity.User;
import com.anarimonov.localseenbot.entity.UserActivity;
import com.anarimonov.localseenbot.entity.dto.Data;
import com.anarimonov.localseenbot.entity.dto.Order;
import com.anarimonov.localseenbot.entity.dto.Payment;
import com.anarimonov.localseenbot.entity.dto.ResponseDto;
import com.anarimonov.localseenbot.repository.ChannelRepository;
import com.anarimonov.localseenbot.repository.ServiceRepository;
import com.anarimonov.localseenbot.repository.UserActivityRepository;
import com.anarimonov.localseenbot.repository.UserRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final UserActivityRepository userActivityRepo;
    private final UserRepository userRepo;
    private final ChannelRepository channelRepo;
    private final ServiceRepository serviceRepo;
    private final Map<Long, Order> orderMap = new HashMap<>();
    private final Map<Long, Payment> paymentMap = new HashMap<>();
    private final RestTemplate restTemplate;
    private final String globalSmmApiKey = "fead6c63cc07d74d6c6ade7da98b599769e7c120b4fcbe404f171c5c6cce6544";
    private final String seenUzApiKey = "ad3da4c933cf0c19052c355752fc84ca";
    private int referralBonus = 300;
    private double perHundredCoinPrice = 0.5;
    private String qiwiAccount = "998946803621";
    private int minAmount = 1000;
    private String adminUsername = "@LocalSeenAdmin";
    private int coinUzs = 200;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        UserActivity userActivity = getCurrentUserActivity(update);
        if (update.hasMessage()) {
            if (checkJoinedChannels(userActivity)) {
                getMessage(update, userActivity);
            }
        } else if (update.hasCallbackQuery()) {
            getCallbackQuery(update, userActivity);
        }
        userActivityRepo.save(userActivity);
    }

    private void getCallbackQuery(Update update, UserActivity userActivity) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        switch (userActivity.getStep()) {
            case 7 -> {
                int serviceId = Integer.parseInt(data);
                getService(userActivity, serviceId, 8);
            }
            case 15 -> {
                if (data.equals("card")) {
                    Long userId = userActivity.getUser().getId();
                    Payment payment = paymentMap.get(userId);
                    sendTextMessage(userActivity.setStep(0), "\uD83D\uDECD Humo, Uzcard\n" +
                            "\uD83D\uDCB0 Tangalar soni : " + payment.getAmount() + " ta\n" +
                            "\uD83D\uDCB5 To'lov miqdori: " + payment.getPrice() * coinUzs + " so'm\n" +
                            "✅ Hisob raqamingiz ID: " + userId + "\n" +
                            "        \n" +
                            "\uD83D\uDCB3 Karta (Humo) tizimi:\n" +
                            "\uD83D\uDD36 9860 2466 0118 2107\n" +
                            "        \n" +
                            "ℹ️ To'lov admin tomonidan tastiqlanishi uchun quydagilarni bajaring!\n" +
                            "        \n" +
                            "1️⃣ Bot sizga bergan " + payment.getPrice() * coinUzs + " so'm miqdorini  9860 2466 0118 2107 karta hisobiga to'lang,\n" +
                            "        \n" +
                            "2️⃣ Keyin " + adminUsername + "-ga o'zingizni ID raqamingizni va to'lov haqidagi chek bilan birga yuboring!");
                }
            }
        }
    }

    private void getMessage(Update update, UserActivity userActivity) {
        Message message = update.getMessage();
        Long userId = userActivity.getUser().getId();
        if (userActivity.getRole().equals("admin") && userActivity.getStep() == 14)
            if (!message.hasText() || message.hasText() && !(message.getText().equals("Bekor qilish") || message.getText().equals("/start") || message.getText().equals("/stats"))) {
                for (User user : userRepo.findAll())
                    sendForwardMessage(user.getId().toString(), userId.toString(), message.getMessageId());
                userActivity.setStep(0);
            }
        if (message.hasText()) {
            String text = message.getText();
            if (text.startsWith("/start ")) {
                String[] s = text.split(" ");
                Optional<User> optionalReferrer = userRepo.findById(Long.valueOf(s[1]));
                if (optionalReferrer.isPresent() && !userActivity.isStarted()) {
                    User referrer = optionalReferrer.get();
                    userActivity.getUser().setReferrer(referrer);
                    userActivity.setStarted(true);
                    UserActivity referrerActivity = userActivityRepo.findByUserId(referrer.getId());
                    sendTextMessage(referrerActivity.setCoins(referrerActivity.getCoins() + referralBonus),
                            "\uD83C\uDF89 Tabriklaymiz, yangi obunachi uchun hisobingizga " + referralBonus + " tanga qo'shildi");
                    userRepo.save(userActivity.getUser());
                    userActivityRepo.save(referrerActivity);
                }
                startMessage(userActivity);
            }
            switch (text) {
                case "/start", "Bekor qilish" -> {
                    if (!userActivity.isStarted())
                        userActivity.setStarted(true);
                    startMessage(userActivity);
                }
                case "\uD83D\uDC41 Prasmotr" -> getService(userActivity, 1334, 1);
                case "❤️ Layk / Ovoz" -> getService(userActivity, 892, 3);
                case "\uD83D\uDC4D Reaksiya" -> sendTextMessage(userActivity.setStep(6), "Xizmatlardan birini tanlang");
                case "\uD83D\uDC64 Mening hisobim" ->
                        sendTextMessage(userActivity, "\uD83D\uDC64 Foydalanuvchi ID: " + userId + "\n" +
                                "\uD83D\uDC49 Ro'yxatdan o'tilgan kun: " + userActivity.getCreatedAt()
                                .toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "\n" +
                                "\uD83D\uDC49 Taklif qilgansiz: " + userRepo.countByReferrerId(userId) + "\n" +
                                "\uD83D\uDCB0 Balansingiz: " + userActivity.getCoins() + " tanga\n");
                case "\uD83D\uDC65 Referral" -> {
                    sendPhotoMessage(userActivity, "src/main/resources/img/img.png", "✅ LocalSeen Bot\n" +
                            "\n" +
                            "\uD83D\uDC41\u200D\uD83D\uDDE8 Telegram postlari uchun ixtisoslashtirilgan ko'rishni oshirish\n" +
                            "❤️ Telegram postlariga layklarni ko'paytiring\n" +
                            "\uD83D\uDC4D \uD83D\uDC4E ❤️ Telegram postlariga reaksiyalarni ko'paytiring\n" +
                            "\uD83D\uDCC8 So'rovnomalarga ovozlarni ko'paytiring\n" +
                            "\uD83D\uDD10 Xavfsiz to'lov\n" +
                            "\n" +
                            "\uD83D\uDD17 https://t.me/LocalSeenBot?start=" + userId);
                    sendTextMessage(userActivity, "Yuqoridagi bannerni do'stlaringizga yoki kontaktlaringizga yuboring va siz LocalSeenga qo'shilgan har bir yangi a'zo uchun siz " + referralBonus + " tanga olasiz!\n" +
                            "\n" +
                            "Foydalanuvchi ID: " + userId);
                }
                case "\uD83D\uDCB3 Tanga sotib olish" -> sendTextMessage(userActivity.setStep(14),
                        "❓Siz xohlagan #tangalar sonini kiriting. (1000 tanga uchun " + perHundredCoinPrice * 10 + " rubl)\n" +
                                "\n" +
                                "* Minimal miqdor - " + minAmount + " tanga.\n" +
                                "* Sotib olgan tangalaringiz muddati tugamaydi.\n" +
                                "* 1 ko'rish = " + serviceRepo.findById(1334).get().getCost() + " tanga\n" +
                                "* 1 ta yoqtirish/ovoz berish = " + serviceRepo.findById(892).get().getCost() + " tanga");
                case "\uD83C\uDD98 Qo'llab-quvvatlash" ->
                        sendTextMessage(userActivity.setStep(0), "\uD83D\uDD30 Agar sizda biron bir savol yoki muammo bo'lsa, bizning qo'llab-quvvatlash jamoamiz bilan bog'laning.\n" +
                                "\n" + adminUsername);
                case "/stats" ->
                        sendTextMessage(userActivity.setStep(0), "Hozirda bot faydalanuvchilari soni " + userActivityRepo.count() + "ta");
                default -> getDefaultMessage(message, userActivity);
            }
        } else {
            int step = userActivity.getStep();
            Order order = orderMap.get(userId);
            if (step == 2 || step == 9 || step == 11 || step == 13) {
                if (getLink(message, userActivity, null)) return;
                getStatus(userActivity, order);
            }
        }
    }


    private void getService(UserActivity userActivity, int serviceId, int nextStep) {
        Service service = serviceRepo.findById(serviceId).get();
        Order order = orderMap.get(userActivity.getUser().getId());
        if (order == null) order = new Order();
        order.setService(service);
        orderMap.put(userActivity.getUser().getId(), order);
        sendTextMessage(userActivity.setStep(nextStep), "\uD83D\uDC41\u200D\uD83D\uDDE8 " + service.getName() + " sonini " + service.getMin() + " dan " + service.getMax() + " gacha kiriting.\n" +
                "\nBalansingiz: " + userActivity.getCoins() + " tanga\n" +
                "\n" +
                "\uD83D\uDCA1 Har bir " + service.getCategory() + " narxi " + service.getCost() + " tanga.");
    }

    private void startMessage(UserActivity userActivity) {
        userActivity.setStep(0);
        sendTextMessage(userActivity, """
                Salom, LocalSeen ✋ ga xush kelibsiz

                LocalSeen bilan telegramdagi postlaringizni ko‘rishlar, layklar va ovozlar sonini oshirish uchun bir necha marta bosish kifoya.

                Davom etish uchun birini tanlang""");
    }

    private void getDefaultMessage(Message message, UserActivity userActivity) {
        String text = message.getText();
        Long userId = userActivity.getUser().getId();
        int step = userActivity.getStep();
        Order order = orderMap.get(userId);
        if (order == null) {
            order = new Order();
            orderMap.put(userId, order);
        }
        if (userActivity.getRole().equals("user")) {
            switch (step) {
                case 1, 3, 8, 10, 12 -> getQuantity(userActivity, text, order);
                case 2, 9, 11, 13 -> {
                    if (getLink(message, userActivity, text)) return;
                    getStatus(userActivity, order);
                    orderMap.remove(userId);
                }
                case 4 -> getLink(message, userActivity, text);
                case 5 -> {
                    order.setAnswer(Integer.parseInt(text));
                    orderMap.put(userId, order);
                    sendTextMessage(userActivity.setStep(0), "☑️ Buyurtma qabul qilindi. \n" +
                            "\n" +
                            "So'rov: " + order.getQuantity() + " " + order.getService().getCategory() + "\n");
                    getStatus(userActivity, order);
                }
                case 6 -> {
                    switch (text) {
                        case "Bitta reaksiyani tanlash" ->
                                sendTextMessage(userActivity.setStep(7), "\uD83C\uDFAF Kerakli reaksiyani tanlang:");
                        case "Ijobiy reaksiyalar[\uD83D\uDC4D ❤️ \uD83D\uDD25 \uD83C\uDF89 \uD83D\uDE01]" ->
                                getService(userActivity, 805, 10);
                        case "Salbiy reaksiyalar[\uD83D\uDC4E \uD83D\uDE31 \uD83D\uDCA9 \uD83D\uDE22 \uD83E\uDD2E]" ->
                                getService(userActivity, 806, 12);
                    }
                }
                case 14 -> {
                    int amount = Integer.parseInt(text);
                    if (amount >= minAmount) {
                        Payment payment = new Payment();
                        payment.setAmount(amount);
                        payment.setPrice((amount / 100.0) * perHundredCoinPrice);
                        paymentMap.put(userId, payment);
                        sendTextMessage(userActivity.setStep(15), "\uD83D\uDD30\u200D\u200D\u200D\u200D\u200D " + amount / 100 * perHundredCoinPrice + " rublga " + amount + " tanga sotib oling.\n" +
                                "\n" +
                                "To'lovdan so'ng siz to'lovning skrinshotini va hisob raqamini (" + userId + ") administratorga (" + adminUsername + ") yuborishing.\n" +
                                "\n" +
                                "Quyidagi tugmalar bilan toʻlang \uD83D\uDC47");
                    }
                }
            }
        } else if (userActivity.getRole().equals("admin")) {
            switch (text) {
                case "CRUD" -> sendTextMessage(userActivity.setStep(1), "Kategoriyalardan birini tanlang");
                case "Kanal" -> sendTextMessage(userActivity.setStep(2), "Bajariladigan amalni kiriting");
                case "Admin Menu" -> sendTextMessage(userActivity.setStep(5), "Bajariladigan amalni kiriting");
                case "Referral" -> sendTextMessage(userActivity.setStep(9), "Tangalar sonini kiriting");
                case "Tanga narxi" -> sendTextMessage(userActivity.setStep(10), "Har 100ta tanga narxini kiriting");
                case "Min. Tanga olish" -> sendTextMessage(userActivity.setStep(11), "Sonni kiriting");
                case "Qiwi" -> sendTextMessage(userActivity.setStep(12), "Qiwi hisob raqamini kiriting");
                case "Kurs" -> sendTextMessage(userActivity.setStep(13), "Rublning so'mdagi kursini kiriting");
                case "Reklama jo'natish" -> sendTextMessage(userActivity.setStep(14), "Xabarni yuboring");
                case "Foydalanuvchiga tanga jo'natish" ->
                        sendTextMessage(userActivity.setStep(15), "Foydalanuvchi ID si va tanga sonini kiriting (masalan: 12345678-1000)");
            }
            switch (step) {
                case 2 -> {
                    if (text.equals("Qo'shish"))
                        sendTextMessage(userActivity.setStep(3), "Kanal usernameni kiriting");
                    else if (text.equals("O'chirish"))
                        sendTextMessage(userActivity.setStep(4), "Kanal usernameni tanlang");
                }
                case 3 -> {
                    channelRepo.save(new Channel(text));
                    sendTextMessage(userActivity.setStep(1), "Kanal qo'shildi");
                }
                case 4 -> {
                    Channel channel = channelRepo.findByChannelId(text);
                    if (channel != null) {
                        channelRepo.delete(channel);
                        sendTextMessage(userActivity.setStep(1), "o'chirildi");
                    }
                }
                case 5 -> {
                    switch (text) {
                        case "Qo'shish" -> sendTextMessage(userActivity.setStep(6), "Foydalanuvchi IDsini kiriting");
                        case "O'chirish" -> sendTextMessage(userActivity.setStep(7), "Foydalanuvchi IDsini kiriting");
                        case "Admin username" -> sendTextMessage(userActivity.setStep(8), "Usernamemini kiriting");
                    }
                }
                case 6, 7 -> {
                    Long id = Long.parseLong(text);
                    if (!id.equals(userId)) {
                        UserActivity userActivity1 = userActivityRepo.findByUserId(id);
                        if (step == 6)
                            userActivity1.setRole("admin");
                        else
                            userActivity1.setRole("user");
                        userActivityRepo.save(userActivity1);
                    }
                    startMessage(userActivity);
                }
                case 8 -> adminUsername = text;
                case 9 -> referralBonus = Integer.parseInt(text);
                case 10 -> perHundredCoinPrice = Integer.parseInt(text);
                case 11 -> minAmount = Integer.parseInt(text);
                case 12 -> qiwiAccount = text;
                case 13 -> coinUzs = Integer.parseInt(text);
                case 15 -> {
                    String[] split = text.split("-");
                    UserActivity byUserId = userActivityRepo.findByUserId(Long.parseLong(split[0]));
                    byUserId.setCoins(byUserId.getCoins() + Integer.parseInt(split[1]));
                    userActivityRepo.save(byUserId);
                    sendTextMessage(byUserId.setStep(0), "Sizga admin tomonidan " + split[1] + " tanga yuborildi");
                }
            }
        }
    }

    private ReplyKeyboard getReplyKeyboard(UserActivity userActivity, boolean isChannelMember) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        replyKeyboardMarkup.setKeyboard(rows);
        Long userId = userActivity.getUser().getId();
        if (!isChannelMember) {
            return getJoinChannelRequest();
        } else {
            KeyboardRow row = new KeyboardRow();
            int step = userActivity.getStep();
            if (userActivity.getRole().equals("user")) {
                switch (step) {
                    case 0 -> {
                        row.add("\uD83D\uDC41 Prasmotr");
                        row.add("❤️ Layk / Ovoz");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("\uD83D\uDC64 Mening hisobim");
                        row.add("\uD83D\uDC4D Reaksiya");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("\uD83D\uDCB3 Tanga sotib olish");
                        row.add("\uD83C\uDD98 Qo'llab-quvvatlash");
                        row.add("\uD83D\uDC65 Referral");
                        rows.add(row);
                    }
                    case 6 -> {
                        row.add("Bitta reaksiyani tanlash");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("Ijobiy reaksiyalar[\uD83D\uDC4D ❤️ \uD83D\uDD25 \uD83C\uDF89 \uD83D\uDE01]");
                        row.add("Salbiy reaksiyalar[\uD83D\uDC4E \uD83D\uDE31 \uD83D\uDCA9 \uD83D\uDE22 \uD83E\uDD2E]");
                        rows.add(row);
                    }
                    case 7 -> {
                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        List<InlineKeyboardButton> inlineRow = new ArrayList<>();
                        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
                        markup.setKeyboard(inlineRows);
                        List<Service> reactions = serviceRepo.findByCategory("reaksiya");
                        int i = 0;
                        for (Service reaction : reactions) {
                            InlineKeyboardButton button = new InlineKeyboardButton();
                            button.setText(reaction.getAbout());
                            button.setCallbackData(String.valueOf(reaction.getId()));
                            inlineRow.add(button);
                            i++;
                            if (i == 4) {
                                i = 0;
                                inlineRows.add(inlineRow);
                                inlineRow = new ArrayList<>();
                            }
                        }
                        inlineRows.add(inlineRow);
                        return markup;
                    }
                    case 15 -> {
                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        List<InlineKeyboardButton> inlineRow = new ArrayList<>();
                        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
                        markup.setKeyboard(inlineRows);
                        Payment payment = paymentMap.get(userId);
                        inlineRow.add(InlineKeyboardButton.builder()
                                .text("Qiwi").url("https://qiwi.com/payment/form/99?extra%5B%27account%27%5D=" + qiwiAccount + "&amountInteger=" + payment.getPrice() + "&amountFraction=0&extra%5B%27comment%27%5D=LocalSeenBot uchun " + userId + "&currency=643")
                                .pay(true).build());
                        inlineRows.add(inlineRow);
                        inlineRow = new ArrayList<>();
                        inlineRow.add(InlineKeyboardButton.builder()
                                .text("Humo, Uzcard").callbackData("card").build());
                        inlineRows.add(inlineRow);
                        return markup;
                    }
                }
            } else if (userActivity.getRole().equals("admin")) {
                switch (step) {
                    case 0 -> {
                        row.add("CRUD");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("Reklama jo'natish");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("Foydalanuvchiga tanga jo'natish");
                        rows.add(row);
                    }
                    case 1 -> {
                        row.add("Kanal");
                        row.add("Admin Menu");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("Referral");
                        row.add("Tanga narxi");
                        rows.add(row);
                        row = new KeyboardRow();
                        row.add("Min. Tanga olish");
                        row.add("Qiwi");
                        row.add("Kurs");
                    }
                    case 2 -> getCrudKeyboard(rows);
                    case 5 -> {
                        getCrudKeyboard(rows);
                        row.add("Admin username");
                        rows.add(row);
                    }
                }
            }
            if (step > 0) {
                row = new KeyboardRow();
                row.add("Bekor qilish");
                rows.add(row);
            }
        }
        return replyKeyboardMarkup;
    }

    private void getCrudKeyboard(List<KeyboardRow> rows) {
        KeyboardRow row = new KeyboardRow();
        row.add("Qo'shish");
        row.add("O'chirish");
        rows.add(row);
    }

    private void getQuantity(UserActivity userActivity, String text, Order order) {
        Service service = order.getService();
        long id = userActivity.getUser().getId();
        int quantity = Integer.parseInt(text);
        if (quantity >= service.getMin() && quantity <= service.getMax()) {
            if (userActivity.getCoins() < quantity * order.getService().getCost())
                sendTextMessage(userActivity, "Tanga yetarli emas!");
            else {
                order.setQuantity(quantity);
                orderMap.put(id, order);
                sendTextMessage(userActivity.setStep(userActivity.getStep() + 1), "\uD83D\uDC49 Postni yuboring (kerak bo'lsa post" +
                        " havolasini ham yuborishingiz mumkin)\n" +
                        "\n" +
                        "So'rov: " + quantity + " marta " + service.getCategory());
            }
        } else if (quantity < service.getMin() || quantity > service.getMax())
            sendTextMessage(userActivity, "\uD83D\uDC41\u200D\uD83D\uDDE8 " + service.getName() + " sonini " + service.getMin() +
                    " dan " + service.getMax() + " gacha kiriting.");
    }

    private void getStatus(UserActivity userActivity, Order order) {
        Thread thread = new Thread(() -> {
            Service service = order.getService();
            String url;
            int fee = order.getQuantity() * order.getService().getCost();
            if (service.getUrl().startsWith("https://global-smm.ru")) {
                url = service.getUrl() + "add?link=" + order.getLink() + "&amount=" + order.getQuantity() +
                        "&service=" + service.getId() + "&access_token=" + globalSmmApiKey;
                ResponseDto res = restTemplate.getForObject(url, ResponseDto.class);
                if (res == null || !res.isSuccess())
                    sendTextMessage(userActivity.setCoins(userActivity.getCoins() + fee), "Buyurtma bajarilmadi ❌. \nTangalaringiz qaytarildi");
                else order.setId(res.getData().getOrder());
            } else {
                url = service.getUrl() + "?key=" + seenUzApiKey + "&action=add&service=" + service.getId() + "&link=" + order.getLink() + "&quantity=" + order.getQuantity() + "&answer_number=" + order.getAnswer();
                Data res = restTemplate.getForObject(url, Data.class);
                if (res == null)
                    sendTextMessage(userActivity.setCoins(userActivity.getCoins() + fee), "Buyurtma bajarilmadi ❌. \nTangalaringiz qaytarildi");
                else order.setId(res.getOrder());
            }
            while (true) {
                try {
                    Thread.sleep(20000);
                    String status = "";
                    if (service.getUrl().startsWith("https://global-smm.ru")) {
                        url = service.getUrl() + "status?id=" + order.getId() + "&access_token=" + globalSmmApiKey;
                        ResponseDto res1 = restTemplate.getForObject(url, ResponseDto.class);
                        if (res1 != null && res1.isSuccess())
                            status = res1.getData().getStatus();
                    } else {
                        URL url1 = new URL(service.getUrl() + "?key=" + seenUzApiKey + "&action=status&order=" + order.getId());
                        URLConnection urlConnection = url1.openConnection();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        Gson gson = new Gson();
                        Data data = gson.fromJson(bufferedReader, Data.class);
                        status = data.getStatus();
                    }
                    if (status.equalsIgnoreCase("completed")) {
                        sendTextMessage(userActivity, "✅ Buyurtma " + order.getId() + " bajarildi! \n" +
                                "\n" +
                                "So'rov: " + order.getQuantity() + " " + order.getService().getCategory() + "\n");
                        return;
                    } else if (status.equalsIgnoreCase("canceled") || status.equalsIgnoreCase("deleted")) {
                        sendTextMessage(userActivity.setCoins(userActivity.getCoins() + fee), "Buyurtma bajarilmadi ❌. \nTangalaringiz qaytarildi");
                        return;
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    private boolean getLink(Message message, UserActivity userActivity, String text) {
        Chat forwardFromChat = message.getForwardFromChat();
        Order order = orderMap.get(userActivity.getUser().getId());
        if (forwardFromChat != null) {
            String username = forwardFromChat.getUserName();
            Integer messageId = message.getForwardFromMessageId();
            order.setLink("https://t.me/" + username + "/" + messageId);
        } else if (text.startsWith("https://"))
            order.setLink(text);
        else {
            sendTextMessage(userActivity, "Postni forward qiling yoki linkini yuboring!");
            return true;
        }
        if (userActivity.getStep() == 4) {
            sendTextMessage(userActivity.setStep(5), "Javob raqamini kiriting");
            return false;
        }
        int coins = userActivity.getCoins();
        userActivity.setCoins(coins - order.getQuantity() * order.getService().getCost());
        sendTextMessage(userActivity.setStep(0), "☑️ Buyurtma qabul qilindi. \n" +
                "\n" +
                "So'rov: " + order.getQuantity() + " " + order.getService().getCategory() + "\n");
        return false;
    }

    private ReplyKeyboard getJoinChannelRequest() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        for (Channel channel : channelRepo.findAll()) {
            List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Kanal");
            button.setUrl("t.me/" + channel.getChannelId());
            keyboardRow.add(button);
            keyboardRows.add(keyboardRow);
        }
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private boolean checkJoinedChannels(UserActivity userActivity) {
        List<Channel> all = channelRepo.findAll();
        Long userId = userActivity.getUser().getId();
        for (Channel channel : all) {
            PromoteChatMember promoteChatMember = new PromoteChatMember("@" + channel.getChannelId(), userId);
            try {
                execute(promoteChatMember);
            } catch (TelegramApiException e) {
                if (e.getMessage().equals("Error promoting chat member: [400] Bad Request: bots can't add new chat members")) {
                    sendTextMessage(userActivity, "Iltimos, avvalo kanallarga obuna bo'ling va qayta \n/start buyrug'ini yuboring");
                    return false;
                }
            }
        }
        return true;
    }

    private void sendTextMessage(UserActivity userActivity, String text) {
        ReplyKeyboard replyKeyboard;
        if (text.equals("Iltimos, avvalo kanallarga obuna bo'ling va qayta \n/start buyrug'ini yuboring")) {
            replyKeyboard = getReplyKeyboard(userActivity, false);
        } else replyKeyboard = getReplyKeyboard(userActivity, true);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(userActivity.getUser().getId().toString());
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setParseMode("html");
        sendMessage.setDisableWebPagePreview(true);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPhotoMessage(UserActivity userActivity, String path, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(userActivity.getUser().getId().toString());
        sendPhoto.setPhoto(new InputFile(new File(path)));
        sendPhoto.setCaption(caption);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendForwardMessage(String toChatId, String fromChatId, int messageId) {
        ForwardMessage forwardMessage = new ForwardMessage();
        forwardMessage.setChatId(toChatId);
        forwardMessage.setMessageId(messageId);
        forwardMessage.setFromChatId(fromChatId);
        try {
            execute(forwardMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private UserActivity getCurrentUserActivity(Update update) {
        org.telegram.telegrambots.meta.api.objects.User from;
        if (update.hasMessage()) from = update.getMessage().getFrom();
        else from = update.getCallbackQuery().getFrom();
        Long id = from.getId();
        UserActivity user = userActivityRepo.findByUserId(id);
        if (user == null) {
            User save = userRepo.save(new User(id, from.getFirstName(), from.getLastName(), from.getUserName(), null));
            return userActivityRepo.save(new UserActivity(save, "uz", "user", 0, 0, false));
        } else
            return user;
    }
}
