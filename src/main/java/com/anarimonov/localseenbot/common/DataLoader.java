package com.anarimonov.localseenbot.common;

import com.anarimonov.localseenbot.bot.Bot;
import com.anarimonov.localseenbot.entity.Service;
import com.anarimonov.localseenbot.repository.ChannelRepository;
import com.anarimonov.localseenbot.repository.ServiceRepository;
import com.anarimonov.localseenbot.repository.UserActivityRepository;
import com.anarimonov.localseenbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    @Value("${botToken}")
    private String botToken;

    @Value("${botUsername}")
    private String botUsername;

    private final UserActivityRepository userActivityRepo;
    private final UserRepository userRepo;
    private final ChannelRepository channelRepo;
    private final ServiceRepository serviceRepo;
    private final RestTemplate restTemplate;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String initMode;

    @Override
    public void run(String... args) throws Exception {
        if (initMode.equals("create")) {
            serviceRepo.save(new Service(1334, 50, 300000, "Ko'rishlar", "ko'rish", "https://global-smm.ru/api/v1/order/", 1, ""));
            serviceRepo.save(new Service(892,25,25000,"Layklar/Ovozlar", "layk/ovoz", "https://seen.uz/api/v2", 50, ""));
            serviceRepo.save(new Service(805,50,500000,"Reaksiyalar", "reaksiya(mix)", "https://seen.uz/api/v2", 10, ""));
            serviceRepo.save(new Service(806,50,500000,"Reaksiyalar", "reaksiya(mix)", "https://seen.uz/api/v2", 10, ""));
            serviceRepo.save(new Service(807,50,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 20, "\uD83D\uDC4D"));
            serviceRepo.save(new Service(808,50,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 20, "\uD83D\uDC4E"));
            serviceRepo.save(new Service(809,50,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "❤️"));
            serviceRepo.save(new Service(810,50,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "\uD83D\uDD25"));
            serviceRepo.save(new Service(811,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "\uD83C\uDF89"));
            serviceRepo.save(new Service(812,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "\uD83E\uDD29"));
            serviceRepo.save(new Service(813,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "\uD83D\uDE31"));
            serviceRepo.save(new Service(814,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "\uD83D\uDE01"));
            serviceRepo.save(new Service(815,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "😢"));
            serviceRepo.save(new Service(816,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "💩"));
            serviceRepo.save(new Service(817,10,500000,"Reaksiyalar", "reaksiya", "https://seen.uz/api/v2", 50, "🤮"));
        }
        new TelegramBotsApi(DefaultBotSession.class).registerBot(new Bot(botToken, botUsername, userActivityRepo, userRepo, channelRepo, serviceRepo,restTemplate));
        while (true){
            String forObject = restTemplate.getForObject(
                    "https://telsale-bot.herokuapp.com/api/test/hello",
                    String.class
            );
            System.out.println(forObject);
            try {
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
