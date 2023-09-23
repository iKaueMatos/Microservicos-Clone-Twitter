package br.comsouza.twitterclone.feed.database.repository.postdetails;

import br.comsouza.twitterclone.feed.client.IAccountsClient;
import br.comsouza.twitterclone.feed.dto.posts.TimelineTweetResponse;
import br.comsouza.twitterclone.feed.handler.exceptions.ServerSideErrorException;
import br.comsouza.twitterclone.feed.service.interactions.IInteractionsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class PostDetailsRepository {

    @PersistenceContext
    private final EntityManager em;
    private final IInteractionsService iInteractionsService;
    private final IAccountsClient iAccountsClient;
    private static final Integer CUSTOM_PAGE = 0;
    private static final Integer CUSTOM_PAGE_SIZE = 10;

    public PostDetailsRepository(EntityManager em,
                                 IInteractionsService iInteractionsService,
                                 IAccountsClient iAccountsClient) {
        this.em = em;
        this.iInteractionsService = iInteractionsService;
        this.iAccountsClient = iAccountsClient;
    }

    public TimelineTweetResponse find(String sessionUserIdentifier, String targetTweetIdentifier, String authorization) throws ServerSideErrorException {

        StringBuilder sb = new StringBuilder();
        sb.append("DECLARE @targetTweetIdentifier VARCHAR(MAX) = ?  ");
        sb.append("  ");
        sb.append("SELECT t.tweet_identifier  ");
        sb.append("	  ,t.original_tweet_identifier  ");
        sb.append("	  ,p.description  ");
        sb.append("	  ,u.identifier  ");
        sb.append("	  ,u.username  ");
        sb.append("	  ,u.first_name  ");
        sb.append("	  ,u.profile_photo_identifier  ");
        sb.append("	  ,t.message  ");
        sb.append("	  ,t.attachment  ");
        sb.append("FROM tweets t  ");
        sb.append("INNER JOIN users u  ");
        sb.append("	ON u.identifier = t.user_identifier  ");
        sb.append("INNER JOIN tweets_types p  ");
        sb.append("	ON p.type_identifier = t.type  ");
        sb.append("WHERE t.tweet_identifier = @targetTweetIdentifier ");
        sb.append("ORDER BY t.publication_time desc  ");

        Query query = em.createNativeQuery(sb.toString());
        query.setParameter(1, targetTweetIdentifier);

        try {
            Object[] result = (Object[]) query.getSingleResult();

            return TimelineTweetResponse.builder()
                    .tweetIdentifier((String) result[0])
                    .originalTweetIdentifier((String) result[1])
                    .tweetTypeDescription((String) result[2])
                    .userIdentifier((String) result[3])
                    .userUsername((String) result[4])
                    .userFirstName((String) result[5])
                    .userProfilePhoto(iAccountsClient.loadProfilePhoto((String) result[6], authorization))
                    .tweetMessage((String) result[7])
                    .tweetAttachment((byte[]) result[8])
                    .tweetCommentsList(new ArrayList<>())
                    .tweetCommentsCount(iInteractionsService.getAllTweetCommentsCount((String) result[0]))
                    .tweetRetweetsCount(iInteractionsService.getTweetOnlyValuedRetweetsPageable((String) result[0], CUSTOM_PAGE, CUSTOM_PAGE_SIZE).getTotalElements())
                    .tweetNoValuesRetweetsCount(iInteractionsService.getTweetOnlyNoValueRetweetsPageable((String) result[0], CUSTOM_PAGE, CUSTOM_PAGE_SIZE).getTotalElements())
                    .tweetLikesCount(iInteractionsService.getTweetLikesCount((String) result[0]))
                    .tweetViewsCount(iInteractionsService.getTweetViewsCount((String) result[0]))
                    .tweetFavsCount(iInteractionsService.getTweetFavsCount((String) result[0]))
                    .isLikedByMe(iInteractionsService.verifyIsLiked((String) result[0], sessionUserIdentifier).isPresent())
                    .isRetweetedByMe(iInteractionsService.verifyIsRetweeted((String) result[0], sessionUserIdentifier).isPresent())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
