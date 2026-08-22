package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventCandidateDto;
import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserIdentityBackgroundRepository;
import com.SocialPairly_Workflow_Manager.repository.UserLifeProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCandidateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserLifeProfileRepository userLifeProfileRepository;

    @Mock
    private UserIdentityBackgroundRepository userIdentityBackgroundRepository;

    @InjectMocks
    private EventCandidateService eventCandidateService;

    private User userAlice;
    private User userBob;
    private UserProfile aliceProfile;
    private UserProfile bobProfile;
    private UserLifeProfile aliceLife;
    private UserIdentityBackground aliceIdentity;

    @BeforeEach
    void setUp() {
        userAlice = buildUser(1L, "Alice", "Smith", "alice@example.com", true);
        userBob = buildUser(2L, "Bob", "Jones", "bob@example.com", false);

        aliceProfile = new UserProfile();
        aliceProfile.setUser(userAlice);
        aliceProfile.setDateOfBirth(LocalDate.now().minusYears(30));
        aliceProfile.setGender("F");
        aliceProfile.setReligion("Christian");
        aliceProfile.setOccupation("Software Engineer");
        aliceProfile.setLocationCity("Austin");
        Education education = new Education();
        education.setEducationLevel("Master");
        aliceProfile.setEducations(List.of(education));

        bobProfile = new UserProfile();
        bobProfile.setUser(userBob);
        bobProfile.setDateOfBirth(LocalDate.now().minusYears(40));
        bobProfile.setGender("M");
        bobProfile.setReligion("None");
        bobProfile.setOccupation("Teacher");
        bobProfile.setLocationCity("Dallas");

        aliceLife = new UserLifeProfile();
        aliceLife.setUser(userAlice);
        aliceLife.setMaritalStatus("Single");

        aliceIdentity = new UserIdentityBackground();
        aliceIdentity.setUser(userAlice);
        aliceIdentity.setStateRegion("Texas");
    }

    @Test
    void searchCandidatesShouldReturnEmptyWhenNoUsers() {
        when(userRepository.findByRole(Role.USER)).thenReturn(List.of());

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, null);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchCandidatesShouldReturnAllUsersWithoutFilters() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, null);

        assertEquals(2, results.size());
        assertEquals("Alice Smith", results.get(0).displayName());
        assertEquals("Bob Jones", results.get(1).displayName());
    }

    @Test
    void searchCandidatesShouldFilterByVerifiedOnly() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, true, null, null, null, null, null, null, null, null);

        assertEquals(1, results.size());
        assertTrue(results.get(0).verified());
    }

    @Test
    void searchCandidatesShouldFilterByGender() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, "F", null, null, null, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals("F", results.get(0).gender());
    }

    @Test
    void searchCandidatesShouldFilterByReligion() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, "Christian", null, null, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals("Christian", results.get(0).religion());
    }

    @Test
    void searchCandidatesShouldFilterByAgeRange() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, 25, 35, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals(30, results.get(0).age());
    }

    @Test
    void searchCandidatesShouldExcludeUsersWithoutAgeWhenAgeFilterApplied() {
        stubRepositories();
        bobProfile.setDateOfBirth(null);

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, 20, 50, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals("Alice Smith", results.get(0).displayName());
    }

    @Test
    void searchCandidatesShouldFilterByLocation() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, "austin", null, null, null);

        assertEquals(1, results.size());
        assertEquals("Austin", results.get(0).locationCity());
    }

    @Test
    void searchCandidatesShouldFilterByStateRegion() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, "texas", null, null, null);

        assertEquals(1, results.size());
        assertEquals("Texas", results.get(0).stateRegion());
    }

    @Test
    void searchCandidatesShouldFilterByProfession() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, "engineer", null, null);

        assertEquals(1, results.size());
        assertEquals("Software Engineer", results.get(0).occupation());
    }

    @Test
    void searchCandidatesShouldFilterByEducationLevel() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, "master", null);

        assertEquals(1, results.size());
        assertEquals("Master", results.get(0).educationLevel());
    }

    @Test
    void searchCandidatesShouldFilterByMaritalStatus() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, "Single");

        assertEquals(1, results.size());
        assertEquals("Single", results.get(0).maritalStatus());
    }

    @Test
    void searchCandidatesShouldFilterByQueryAcrossFields() {
        stubRepositories();

        assertEquals(1, eventCandidateService.searchCandidates(
                "alice", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "bob@example.com", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "2", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "teacher", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "dallas", null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    void searchCandidatesShouldFilterProfilesWithNullUser() {
        UserProfile orphanProfile = new UserProfile();
        orphanProfile.setUser(null);
        when(userRepository.findByRole(Role.USER)).thenReturn(List.of(userAlice));
        when(userProfileRepository.findAll()).thenReturn(List.of(aliceProfile, orphanProfile));
        when(userLifeProfileRepository.findAll()).thenReturn(List.of(aliceLife));
        when(userIdentityBackgroundRepository.findAll()).thenReturn(List.of(aliceIdentity));

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, null);
        assertEquals(1, results.size());
    }

    @Test
    void searchCandidatesShouldExcludeCandidatesBelowMinAge() {
        stubRepositories();
        aliceProfile.setDateOfBirth(LocalDate.now().minusYears(20));

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, 25, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("Bob Jones", results.get(0).displayName());
    }

    @Test
    void searchCandidatesShouldFilterByMaxAgeOnly() {
        stubRepositories();
        aliceProfile.setDateOfBirth(LocalDate.now().minusYears(30));
        bobProfile.setDateOfBirth(LocalDate.now().minusYears(45));

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, null, 35, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("Alice Smith", results.get(0).displayName());
    }

    @Test
    void searchCandidatesShouldIncludeUnverifiedWhenVerifiedOnlyFalse() {
        stubRepositories();

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, false, null, null, null, null, null, null, null, null);
        assertEquals(2, results.size());
    }

    @Test
    void searchCandidatesShouldMatchQueryAgainstGenderAndReligion() {
        stubRepositories();

        assertEquals(1, eventCandidateService.searchCandidates(
                "christian", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "f", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "texas", null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    void searchCandidatesShouldReturnAllWhenQueryBlank() {
        stubRepositories();
        assertEquals(2, eventCandidateService.searchCandidates(
                "   ", null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    void searchCandidatesShouldExcludeWhenMaritalStatusDoesNotMatch() {
        stubRepositories();
        aliceLife.setMaritalStatus(null);

        assertTrue(eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, "Single").isEmpty());
    }

    @Test
    void searchCandidatesShouldExcludeWhenReligionFilterDoesNotMatch() {
        stubRepositories();
        aliceProfile.setReligion(null);

        assertTrue(eventCandidateService.searchCandidates(
                null, null, null, "Christian", null, null, null, null, null, null).isEmpty());
    }

    @Test
    void searchCandidatesNullOccupationAndEducationFailFilters() {
        stubRepositories();
        aliceProfile.setOccupation(null);
        aliceProfile.setEducations(null);
        bobProfile.setOccupation(null);

        assertTrue(eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, "engineer", null, null).isEmpty());
        assertTrue(eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, "master", null).isEmpty());
    }

    @Test
    void searchCandidatesBlankLocationProfessionEducationPassThrough() {
        stubRepositories();
        assertEquals(2, eventCandidateService.searchCandidates(
                null, null, null, null, null, null, "  ", "  ", "  ", "  ").size());
    }

    @Test
    void searchCandidatesQueryMatchesMaritalAndEducation() {
        stubRepositories();
        assertEquals(1, eventCandidateService.searchCandidates(
                "single", null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, eventCandidateService.searchCandidates(
                "master", null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    void searchCandidatesShouldExcludeWhenGenderFilterDoesNotMatchNullGender() {
        stubRepositories();
        aliceProfile.setGender(null);

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, "F", null, null, null, null, null, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchCandidatesShouldExcludeWhenMinAgeExceedsCandidateAge() {
        stubRepositories();
        aliceProfile.setDateOfBirth(LocalDate.now().minusYears(50));
        bobProfile.setDateOfBirth(LocalDate.now().minusYears(30));

        List<EventCandidateDto> results = eventCandidateService.searchCandidates(
                null, null, null, null, 45, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("Alice Smith", results.get(0).displayName());
    }

    @Test
    void searchCandidatesShouldIgnoreLifeAndIdentityProfilesWithNullUser() {
        UserLifeProfile orphanLife = new UserLifeProfile();
        orphanLife.setUser(null);
        UserIdentityBackground orphanIdentity = new UserIdentityBackground();
        orphanIdentity.setUser(null);
        when(userRepository.findByRole(Role.USER)).thenReturn(List.of(userAlice));
        when(userProfileRepository.findAll()).thenReturn(List.of(aliceProfile));
        when(userLifeProfileRepository.findAll()).thenReturn(List.of(aliceLife, orphanLife));
        when(userIdentityBackgroundRepository.findAll()).thenReturn(List.of(aliceIdentity, orphanIdentity));

        assertEquals(1, eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    void searchCandidatesShouldHandleDuplicateProfilesByUserId() {
        UserProfile duplicate = new UserProfile();
        duplicate.setUser(userAlice);
        duplicate.setGender("F");
        when(userRepository.findByRole(Role.USER)).thenReturn(List.of(userAlice));
        when(userProfileRepository.findAll()).thenReturn(List.of(aliceProfile, duplicate));
        when(userLifeProfileRepository.findAll()).thenReturn(List.of(aliceLife));
        when(userIdentityBackgroundRepository.findAll()).thenReturn(List.of(aliceIdentity));

        assertEquals(1, eventCandidateService.searchCandidates(
                null, null, null, null, null, null, null, null, null, null).size());
    }

    private void stubRepositories() {
        when(userRepository.findByRole(Role.USER)).thenReturn(List.of(userAlice, userBob));
        when(userProfileRepository.findAll()).thenReturn(List.of(aliceProfile, bobProfile));
        when(userLifeProfileRepository.findAll()).thenReturn(List.of(aliceLife));
        when(userIdentityBackgroundRepository.findAll()).thenReturn(List.of(aliceIdentity));
    }

    private static User buildUser(Long id, String first, String last, String email, boolean verified) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setVerified(verified);
        return user;
    }
}
