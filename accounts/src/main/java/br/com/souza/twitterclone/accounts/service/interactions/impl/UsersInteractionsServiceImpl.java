package br.com.souza.twitterclone.accounts.service.interactions.impl;

import br.com.souza.twitterclone.accounts.database.model.BlockedUsers;
import br.com.souza.twitterclone.accounts.database.model.BlockedUsersId;
import br.com.souza.twitterclone.accounts.database.model.User;
import br.com.souza.twitterclone.accounts.database.model.UsersFollows;
import br.com.souza.twitterclone.accounts.database.model.UsersFollowsId;
import br.com.souza.twitterclone.accounts.database.model.UsersPendingFollows;
import br.com.souza.twitterclone.accounts.database.model.UsersPendingFollowsId;
import br.com.souza.twitterclone.accounts.database.repository.BlockedUsersRepository;
import br.com.souza.twitterclone.accounts.database.repository.UserRepository;
import br.com.souza.twitterclone.accounts.database.repository.UsersFollowsRepository;
import br.com.souza.twitterclone.accounts.database.repository.UsersPendingFollowsRepository;
import br.com.souza.twitterclone.accounts.handler.exceptions.NonexistentPendingFollowException;
import br.com.souza.twitterclone.accounts.handler.exceptions.UnableToFollowException;
import br.com.souza.twitterclone.accounts.handler.exceptions.UserNotFoundException;
import br.com.souza.twitterclone.accounts.service.interactions.IUsersInteractionsService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UsersInteractionsServiceImpl implements IUsersInteractionsService {

    private final UserRepository userRepository;
    private final BlockedUsersRepository blockedUsersRepository;
    private final UsersFollowsRepository usersFollowsRepository;
    private final UsersPendingFollowsRepository usersPendingFollowsRepository;

    public UsersInteractionsServiceImpl(UserRepository userRepository,
                                        BlockedUsersRepository blockedUsersRepository,
                                        UsersFollowsRepository usersFollowsRepository,
                                        UsersPendingFollowsRepository usersPendingFollowsRepository) {
        this.userRepository = userRepository;
        this.blockedUsersRepository = blockedUsersRepository;
        this.usersFollowsRepository = usersFollowsRepository;
        this.usersPendingFollowsRepository = usersPendingFollowsRepository;
    }

    @Override
    public void blockToggle(String sessionUserIdentifier, String targetUserIdentifier) throws Exception {
        Optional<User> user = userRepository.findById(targetUserIdentifier);

        if (user.isEmpty()) {
            throw new UserNotFoundException();
        }

        Optional<BlockedUsers> targetUserIsBlocked = verifyIfIsBlocked(sessionUserIdentifier, targetUserIdentifier);

        if (targetUserIsBlocked.isPresent()) {
            blockedUsersRepository.delete(targetUserIsBlocked.get());
        } else {

            //Verificando se ambos usuarios se seguem/tem solicitacao pendente. Caso sim, é necessário remover antes de bloquear
            Optional<UsersFollows> targetUserIsFollowed = verifyIfIsFollowing(sessionUserIdentifier, targetUserIdentifier);
            targetUserIsFollowed.ifPresent(usersFollowsRepository::delete);

            Optional<UsersPendingFollows> targetUserIsPendingFollowed = verifyIfIsPendingFollowing(sessionUserIdentifier, targetUserIdentifier);
            targetUserIsPendingFollowed.ifPresent(usersPendingFollowsRepository::delete);

            Optional<UsersFollows> sessionUserIsFollowed = verifyIfIsFollowing(targetUserIdentifier, sessionUserIdentifier);
            sessionUserIsFollowed.ifPresent(usersFollowsRepository::delete);

            Optional<UsersPendingFollows> sessionUserIsPendingFollowed = verifyIfIsPendingFollowing(targetUserIdentifier, sessionUserIdentifier);
            sessionUserIsPendingFollowed.ifPresent(usersPendingFollowsRepository::delete);

            blockedUsersRepository.save(BlockedUsers.builder()
                    .id(BlockedUsersId.builder()
                            .blockerIdentifier(sessionUserIdentifier)
                            .blockedIdentifier(targetUserIdentifier)
                            .build())
                    .build());
        }
    }

    @Override
    public void followToggle(String sessionUserIdentifier, String targetUserIdentifier) throws Exception {
        Optional<User> targetUser = userRepository.findById(targetUserIdentifier);

        if (targetUser.isEmpty()) {
            throw new UserNotFoundException();
        }

        Optional<BlockedUsers> targetUserIsBlocked = verifyIfIsBlocked(sessionUserIdentifier, targetUserIdentifier);
        if (targetUserIsBlocked.isPresent()) {
            throw new UnableToFollowException();
        }

        Optional<BlockedUsers> sessionUserIsBlocked = verifyIfIsBlocked(targetUserIdentifier, sessionUserIdentifier);
        if (sessionUserIsBlocked.isPresent()) {
            throw new UnableToFollowException();
        }

        Optional<UsersFollows> targetUserIsFollowed = verifyIfIsFollowing(sessionUserIdentifier, targetUserIdentifier);
        Optional<UsersPendingFollows> targetUserIsPendingFollowed = verifyIfIsPendingFollowing(sessionUserIdentifier, targetUserIdentifier);

        //se o sessionUserIdentifier estiver seguindo o targetUserIdentifier, vai dar unfollow
        if (targetUserIsFollowed.isPresent()) {
            usersFollowsRepository.delete(targetUserIsFollowed.get());
        } //se o sessionUserIdentifier estiver com solicitação pendente para o targetUserIdentifier, vai cancelar solicitação
        else if (targetUserIsPendingFollowed.isPresent()) {
            usersPendingFollowsRepository.delete(targetUserIsPendingFollowed.get());
        } //se o sessionUserIdentifier não seguir e nem estiver com solicitação pendente para o targetUserIdentifier, vai seguir/mandar solicitação
        else {
            if (targetUser.get().getPrivateAccount()) {
                usersPendingFollowsRepository.save(UsersPendingFollows.builder()
                        .id(UsersPendingFollowsId.builder()
                                .pendingFollowerIdentifier(sessionUserIdentifier)
                                .pendingFollowedIdentifier(targetUserIdentifier)
                                .build())
                        .build());
            } else {
                usersFollowsRepository.save(UsersFollows.builder()
                        .id(UsersFollowsId.builder()
                                .followerIdentifier(sessionUserIdentifier)
                                .followedIdentifier(targetUserIdentifier)
                                .build())
                        .build());
            }
        }
    }

    @Override
    public void pendingFollowAcceptDecline(String sessionUserIdentifier, String targetIdentifier, boolean isAccept) throws Exception {
        Optional<UsersPendingFollows> pendingFollowRequest = verifyIfIsPendingFollowing(targetIdentifier, sessionUserIdentifier);

        if (pendingFollowRequest.isEmpty()) {
            throw new NonexistentPendingFollowException();
        }

        if (isAccept) {
            usersPendingFollowsRepository.delete(pendingFollowRequest.get());
            usersFollowsRepository.save(UsersFollows.builder()
                    .id(UsersFollowsId.builder()
                            .followerIdentifier(targetIdentifier)
                            .followedIdentifier(sessionUserIdentifier)
                            .build())
                    .build());
        }else {
            usersPendingFollowsRepository.delete(pendingFollowRequest.get());
        }
    }

    private Optional<UsersFollows> verifyIfIsFollowing(String follower, String followed) {
        return usersFollowsRepository.findById(UsersFollowsId.builder()
                .followerIdentifier(follower)
                .followedIdentifier(followed)
                .build());
    }

    private Optional<UsersPendingFollows> verifyIfIsPendingFollowing(String follower, String followed) {
        return usersPendingFollowsRepository.findById(UsersPendingFollowsId.builder()
                .pendingFollowerIdentifier(follower)
                .pendingFollowedIdentifier(followed)
                .build());
    }

    private Optional<BlockedUsers> verifyIfIsBlocked(String blocker, String blocked) {
        return blockedUsersRepository.findById(BlockedUsersId.builder()
                .blockerIdentifier(blocker)
                .blockedIdentifier(blocked)
                .build());
    }
}
