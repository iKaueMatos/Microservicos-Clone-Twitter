package br.com.souza.twitterclone.accounts.service.client;

import br.com.souza.twitterclone.accounts.dto.client.DeleteNotificationRequest;

public interface INotificationsClientService {

    void createNewNotification(String userSender, String userReceiver, String tweetType, String tweetIdentifier, String authorization);

    void deleteNotification(DeleteNotificationRequest request, String authorization);
}
