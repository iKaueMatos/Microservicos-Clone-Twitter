package br.comsouza.twitterclone.feed.database.repository.timeline.impl;

import br.comsouza.twitterclone.feed.client.IAccountsClient;
import br.comsouza.twitterclone.feed.database.repository.timeline.ITimelineStrategy;
import br.comsouza.twitterclone.feed.dto.posts.TimelineTweetResponse;
import br.comsouza.twitterclone.feed.handler.exceptions.ServerSideErrorException;
import br.comsouza.twitterclone.feed.service.interactions.IInteractionsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ForyouTimelineRepository implements ITimelineStrategy {

    @PersistenceContext
    private final EntityManager em;
    private final IInteractionsService iInteractionsService;

    public ForyouTimelineRepository(EntityManager em,
                                    IInteractionsService iInteractionsService) {
        this.em = em;
        this.iInteractionsService = iInteractionsService;
    }

    @Override
    public List<TimelineTweetResponse> getTimeLine(String sessionUserIdentifier, Integer page, Integer size, String targetUserIdentifier) {

        StringBuilder sb = new StringBuilder();
        sb.append("DECLARE @sessionUserIdentifier VARCHAR(MAX)  = ? ");
        sb.append("	      ,@PageNumber			  INT           = ? ");
        sb.append("       ,@RowsOfPage			  INT           = ? ");
        sb.append("  ");
        sb.append("SELECT t.tweet_identifier  ");
        sb.append("	  ,t.original_tweet_identifier  ");
        sb.append("	  ,p.description  ");
        sb.append("	  ,u.identifier  ");
        sb.append("	  ,u.username  ");
        sb.append("   ,u.first_name  ");
        sb.append("	  ,u.profile_photo_url  ");
        sb.append("	  ,t.message  ");
        sb.append("	  ,t.attachment  ");
        sb.append("FROM tweets t  ");
        sb.append("INNER JOIN users u  ");
        sb.append("	ON u.identifier = t.user_identifier ");
        sb.append("INNER JOIN tweets_types p  ");
        sb.append("	ON p.type_identifier = t.type  ");
        sb.append("LEFT JOIN silenced_users s  ");
        sb.append("	ON s.silenced_identifier = t.user_identifier  ");
        sb.append("	AND s.silencer_identifier = @sessionUserIdentifier  ");
        sb.append("LEFT JOIN blocked_users b  ");
        sb.append("	ON b.blocked_identifier = t.user_identifier  ");
        sb.append("	AND b.blocker_identifier = @sessionUserIdentifier  ");
        sb.append("WHERE s.silenced_identifier IS NULL AND b.blocked_identifier IS NULL ");
        sb.append("ORDER BY t.publication_time desc  ");
        sb.append("OFFSET (@PageNumber - 1) * @RowsOfPage ROWS    ");
        sb.append("FETCH NEXT @RowsOfPage ROWS ONLY    ");

        Query query = em.createNativeQuery(sb.toString());
        query.setParameter(1, sessionUserIdentifier);
        query.setParameter(2, page);
        query.setParameter(3, size);

        List<Object[]> list = query.getResultList();

        List<TimelineTweetResponse> response = new ArrayList<>();

        if (!list.isEmpty()) {
            for (Object[] result : list) {
                response.add(TimelineTweetResponse.builder()
                        .tweetIdentifier((String) result[0])
                        .originalTweetIdentifier((String) result[1])
                        .tweetTypeDescription((String) result[2])
                        .userIdentifier((String) result[3])
                        .userUsername((String) result[4])
                        .userFirstName((String) result[5])
                        .userProfilePhotoUrl((String) result[6])
                        .tweetMessage((String) result[7])
                        .tweetAttachment((byte[]) result[8])
                        .tweetCommentsCount(iInteractionsService.getAllTweetCommentsCount((String) result[0]))
                        .tweetRetweetsCount(iInteractionsService.getTweetAllRetweetsTypesCount((String) result[0]))
                        .tweetLikesCount(iInteractionsService.getTweetLikesCount((String) result[0]))
                        .tweetViewsCount(iInteractionsService.getTweetViewsCount((String) result[0]))
                        .isLikedByMe(iInteractionsService.verifyIsLiked((String) result[0], sessionUserIdentifier).isPresent())
                        .isRetweetedByMe(iInteractionsService.verifyIsRetweeted((String) result[0], sessionUserIdentifier).isPresent())
                        .build());
            }
        }
        return response;
    }
}
