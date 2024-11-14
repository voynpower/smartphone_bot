package org.example.smarphonebot2;

import org.example.smarphonebot2.controller.SmartPhoneBot2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class SmarPhoneBot2Application {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(SmarPhoneBot2Application.class, args);

		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

			SmartPhoneBot2 smartPhoneBot2 = context.getBean(SmartPhoneBot2.class);
			botsApi.registerBot(smartPhoneBot2);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}


	}

}
