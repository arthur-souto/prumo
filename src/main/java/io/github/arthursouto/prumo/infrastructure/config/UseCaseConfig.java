package io.github.arthursouto.prumo.infrastructure.config;

import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.ExchangeOAuthCodeUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateLinkTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.GetCurrentUserUseCase;
import io.github.arthursouto.prumo.application.port.in.GetUserIdentitiesUseCase;
import io.github.arthursouto.prumo.application.port.in.GetWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.ListWorkspacesUseCase;
import io.github.arthursouto.prumo.application.port.in.UnlinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RefreshTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RegisterUserUseCase;
import io.github.arthursouto.prumo.application.port.in.RevokeRefreshTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.PasswordHasherPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.application.usecase.AuthenticateUserService;
import io.github.arthursouto.prumo.application.usecase.ExchangeOAuthCodeService;
import io.github.arthursouto.prumo.application.usecase.CreateLinkTokenService;
import io.github.arthursouto.prumo.application.usecase.CreateWorkspaceService;
import io.github.arthursouto.prumo.application.usecase.DeleteWorkspaceService;
import io.github.arthursouto.prumo.application.usecase.GetCurrentUserService;
import io.github.arthursouto.prumo.application.usecase.GetUserIdentitiesService;
import io.github.arthursouto.prumo.application.usecase.GetWorkspaceService;
import io.github.arthursouto.prumo.application.usecase.LinkOAuthIdentityService;
import io.github.arthursouto.prumo.application.usecase.ListWorkspacesService;
import io.github.arthursouto.prumo.application.usecase.UnlinkOAuthIdentityService;
import io.github.arthursouto.prumo.application.usecase.UpdateWorkspaceService;
import io.github.arthursouto.prumo.application.usecase.IssueTokensService;
import io.github.arthursouto.prumo.application.usecase.RefreshTokensService;
import io.github.arthursouto.prumo.application.usecase.RegisterUserService;
import io.github.arthursouto.prumo.application.usecase.RevokeRefreshTokenService;
import io.github.arthursouto.prumo.application.usecase.StartOAuthLoginService;
import io.github.arthursouto.prumo.application.port.in.ConnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.DisconnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.GetBoardUseCase;
import io.github.arthursouto.prumo.application.port.in.GetGithubConnectionUseCase;
import io.github.arthursouto.prumo.application.port.in.GetStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.ListAccessibleRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.ListConnectedRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.ListStoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.MoveStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.RemoveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.RenameColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.ReorderColumnsUseCase;
import io.github.arthursouto.prumo.application.port.in.SaveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.SetChecklistItemDoneUseCase;
import io.github.arthursouto.prumo.application.port.in.SuggestBranchNameUseCase;
import io.github.arthursouto.prumo.application.port.in.SyncAllPullRequestsUseCase;
import io.github.arthursouto.prumo.application.port.in.SyncWorkspacePullRequestsUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateBoardSettingsUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateStoryUseCase;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.application.usecase.BoardProvisioner;
import io.github.arthursouto.prumo.application.usecase.ConnectRepositoryService;
import io.github.arthursouto.prumo.application.usecase.CreateColumnService;
import io.github.arthursouto.prumo.application.usecase.CreateStoryBranchService;
import io.github.arthursouto.prumo.application.usecase.CreateStoryRepositoryService;
import io.github.arthursouto.prumo.application.usecase.CreateStoryService;
import io.github.arthursouto.prumo.application.usecase.DeleteColumnService;
import io.github.arthursouto.prumo.application.usecase.DeleteStoryService;
import io.github.arthursouto.prumo.application.usecase.DisconnectRepositoryService;
import io.github.arthursouto.prumo.application.usecase.GetBoardService;
import io.github.arthursouto.prumo.application.usecase.GetGithubConnectionService;
import io.github.arthursouto.prumo.application.usecase.GetStoryService;
import io.github.arthursouto.prumo.application.usecase.GithubTokens;
import io.github.arthursouto.prumo.application.usecase.LinkStoryRepositoryService;
import io.github.arthursouto.prumo.application.usecase.ListAccessibleRepositoriesService;
import io.github.arthursouto.prumo.application.usecase.ListConnectedRepositoriesService;
import io.github.arthursouto.prumo.application.usecase.ListStoriesService;
import io.github.arthursouto.prumo.application.usecase.MoveStoryService;
import io.github.arthursouto.prumo.application.usecase.PullRequestSyncer;
import io.github.arthursouto.prumo.application.usecase.RemoveGithubTokenService;
import io.github.arthursouto.prumo.application.usecase.RenameColumnService;
import io.github.arthursouto.prumo.application.usecase.ReorderColumnsService;
import io.github.arthursouto.prumo.application.usecase.RepositoryConnector;
import io.github.arthursouto.prumo.application.usecase.SaveGithubTokenService;
import io.github.arthursouto.prumo.application.usecase.SetChecklistItemDoneService;
import io.github.arthursouto.prumo.application.usecase.StoryLookup;
import io.github.arthursouto.prumo.application.usecase.SuggestBranchNameService;
import io.github.arthursouto.prumo.application.usecase.SyncAllPullRequestsService;
import io.github.arthursouto.prumo.application.usecase.SyncWorkspacePullRequestsService;
import io.github.arthursouto.prumo.application.usecase.UpdateBoardSettingsService;
import io.github.arthursouto.prumo.application.usecase.UpdateStoryService;
import io.github.arthursouto.prumo.application.usecase.WorkspaceAccess;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        return new RegisterUserService(userRepository, passwordHasher);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort userRepository,
                                                    PasswordHasherPort passwordHasher) {
        return new AuthenticateUserService(userRepository, passwordHasher);
    }

    @Bean
    StartOAuthLoginUseCase startOAuthLoginUseCase(UserRepositoryPort userRepository,
                                                  UserIdentityRepositoryPort identityRepository,
                                                  AuthorizationCodeRepositoryPort codeRepository,
                                                  AuthorizationCodeCodecPort codeCodec,
                                                  Clock clock,
                                                  OAuthProperties properties) {
        return new StartOAuthLoginService(
                userRepository,
                identityRepository,
                codeRepository, codeCodec, clock, properties.codeTtl());
    }

    @Bean
    ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase(UserRepositoryPort userRepository,
                                                      AuthorizationCodeRepositoryPort codeRepository,
                                                      AuthorizationCodeCodecPort codeCodec,
                                                      Clock clock,
                                                      IssueTokensUseCase issueTokens) {
        return new ExchangeOAuthCodeService(userRepository, codeRepository, codeCodec, clock, issueTokens);
    }

    @Bean
    CreateLinkTokenUseCase createLinkTokenUseCase(UserRepositoryPort userRepository,
                                                  AuthorizationCodeRepositoryPort codeRepository,
                                                  AuthorizationCodeCodecPort codeCodec,
                                                  Clock clock,
                                                  OAuthProperties properties) {
        return new CreateLinkTokenService(userRepository, codeRepository, codeCodec, clock, properties.codeTtl());
    }

    @Bean
    LinkOAuthIdentityUseCase linkOAuthIdentityUseCase(UserRepositoryPort userRepository,
                                                      UserIdentityRepositoryPort identityRepository,
                                                      AuthorizationCodeRepositoryPort codeRepository,
                                                      AuthorizationCodeCodecPort codeCodec,
                                                      Clock clock) {
        return new LinkOAuthIdentityService(userRepository, identityRepository, codeRepository, codeCodec, clock);
    }

    @Bean
    UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase(UserRepositoryPort userRepository,
                                                          UserIdentityRepositoryPort identityRepository) {
        return new UnlinkOAuthIdentityService(userRepository, identityRepository);
    }

    @Bean
    GetUserIdentitiesUseCase getUserIdentitiesUseCase(UserIdentityRepositoryPort identityRepository) {
        return new GetUserIdentitiesService(identityRepository);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepositoryPort userRepository) {
        return new GetCurrentUserService(userRepository);
    }

    @Bean
    IssueTokensUseCase issueTokensUseCase(UserRepositoryPort userRepository,
                                          RefreshTokenRepositoryPort refreshTokenRepository,
                                          AccessTokenIssuerPort accessTokenIssuer,
                                          RefreshTokenCodecPort refreshTokenCodec,
                                          Clock clock,
                                          JwtProperties properties) {
        return new IssueTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                clock,
                properties.refreshTokenTtl()
        );
    }

    @Bean
    RefreshTokensUseCase refreshTokensUseCase(UserRepositoryPort userRepository,
                                              RefreshTokenRepositoryPort refreshTokenRepository,
                                              AccessTokenIssuerPort accessTokenIssuer,
                                              RefreshTokenCodecPort refreshTokenCodec,
                                              Clock clock,
                                              JwtProperties properties) {
        return new RefreshTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                clock,
                properties.refreshTokenTtl()
        );
    }

    @Bean
    RevokeRefreshTokenUseCase revokeRefreshTokenUseCase(RefreshTokenRepositoryPort refreshTokenRepository,
                                                        RefreshTokenCodecPort refreshTokenCodec,
                                                        Clock clock) {
        return new RevokeRefreshTokenService(refreshTokenRepository, refreshTokenCodec, clock);
    }

    @Bean
    CreateWorkspaceUseCase createWorkspaceUseCase(WorkspaceRepositoryPort workspaceRepository) {
        return new CreateWorkspaceService(workspaceRepository);
    }

    @Bean
    ListWorkspacesUseCase listWorkspacesUseCase(WorkspaceRepositoryPort workspaceRepository) {
        return new ListWorkspacesService(workspaceRepository);
    }

    @Bean
    GetWorkspaceUseCase getWorkspaceUseCase(WorkspaceRepositoryPort workspaceRepository) {
        return new GetWorkspaceService(workspaceRepository);
    }

    @Bean
    UpdateWorkspaceUseCase updateWorkspaceUseCase(WorkspaceRepositoryPort workspaceRepository) {
        return new UpdateWorkspaceService(workspaceRepository);
    }

    @Bean
    DeleteWorkspaceUseCase deleteWorkspaceUseCase(WorkspaceRepositoryPort workspaceRepository) {
        return new DeleteWorkspaceService(workspaceRepository);
    }

    @Bean
    WorkspaceAccess workspaceAccess(WorkspaceRepositoryPort workspaceRepository) {
        return new WorkspaceAccess(workspaceRepository);
    }

    @Bean
    BoardProvisioner boardProvisioner(BoardRepositoryPort boardRepository) {
        return new BoardProvisioner(boardRepository);
    }

    @Bean
    StoryLookup storyLookup(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        return new StoryLookup(access, storyRepository);
    }

    @Bean
    GithubTokens githubTokens(
            GithubConnectionRepositoryPort connectionRepository,
            TokenCipherPort cipher
    ) {
        return new GithubTokens(connectionRepository, cipher);
    }

    @Bean
    RepositoryConnector repositoryConnector(ConnectedRepositoryPort repositoryPort) {
        return new RepositoryConnector(repositoryPort);
    }

    @Bean
    PullRequestSyncer pullRequestSyncer(
            GithubConnectionRepositoryPort connectionRepository,
            TokenCipherPort cipher,
            GithubPort github,
            StoryRepositoryPort storyRepository,
            BoardRepositoryPort boardRepository,
            ConnectedRepositoryPort repositoryPort,
            Clock clock
    ) {
        return new PullRequestSyncer(
                connectionRepository,
                cipher,
                github,
                storyRepository,
                boardRepository,
                repositoryPort,
                clock
        );
    }

    @Bean
    GetBoardUseCase getBoardUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            StoryRepositoryPort storyRepository
    ) {
        return new GetBoardService(access, boards, storyRepository);
    }

    @Bean
    UpdateBoardSettingsUseCase updateBoardSettingsUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository,
            StoryRepositoryPort storyRepository
    ) {
        return new UpdateBoardSettingsService(access, boards, boardRepository, storyRepository);
    }

    @Bean
    CreateColumnUseCase createColumnUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository
    ) {
        return new CreateColumnService(access, boards, boardRepository);
    }

    @Bean
    RenameColumnUseCase renameColumnUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository
    ) {
        return new RenameColumnService(access, boards, boardRepository);
    }

    @Bean
    ReorderColumnsUseCase reorderColumnsUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository
    ) {
        return new ReorderColumnsService(access, boards, boardRepository);
    }

    @Bean
    DeleteColumnUseCase deleteColumnUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository,
            StoryRepositoryPort storyRepository
    ) {
        return new DeleteColumnService(access, boards, boardRepository, storyRepository);
    }

    @Bean
    CreateStoryUseCase createStoryUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            BoardRepositoryPort boardRepository,
            StoryRepositoryPort storyRepository
    ) {
        return new CreateStoryService(access, boards, boardRepository, storyRepository);
    }

    @Bean
    ListStoriesUseCase listStoriesUseCase(
            WorkspaceAccess access,
            StoryRepositoryPort storyRepository
    ) {
        return new ListStoriesService(access, storyRepository);
    }

    @Bean
    GetStoryUseCase getStoryUseCase(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        return new GetStoryService(access, storyRepository);
    }

    @Bean
    UpdateStoryUseCase updateStoryUseCase(StoryLookup stories, StoryRepositoryPort storyRepository) {
        return new UpdateStoryService(stories, storyRepository);
    }

    @Bean
    DeleteStoryUseCase deleteStoryUseCase(StoryLookup stories, StoryRepositoryPort storyRepository) {
        return new DeleteStoryService(stories, storyRepository);
    }

    @Bean
    SetChecklistItemDoneUseCase setChecklistItemDoneUseCase(
            StoryLookup stories,
            StoryRepositoryPort storyRepository
    ) {
        return new SetChecklistItemDoneService(stories, storyRepository);
    }

    @Bean
    MoveStoryUseCase moveStoryUseCase(
            WorkspaceAccess access,
            BoardProvisioner boards,
            StoryLookup stories,
            StoryRepositoryPort storyRepository
    ) {
        return new MoveStoryService(access, boards, stories, storyRepository);
    }

    @Bean
    SaveGithubTokenUseCase saveGithubTokenUseCase(
            GithubPort github,
            GithubConnectionRepositoryPort connectionRepository,
            TokenCipherPort cipher,
            Clock clock
    ) {
        return new SaveGithubTokenService(github, connectionRepository, cipher, clock);
    }

    @Bean
    GetGithubConnectionUseCase getGithubConnectionUseCase(
            GithubConnectionRepositoryPort connectionRepository
    ) {
        return new GetGithubConnectionService(connectionRepository);
    }

    @Bean
    RemoveGithubTokenUseCase removeGithubTokenUseCase(
            GithubConnectionRepositoryPort connectionRepository
    ) {
        return new RemoveGithubTokenService(connectionRepository);
    }

    @Bean
    ListAccessibleRepositoriesUseCase listAccessibleRepositoriesUseCase(
            GithubTokens tokens,
            GithubPort github
    ) {
        return new ListAccessibleRepositoriesService(tokens, github);
    }

    @Bean
    ConnectRepositoryUseCase connectRepositoryUseCase(
            WorkspaceAccess access,
            GithubTokens tokens,
            GithubPort github,
            RepositoryConnector connector
    ) {
        return new ConnectRepositoryService(access, tokens, github, connector);
    }

    @Bean
    ListConnectedRepositoriesUseCase listConnectedRepositoriesUseCase(
            WorkspaceAccess access,
            ConnectedRepositoryPort repositoryPort
    ) {
        return new ListConnectedRepositoriesService(access, repositoryPort);
    }

    @Bean
    DisconnectRepositoryUseCase disconnectRepositoryUseCase(
            WorkspaceAccess access,
            ConnectedRepositoryPort repositoryPort
    ) {
        return new DisconnectRepositoryService(access, repositoryPort);
    }

    @Bean
    LinkStoryRepositoryUseCase linkStoryRepositoryUseCase(
            StoryLookup stories,
            StoryRepositoryPort storyRepository,
            ConnectedRepositoryPort repositoryPort
    ) {
        return new LinkStoryRepositoryService(stories, storyRepository, repositoryPort);
    }

    @Bean
    SuggestBranchNameUseCase suggestBranchNameUseCase(StoryLookup stories) {
        return new SuggestBranchNameService(stories);
    }

    @Bean
    CreateStoryBranchUseCase createStoryBranchUseCase(
            StoryLookup stories,
            StoryRepositoryPort storyRepository,
            ConnectedRepositoryPort repositoryPort,
            GithubTokens tokens,
            GithubPort github
    ) {
        return new CreateStoryBranchService(
                stories,
                storyRepository,
                repositoryPort,
                tokens,
                github
        );
    }

    @Bean
    CreateStoryRepositoryUseCase createStoryRepositoryUseCase(
            StoryLookup stories,
            StoryRepositoryPort storyRepository,
            GithubTokens tokens,
            GithubPort github,
            RepositoryConnector connector
    ) {
        return new CreateStoryRepositoryService(stories, storyRepository, tokens, github, connector);
    }

    @Bean
    SyncWorkspacePullRequestsUseCase syncWorkspacePullRequestsUseCase(
            WorkspaceAccess access,
            ConnectedRepositoryPort repositoryPort,
            PullRequestSyncer syncer
    ) {
        return new SyncWorkspacePullRequestsService(access, repositoryPort, syncer);
    }

    @Bean
    SyncAllPullRequestsUseCase syncAllPullRequestsUseCase(
            ConnectedRepositoryPort repositoryPort,
            PullRequestSyncer syncer
    ) {
        return new SyncAllPullRequestsService(repositoryPort, syncer);
    }
}
