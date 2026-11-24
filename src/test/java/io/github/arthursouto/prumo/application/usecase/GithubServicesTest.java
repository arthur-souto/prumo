package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.in.command.ConnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.DisconnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.LinkStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListAccessibleRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.SaveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubServicesTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private ConnectedRepositoryPort repositoryPort;

    @Mock
    private GithubConnectionRepositoryPort connectionRepository;

    @Mock
    private TokenCipherPort cipher;

    @Mock
    private GithubPort github;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private Workspace workspace;
    private UserStory story;
    private WorkspaceAccess access;
    private StoryLookup lookup;
    private GithubTokens tokens;
    private RepositoryConnector connector;
    private ConnectedRepository repository;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        Board board = BoardFixtures.boardFor(workspace);
        story = BoardFixtures.story(workspace, board.first(), 12, "V");
        story.update("Reset the Password", null, story.getPriority(), null, List.of(), List.of());
        access = new WorkspaceAccess(workspaceRepository);
        lookup = new StoryLookup(access, storyRepository);
        tokens = new GithubTokens(connectionRepository, cipher);
        connector = new RepositoryConnector(repositoryPort);
        repository = ConnectedRepository.connect(
                workspace.getId(), "arthur", "prumo", "main", "https://github.com/arthur/prumo", ownerId
        );
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-12")).thenReturn(Optional.of(story));
        lenient().when(storyRepository.save(story)).thenReturn(story);
    }

    private void withToken(UUID userId) {
        when(connectionRepository.findByUserId(userId)).thenReturn(
                Optional.of(new GithubConnection(userId, "cipher", "arthur", clock.instant()))
        );
        when(cipher.decrypt("cipher")).thenReturn("ghp_plain");
    }

    @Test
    void savingATokenChecksItWithGithubAndStoresOnlyTheCipherText() {
        when(github.authenticate("ghp_plain")).thenReturn("arthur");
        when(cipher.encrypt("ghp_plain")).thenReturn("cipher");
        when(connectionRepository.save(any(GithubConnection.class))).thenAnswer(call -> call.getArgument(0));

        GithubConnection saved = new SaveGithubTokenService(github, connectionRepository, cipher, clock)
                .execute(new SaveGithubTokenCommand(ownerId, "  ghp_plain  "));

        assertThat(saved.tokenCipher()).isEqualTo("cipher");
        assertThat(saved.login()).isEqualTo("arthur");
        assertThat(saved.createdAt()).isEqualTo(clock.instant());
    }

    @Test
    void anInvalidTokenIsNeverStored() {
        when(github.authenticate("bad")).thenThrow(new GithubIntegrationException(GithubFailure.INVALID_TOKEN, "bad"));

        assertThatThrownBy(() -> new SaveGithubTokenService(github, connectionRepository, cipher, clock)
                .execute(new SaveGithubTokenCommand(ownerId, "bad")))
                .isInstanceOf(GithubIntegrationException.class);
        verify(connectionRepository, never()).save(any());
        verify(cipher, never()).encrypt(any());
    }

    @Test
    void usingGithubWithoutATokenReportsMissingToken() {
        when(connectionRepository.findByUserId(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ListAccessibleRepositoriesService(tokens, github)
                .execute(new ListAccessibleRepositoriesQuery(ownerId)))
                .isInstanceOfSatisfying(
                        GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.MISSING_TOKEN)
                );
    }

    @Test
    void listsAccessibleRepositoriesWithTheDecryptedToken() {
        withToken(ownerId);
        RemoteRepository remote = new RemoteRepository("arthur", "prumo", "main", "url", true, null);
        when(github.listRepositories("ghp_plain")).thenReturn(List.of(remote));

        assertThat(new ListAccessibleRepositoriesService(tokens, github)
                .execute(new ListAccessibleRepositoriesQuery(ownerId))).containsExactly(remote);
    }

    @Test
    void ownersConnectARepositoryUsingTheCanonicalNamesFromGithub() {
        withToken(ownerId);
        when(github.getRepository("ghp_plain", "ARTHUR", "Prumo"))
                .thenReturn(new RemoteRepository("arthur", "prumo", "develop", "https://github.com/arthur/prumo", false, null));
        when(repositoryPort.findByWorkspaceIdAndOwnerAndName(workspace.getId(), "arthur", "prumo")).thenReturn(Optional.empty());
        when(repositoryPort.save(any(ConnectedRepository.class))).thenAnswer(call -> call.getArgument(0));

        ConnectedRepository connected = new ConnectRepositoryService(access, tokens, github, connector)
                .execute(new ConnectRepositoryCommand(ownerId, workspace.getId(), "ARTHUR", " Prumo "));

        assertThat(connected.fullName()).isEqualTo("arthur/prumo");
        assertThat(connected.getDefaultBranch()).isEqualTo("develop");
        assertThat(connected.getConnectedBy()).isEqualTo(ownerId);
    }

    @Test
    void connectingTheSameRepositoryTwiceConflicts() {
        withToken(ownerId);
        when(github.getRepository("ghp_plain", "arthur", "prumo"))
                .thenReturn(new RemoteRepository("arthur", "prumo", "main", "url", false, null));
        when(repositoryPort.findByWorkspaceIdAndOwnerAndName(workspace.getId(), "arthur", "prumo"))
                .thenReturn(Optional.of(repository));

        assertThatThrownBy(() -> new ConnectRepositoryService(access, tokens, github, connector)
                .execute(new ConnectRepositoryCommand(ownerId, workspace.getId(), "arthur", "prumo")))
                .isInstanceOf(ConflictException.class);
        verify(repositoryPort, never()).save(any());
    }

    @Test
    void plainMembersCannotConnectOrDisconnectRepositories() {
        assertThatThrownBy(() -> new ConnectRepositoryService(access, tokens, github, connector)
                .execute(new ConnectRepositoryCommand(memberId, workspace.getId(), "a", "b")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> new DisconnectRepositoryService(access, repositoryPort)
                .execute(new DisconnectRepositoryCommand(memberId, workspace.getId(), repository.getId())))
                .isInstanceOf(ForbiddenException.class);
        verify(github, never()).getRepository(any(), any(), any());
    }

    @Test
    void disconnectingOnlyWorksForRepositoriesOfTheSameWorkspace() {
        ConnectedRepository foreign = ConnectedRepository.connect(UUID.randomUUID(), "x", "y", "main", "u", ownerId);
        when(repositoryPort.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        when(repositoryPort.findById(repository.getId())).thenReturn(Optional.of(repository));
        DisconnectRepositoryService service = new DisconnectRepositoryService(access, repositoryPort);

        assertThatThrownBy(() -> service.execute(new DisconnectRepositoryCommand(ownerId, workspace.getId(), foreign.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        service.execute(new DisconnectRepositoryCommand(ownerId, workspace.getId(), repository.getId()));

        verify(repositoryPort).deleteById(repository.getId());
        verify(repositoryPort, never()).deleteById(foreign.getId());
    }

    @Test
    void linkAndUnlinkAStoryToAConnectedRepository() {
        when(repositoryPort.findById(repository.getId())).thenReturn(Optional.of(repository));
        LinkStoryRepositoryService service = new LinkStoryRepositoryService(lookup, storyRepository, repositoryPort);

        service.execute(new LinkStoryRepositoryCommand(memberId, workspace.getId(), "PRU-12", repository.getId()));
        assertThat(story.getRepositoryId()).isEqualTo(repository.getId());

        service.execute(new LinkStoryRepositoryCommand(memberId, workspace.getId(), "PRU-12", null));
        assertThat(story.getRepositoryId()).isNull();
    }

    @Test
    void aStoryCannotLinkARepositoryOfAnotherWorkspace() {
        ConnectedRepository foreign = ConnectedRepository.connect(UUID.randomUUID(), "x", "y", "main", "u", ownerId);
        when(repositoryPort.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> new LinkStoryRepositoryService(lookup, storyRepository, repositoryPort)
                .execute(new LinkStoryRepositoryCommand(ownerId, workspace.getId(), "PRU-12", foreign.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(story.getRepositoryId()).isNull();
    }

    @Test
    void createsARepositoryConnectsItAndLinksTheStory() {
        withToken(ownerId);
        when(github.createRepository("ghp_plain", "pru-12", "desc", true))
                .thenReturn(new RemoteRepository("arthur", "pru-12", "main", "https://github.com/arthur/pru-12", true, "desc"));
        when(repositoryPort.findByWorkspaceIdAndOwnerAndName(workspace.getId(), "arthur", "pru-12")).thenReturn(Optional.empty());
        ArgumentCaptor<ConnectedRepository> saved = ArgumentCaptor.forClass(ConnectedRepository.class);
        when(repositoryPort.save(saved.capture())).thenAnswer(call -> call.getArgument(0));

        UserStory updated = new CreateStoryRepositoryService(lookup, storyRepository, tokens, github, connector)
                .execute(new CreateStoryRepositoryCommand(ownerId, workspace.getId(), "PRU-12", "pru-12", " desc ", true));

        assertThat(updated.getRepositoryId()).isEqualTo(saved.getValue().getId());
        assertThat(saved.getValue().fullName()).isEqualTo("arthur/pru-12");
    }

    @Test
    void plainMembersCannotCreateRepositories() {
        assertThatThrownBy(() -> new CreateStoryRepositoryService(lookup, storyRepository, tokens, github, connector)
                .execute(new CreateStoryRepositoryCommand(memberId, workspace.getId(), "PRU-12", "x", null, false)))
                .isInstanceOf(ForbiddenException.class);
        verify(github, never()).createRepository(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
